package net.drvpn.app.handler

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import net.drvpn.app.AppConfig
import net.drvpn.app.R
import net.drvpn.app.dto.UrlContentRequest
import net.drvpn.app.helper.NotificationHelper
import net.drvpn.app.ui.main.MainActivity
import net.drvpn.app.util.HttpUtil
import net.drvpn.app.util.LogUtil
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Dr VPN announcements: a small JSON file the owner edits on GitHub (notice.json in the app repo).
 *
 * {
 *   "enabled": true,
 *   "id": "2026-10-01",
 *   "title":   { "fa": "...", "en": "..." },
 *   "message": { "fa": "...", "en": "..." },
 *   "link": "https://drvpn.net",
 *   "notify": true
 * }
 *
 * A new "id" shows the card again for users who dismissed an older one. With "notify", each new
 * id is also shown once as a system notification, checked every few hours in the background
 * (no push service: the app reads the same file, so nothing is sent about the user).
 */
object AnnouncementManager {
    private const val NOTICE_URL = "https://raw.githubusercontent.com/DrvpnTM/app/main/notice.json"
    private const val PREF_DISMISSED = "pref_drvpn_notice_dismissed"
    /** The last announcement shown as a notification or as the card: never notified again. */
    private const val PREF_NOTIFIED = "pref_drvpn_notice_notified"
    private const val CHANNEL_ID = "drvpn_announcements"
    private const val NOTIFICATION_ID = 15
    private const val WORK_NAME = "drvpn_announcement_check"

    data class Announcement(val id: String, val title: String, val message: String, val link: String?, val notify: Boolean = false)

    /** Fetches the current announcement; null when disabled, dismissed, unreachable or invalid. Blocking. */
    fun fetch(language: String = Locale.getDefault().language): Announcement? {
        val body = runCatching {
            HttpUtil.getUrlContent(UrlContentRequest(url = NOTICE_URL, timeout = 8000))
                ?: if (SettingsManager.getHttpPort() > 0) HttpUtil.getUrlContent(
                    UrlContentRequest(url = NOTICE_URL, timeout = 8000, httpPort = SettingsManager.getHttpPort())
                ) else null
        }.getOrNull() ?: return null
        return try {
            val json = JSONObject(body)
            if (!json.optBoolean("enabled", false)) return null
            val id = json.optString("id").takeIf { it.isNotBlank() } ?: return null
            if (id == MmkvManager.decodeSettingsString(PREF_DISMISSED)) return null
            val title = localized(json.optJSONObject("title"), language)
            val message = localized(json.optJSONObject("message"), language)
            if (title.isBlank() && message.isBlank()) return null
            Announcement(id, title, message, json.optString("link").takeIf { it.startsWith("http") }, json.optBoolean("notify", false))
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "Announcement: invalid notice.json (${e.javaClass.simpleName})")
            null
        }
    }

    fun dismiss(id: String) {
        MmkvManager.encodeSettings(PREF_DISMISSED, id)
    }

    /** The card for [id] is on screen: a notification for it would only repeat it. */
    fun markSeen(id: String) {
        MmkvManager.encodeSettings(PREF_NOTIFIED, id)
    }

    /** Checks every 6 hours, while online (kept when already scheduled). */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<AnnouncementWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Shows a new announcement marked "notify" once as a notification. Blocking. */
    fun notifyIfNew(context: Context) {
        val res = AppLocaleManager.localizedContext(context)
        val lang = runCatching { res.resources.configuration.locales[0].language }.getOrDefault("fa")
        val notice = fetch(lang) ?: return
        if (!notice.notify || notice.id == MmkvManager.decodeSettingsString(PREF_NOTIFIED)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        NotificationHelper.ensureNotificationChannel(
            context = context,
            channelId = CHANNEL_ID,
            channelNameRes = R.string.announcement_channel_name,
            importance = NotificationManager.IMPORTANCE_DEFAULT,
        )
        // Opens the app, where the same announcement is the card on the home screen.
        val open = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = notice.title.ifBlank { res.getString(R.string.app_name) }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(title)
            .setContentText(notice.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notice.message))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            markSeen(notice.id)
        } catch (e: SecurityException) {
            LogUtil.w(AppConfig.TAG, "Announcement notification not allowed: ${e.message}")
        }
    }

    class AnnouncementWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            runCatching { notifyIfNew(applicationContext) }
                .onFailure { LogUtil.w(AppConfig.TAG, "Announcement check failed (${it.javaClass.simpleName})") }
            return Result.success()
        }
    }

    private fun localized(obj: JSONObject?, language: String): String {
        obj ?: return ""
        return obj.optString(language).ifBlank { obj.optString("fa").ifBlank { obj.optString("en") } }
    }
}
