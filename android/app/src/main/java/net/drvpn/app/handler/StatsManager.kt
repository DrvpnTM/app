package net.drvpn.app.handler

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import net.drvpn.app.AppConfig
import net.drvpn.app.BuildConfig
import net.drvpn.app.core.ExtraHopManager
import net.drvpn.app.core.ExtraHopMode
import net.drvpn.app.dto.UrlContentRequest
import net.drvpn.app.util.HttpUtil
import net.drvpn.app.util.LogUtil
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Anonymous usage stats: once a day the app sends a random install ID (made here, tied to nothing),
 * the app version, the region picked in the app, the extra hop in use, gaming mode, how many
 * times it connected that day, and the phone's maker, model and Android version. Nothing else: no IP is kept by the server, no traffic, no servers.
 * Off in Settings (PREF_STATS_ENABLED). The server's address comes from "stats_url" in notice.json,
 * so nothing is sent until the owner sets one (stats-worker/ in the repo).
 */
object StatsManager {

    private const val PREF_ID = "pref_drvpn_stats_id"
    private const val PREF_LAST_SENT = "pref_drvpn_stats_last_sent"
    private const val PREF_CONNECTS = "pref_drvpn_stats_connects"
    private const val WORK_NAME = "drvpn_stats_ping"
    private const val NOTICE_URL = "https://raw.githubusercontent.com/DrvpnTM/app/main/notice.json"

    fun isEnabled(): Boolean = MmkvManager.decodeSettingsBool(AppConfig.PREF_STATS_ENABLED, true)

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

    /** Counts a connection for today (VPN process). Stored as "day|count". */
    fun countConnect() {
        val day = today()
        val (stored, count) = MmkvManager.decodeSettingsString(PREF_CONNECTS).orEmpty().split('|').let {
            it.getOrNull(0).orEmpty() to (it.getOrNull(1)?.toIntOrNull() ?: 0)
        }
        MmkvManager.encodeSettings(PREF_CONNECTS, "$day|${if (stored == day) count + 1 else 1}")
    }

    /** Once a day, in the background (kept when already scheduled). */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<StatsWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(10, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Sends today's ping unless it was sent, stats are off, or no server is set. Blocking. */
    fun sendIfDue() {
        if (!isEnabled()) return
        val day = today()
        if (MmkvManager.decodeSettingsString(PREF_LAST_SENT) == day) return
        val url = statsUrl() ?: return
        val id = MmkvManager.decodeSettingsString(PREF_ID)?.takeIf { it.length >= 16 }
            ?: UUID.randomUUID().toString().replace("-", "").also { MmkvManager.encodeSettings(PREF_ID, it) }
        val connects = MmkvManager.decodeSettingsString(PREF_CONNECTS).orEmpty().split('|')
            .takeIf { it.getOrNull(0) == day }?.getOrNull(1)?.toIntOrNull() ?: 0
        val hop = if (ExtraHopManager.mode() == ExtraHopMode.OFF) "off" else ExtraHopManager.kind().value
        val body = JSONObject()
            .put("id", id)
            .put("v", BuildConfig.VERSION_NAME)
            .put("region", MmkvManager.decodeSettingsString(AppConfig.PREF_DRVPN_REGION) ?: "other")
            .put("hop", hop)
            .put("gaming", MmkvManager.decodeSettingsBool(AppConfig.PREF_GAMING_MODE, false))
            .put("connects", connects)
            // The phone, for the owner's "which phones" chart: maker, model, Android version.
            .put("brand", android.os.Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() })
            .put("model", android.os.Build.MODEL.orEmpty())
            .put("android", android.os.Build.VERSION.RELEASE.orEmpty())
            .toString()
        // Through the VPN when it runs (the server may be filtered), else directly.
        val ports = listOfNotNull(SettingsManager.getHttpPort().takeIf { it > 0 }, 0).distinct()
        for (port in ports) {
            val ok = runCatching {
                HttpUtil.buildOkHttpClient(10_000, port, SettingsManager.getSocksUsername(), SettingsManager.getSocksPassword(), true)
                    .newCall(Request.Builder().url(url).post(body.toRequestBody("application/json".toMediaType())).build())
                    .execute().use { it.isSuccessful }
            }.getOrDefault(false)
            if (ok) {
                MmkvManager.encodeSettings(PREF_LAST_SENT, day)
                return
            }
        }
        LogUtil.w(AppConfig.TAG, "Stats: ping not sent")
    }

    /** "stats_url" of notice.json, or null when there is none. */
    private fun statsUrl(): String? {
        val text = runCatching { HttpUtil.getUrlContent(UrlContentRequest(url = NOTICE_URL, timeout = 8000)) }.getOrNull()
            ?: SettingsManager.getHttpPort().takeIf { it > 0 }?.let { port ->
                runCatching { HttpUtil.getUrlContent(UrlContentRequest(url = NOTICE_URL, timeout = 8000, httpPort = port)) }.getOrNull()
            }
            ?: return null
        return runCatching { JSONObject(text).optString("stats_url") }.getOrNull()?.takeIf { it.startsWith("https://") }
    }

    class StatsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            runCatching { sendIfDue() }
            return Result.success()
        }
    }
}
