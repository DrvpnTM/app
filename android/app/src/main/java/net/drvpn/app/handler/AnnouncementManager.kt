package net.drvpn.app.handler

import net.drvpn.app.AppConfig
import net.drvpn.app.dto.UrlContentRequest
import net.drvpn.app.util.HttpUtil
import net.drvpn.app.util.LogUtil
import org.json.JSONObject
import java.util.Locale

/**
 * Dr VPN announcements: a small JSON file the owner edits on GitHub (notice.json in the app repo).
 *
 * {
 *   "enabled": true,
 *   "id": "2026-10-01",
 *   "title":   { "fa": "...", "en": "..." },
 *   "message": { "fa": "...", "en": "..." },
 *   "link": "https://drvpn.net"
 * }
 *
 * A new "id" shows the card again for users who dismissed an older one.
 */
object AnnouncementManager {
    private const val NOTICE_URL = "https://raw.githubusercontent.com/DrvpnTM/app/main/notice.json"
    private const val PREF_DISMISSED = "pref_drvpn_notice_dismissed"

    data class Announcement(val id: String, val title: String, val message: String, val link: String?)

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
            Announcement(id, title, message, json.optString("link").takeIf { it.startsWith("http") })
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "Announcement: invalid notice.json (${e.javaClass.simpleName})")
            null
        }
    }

    fun dismiss(id: String) {
        MmkvManager.encodeSettings(PREF_DISMISSED, id)
    }

    private fun localized(obj: JSONObject?, language: String): String {
        obj ?: return ""
        return obj.optString(language).ifBlank { obj.optString("fa").ifBlank { obj.optString("en") } }
    }
}
