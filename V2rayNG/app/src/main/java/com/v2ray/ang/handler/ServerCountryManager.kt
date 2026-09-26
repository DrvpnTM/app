package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.dto.UrlContentRequest
import com.v2ray.ang.util.HttpUtil
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.Utils
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Dr VPN: looks up the country of each server address and caches it, so the list can show a flag.
 * Lookups run on the caller's IO dispatcher and are best-effort.
 */
object ServerCountryManager {
    private const val CACHE_KEY = "pref_drvpn_server_country_cache"
    private const val LOOKUP_URL = "https://api.country.is/"

    private val cache: ConcurrentHashMap<String, String> by lazy {
        val map = ConcurrentHashMap<String, String>()
        runCatching {
            MmkvManager.decodeSettingsString(CACHE_KEY)?.let { json ->
                JsonUtil.fromJsonSafe(json, Map::class.java)?.forEach { (k, v) ->
                    if (k is String && v is String) map[k] = v
                }
            }
        }
        map
    }

    /** Hosts that failed this session, so they are not retried on every refresh. */
    private val failed = ConcurrentHashMap.newKeySet<String>()

    fun countryCode(host: String?): String? = host?.lowercase(Locale.ROOT)?.let { cache[it] }

    fun flag(host: String?): String? = countryCode(host)?.let(::toFlag)

    /** Resolves missing hosts. Returns true when at least one new country was found. Blocking. */
    fun resolveMissing(hosts: Collection<String>, httpPort: Int = 0): Boolean {
        var changed = false
        hosts.asSequence()
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() && !cache.containsKey(it) && it !in failed }
            .distinct()
            .take(200)
            .forEach { host ->
                val code = lookup(host, httpPort)
                if (code != null) {
                    cache[host] = code
                    changed = true
                } else {
                    failed.add(host)
                }
            }
        if (changed) {
            runCatching { MmkvManager.encodeSettings(CACHE_KEY, JsonUtil.toJson(HashMap(cache))) }
        }
        return changed
    }

    private fun lookup(host: String, httpPort: Int): String? {
        return try {
            val ip = if (Utils.isPureIpAddress(host)) host
            else HttpUtil.resolveHostToIP(host)?.firstOrNull() ?: return null
            val body = HttpUtil.getUrlContent(
                UrlContentRequest(url = LOOKUP_URL + ip, timeout = 8000, httpPort = httpPort)
            ) ?: return null
            val code = Regex(""""country"\s*:\s*"([A-Za-z]{2})"""").find(body)?.groupValues?.get(1)
            code?.uppercase(Locale.ROOT)
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "Country lookup failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun toFlag(code: String): String? {
        if (code.length != 2 || !code.all { it in 'A'..'Z' }) return null
        val first = Character.toChars(0x1F1E6 + (code[0] - 'A'))
        val second = Character.toChars(0x1F1E6 + (code[1] - 'A'))
        return String(first) + String(second)
    }
}
