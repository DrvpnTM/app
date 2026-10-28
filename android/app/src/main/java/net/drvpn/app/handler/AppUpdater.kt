package net.drvpn.app.handler

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.drvpn.app.AppConfig
import net.drvpn.app.BuildConfig
import net.drvpn.app.R
import net.drvpn.app.dto.CheckUpdateResult
import net.drvpn.app.helper.NotificationHelper
import net.drvpn.app.ui.main.MainActivity
import net.drvpn.app.util.HttpUtil
import net.drvpn.app.util.LogUtil
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Tells users about a new version and installs it from inside the app:
 * - on opening the app (at most every [CHECK_INTERVAL_MS]) the home screen shows an update dialog;
 * - while the app is closed, [UpdateWorker] checks twice a day and posts a notification that opens it;
 * - "Update now" downloads the APK (directly, else through the running VPN) and opens Android's installer.
 */
object AppUpdater {

    private const val PREF_LAST_CHECK = "pref_drvpn_update_last_check"
    private const val PREF_LATER = "pref_drvpn_update_later"
    private const val PREF_NOTIFIED = "pref_drvpn_update_notified"
    private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

    /** "Later" hides a version's dialog for a day. */
    private const val LATER_MS = 24 * 60 * 60 * 1000L
    private const val WORK_NAME = "drvpn_app_update_check"
    private const val CHANNEL_ID = "app_update_channel"
    private const val NOTIFICATION_ID = 14
    private const val UPDATES_DIR = "updates"

    /** The update to offer on opening the app, or null (none, checked lately, or "Later" this day). */
    suspend fun checkOnOpen(): CheckUpdateResult? {
        val now = System.currentTimeMillis()
        val last = MmkvManager.decodeSettingsString(PREF_LAST_CHECK)?.toLongOrNull() ?: 0L
        if (now - last in 0 until CHECK_INTERVAL_MS) return null
        val result = runCatching { UpdateCheckerManager.checkForUpdate() }
            .onFailure { LogUtil.w(AppConfig.TAG, "Update check failed: ${it.message}") }
            .getOrNull() ?: return null
        MmkvManager.encodeSettings(PREF_LAST_CHECK, now.toString())
        if (!result.hasUpdate || result.downloadUrl.isNullOrBlank()) return null
        val later = MmkvManager.decodeSettingsString(PREF_LATER).orEmpty()
        val (laterVersion, laterAt) = later.substringBefore('@') to (later.substringAfter('@', "").toLongOrNull() ?: 0L)
        if (laterVersion == result.latestVersion && now - laterAt < LATER_MS) return null
        return result
    }

    /** Opening the app after the notification checks again at once. */
    fun forgetLastCheck() = MmkvManager.encodeSettings(PREF_LAST_CHECK, "0")

    fun later(version: String?) {
        MmkvManager.encodeSettings(PREF_LATER, "${version.orEmpty()}@${System.currentTimeMillis()}")
    }

    // ---------- Download and install ----------

    private fun apkFile(context: Context, version: String) =
        File(File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }, "DrVPN-$version.apk")

    /**
     * Downloads the APK of [version] from [url], reporting 0..100 (or -1 while the size is unknown),
     * directly first and through the app's local proxy when that fails. Null when both fail.
     */
    suspend fun download(context: Context, url: String, version: String, onProgress: (Int) -> Unit): File? =
        withContext(Dispatchers.IO) {
            val target = apkFile(context, version)
            // Earlier versions' files are no longer needed.
            target.parentFile?.listFiles()?.filter { it != target }?.forEach { it.delete() }
            for (port in listOf(0, SettingsManager.getHttpPort())) {
                if (downloadOnce(url, target, port, onProgress)) return@withContext target
            }
            target.delete()
            null
        }

    private fun downloadOnce(url: String, target: File, httpPort: Int, onProgress: (Int) -> Unit): Boolean {
        val partial = File(target.path + ".part")
        return try {
            val client = HttpUtil.buildOkHttpClient(
                30_000, httpPort, SettingsManager.getSocksUsername(), SettingsManager.getSocksPassword(), followRedirects = true
            )
            client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                if (!response.isSuccessful) return false
                val body = response.body ?: return false
                val total = body.contentLength()
                var done = 0L
                var lastPercent = -2
                body.byteStream().use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            done += n
                            val percent = if (total > 0) (done * 100 / total).toInt() else -1
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
                if (total > 0 && done != total) return false
                partial.renameTo(target)
            }
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "Update download failed (port $httpPort): ${e.message}")
            false
        } finally {
            partial.delete()
        }
    }

    /** Whether Android lets this app install APKs; on Android 8+ the user allows it once in Settings. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Opens the Settings page where the user allows this app to install updates. */
    fun openInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Opens Android's installer on [apk]; Android checks it is signed like the installed app. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.cache", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** The release page, for a download in the browser when the in-app one can't be used. */
    const val DOWNLOAD_PAGE = "https://github.com/DrvpnTM/app/releases/latest"

    // ---------- While the app is closed ----------

    /** Checks twice a day in the background; kept when already scheduled. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<UpdateWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val result = runCatching { UpdateCheckerManager.checkForUpdate() }.getOrNull() ?: return Result.success()
            val version = result.latestVersion
            if (result.hasUpdate && version != null && MmkvManager.decodeSettingsString(PREF_NOTIFIED) != version) {
                notifyNewVersion(applicationContext, version)
                MmkvManager.encodeSettings(PREF_NOTIFIED, version)
            }
            return Result.success()
        }
    }

    private fun notifyNewVersion(context: Context, version: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        NotificationHelper.ensureNotificationChannel(
            context = context,
            channelId = CHANNEL_ID,
            channelNameRes = R.string.update_channel_name,
            importance = NotificationManager.IMPORTANCE_DEFAULT,
        )
        val res = AppLocaleManager.localizedContext(context)
        val open = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_FROM_UPDATE, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(res.getString(R.string.update_title, version))
            .setContentText(res.getString(R.string.update_notification_text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            LogUtil.w(AppConfig.TAG, "Update notification not allowed: ${e.message}")
        }
    }

    /** MainActivity's intent extra: opened from the update notification. */
    const val EXTRA_FROM_UPDATE = "net.drvpn.app.extra.FROM_UPDATE"
}
