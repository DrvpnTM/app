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
 * times it connected (and failed to) and how many bytes it moved since the last ping (only the totals), and the
 * phone's maker, model and Android version. Nothing else: no IP is kept by the server, no traffic
 * content or destinations, no servers.
 *
 * Connections and bytes are counted since the last ping that reached the server, not "today so
 * far": the ping usually goes out when the app opens, before that day's connections.
 * Off in Settings (PREF_STATS_ENABLED). The server's address comes from "stats_url" in notice.json,
 * so nothing is sent until the owner sets one (stats-worker/ in the repo).
 */
object StatsManager {

    private const val PREF_ID = "pref_drvpn_stats_id"
    private const val PREF_LAST_SENT = "pref_drvpn_stats_last_sent"
    private const val PREF_CONNECTS = "pref_drvpn_stats_connects_pending"
    private const val PREF_FAILS = "pref_drvpn_stats_fails_pending"
    private const val WORK_NAME = "drvpn_stats_ping"
    private const val NOTICE_URL = "https://raw.githubusercontent.com/DrvpnTM/app/main/notice.json"

    fun isEnabled(): Boolean = MmkvManager.decodeSettingsBool(AppConfig.PREF_STATS_ENABLED, true)

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

    /** Counts a connection since the last ping (VPN process). */
    fun countConnect() {
        if (!isEnabled()) return
        MmkvManager.encodeSettings(PREF_CONNECTS, MmkvManager.decodeSettingsInt(PREF_CONNECTS, 0) + 1)
    }

    /** Counts a connection that failed to start since the last ping (VPN process). */
    fun countFail() {
        if (!isEnabled()) return
        MmkvManager.encodeSettings(PREF_FAILS, MmkvManager.decodeSettingsInt(PREF_FAILS, 0) + 1)
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
        if (!isEnabled()) {
            // Nothing from while stats were off is sent once they are back on.
            MmkvManager.encodeSettings(PREF_CONNECTS, 0)
            MmkvManager.encodeSettings(PREF_FAILS, 0)
            UsageStats.clearPending()
            return
        }
        val day = today()
        if (MmkvManager.decodeSettingsString(PREF_LAST_SENT) == day) return
        val url = statsUrl() ?: return
        val id = MmkvManager.decodeSettingsString(PREF_ID)?.takeIf { it.length >= 16 }
            ?: UUID.randomUUID().toString().replace("-", "").also { MmkvManager.encodeSettings(PREF_ID, it) }
        val connects = MmkvManager.decodeSettingsInt(PREF_CONNECTS, 0)
        val fails = MmkvManager.decodeSettingsInt(PREF_FAILS, 0)
        val (up, down) = UsageStats.pending()
        val hop = if (ExtraHopManager.mode() == ExtraHopMode.OFF) "off" else ExtraHopManager.kind().value
        val body = JSONObject()
            .put("id", id)
            .put("v", BuildConfig.VERSION_NAME)
            .put("region", MmkvManager.decodeSettingsString(AppConfig.PREF_DRVPN_REGION) ?: "other")
            .put("hop", hop)
            .put("gaming", MmkvManager.decodeSettingsBool(AppConfig.PREF_GAMING_MODE, false))
            .put("server", serverKind())
            .put("connects", connects)
            .put("fails", fails)
            // Bytes through the VPN since the last ping, the same totals as the Usage screen.
            .put("up", up)
            .put("down", down)
            // The phone, for the owner's "which phones" chart: maker, model, Android version.
            .put("brand", android.os.Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() })
            .put("model", android.os.Build.MODEL.orEmpty())
            .put("android", android.os.Build.VERSION.RELEASE.orEmpty())
        // Through the VPN when it runs (the server may be filtered), else directly. The server is
        // told which, since through the VPN the place and network it sees are the VPN server's.
        val ports = listOfNotNull(SettingsManager.getHttpPort().takeIf { it > 0 }, 0).distinct()
        for (port in ports) {
            val payload = body.put("via", if (port > 0) "vpn" else "direct").toString()
            val ok = runCatching {
                HttpUtil.buildOkHttpClient(10_000, port, SettingsManager.getSocksUsername(), SettingsManager.getSocksPassword(), true)
                    .newCall(Request.Builder().url(url).post(payload.toRequestBody("application/json".toMediaType())).build())
                    .execute().use { it.isSuccessful }
            }.getOrDefault(false)
            if (ok) {
                MmkvManager.encodeSettings(PREF_LAST_SENT, day)
                // Connections made while the ping was on its way stay for the next one.
                MmkvManager.encodeSettings(PREF_CONNECTS, (MmkvManager.decodeSettingsInt(PREF_CONNECTS, 0) - connects).coerceAtLeast(0))
                MmkvManager.encodeSettings(PREF_FAILS, (MmkvManager.decodeSettingsInt(PREF_FAILS, 0) - fails).coerceAtLeast(0))
                UsageStats.markSent(up, down)
                return
            }
        }
        LogUtil.w(AppConfig.TAG, "Stats: ping not sent")
    }

    /** The kind of server selected, for the owner's "server type" chart. No address, no name. */
    private fun serverKind(): String {
        val guid = MmkvManager.getSelectServer() ?: return "none"
        if (guid == BuiltinServers.SERVERLESS_GUID) return "serverless"
        return when (MmkvManager.decodeServerConfig(guid)?.configType) {
            null -> "none"
            net.drvpn.app.enums.EConfigType.OPENVPN -> "openvpn"
            net.drvpn.app.enums.EConfigType.AETHER -> "aether"
            net.drvpn.app.enums.EConfigType.CUSTOM -> "custom"
            net.drvpn.app.enums.EConfigType.POLICYGROUP, net.drvpn.app.enums.EConfigType.PROXYCHAIN -> "group"
            else -> "xray"
        }
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
