package net.drvpn.app.handler

import net.drvpn.app.AppConfig
import net.drvpn.app.dto.UrlContentRequest
import net.drvpn.app.util.HttpUtil
import net.drvpn.app.util.JsonUtil
import net.drvpn.app.util.LogUtil
import net.drvpn.app.util.Utils
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

    /**
     * Country written in a server name: a flag emoji first, then a common country name (English or
     * Persian) or an upper-case ISO code token such as "DE-1". Used for CDN servers, whose address
     * cannot reveal where the real server is.
     */
    fun countryFromRemarks(remarks: String): String? {
        flagCountry(remarks)?.let { return it }
        val lower = remarks.lowercase(Locale.ROOT)
        for ((name, code) in COUNTRY_NAMES) {
            val hit = if (name.first().code < 128) {
                Regex("(?<![a-z])" + Regex.escape(name) + "(?![a-z])").containsMatchIn(lower)
            } else {
                lower.contains(name)
            }
            if (hit) return code
        }
        return Regex("(?<![A-Za-z])([A-Z]{2})(?![A-Za-z])").findAll(remarks)
            .map { it.groupValues[1] }
            .firstOrNull { it in COUNTRY_CODES }
            ?.let { if (it == "UK") "GB" else it }
    }

    fun flagOf(code: String?): String? = code?.let(::toFlag)

    private fun flagCountry(text: String): String? {
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val next = i + Character.charCount(cp)
            if (cp in 0x1F1E6..0x1F1FF && next < text.length) {
                val cp2 = text.codePointAt(next)
                if (cp2 in 0x1F1E6..0x1F1FF) {
                    return "${'A' + (cp - 0x1F1E6)}${'A' + (cp2 - 0x1F1E6)}"
                }
            }
            i = next
        }
        return null
    }

    private val COUNTRY_CODES = setOf(
        "DE", "NL", "FR", "GB", "UK", "US", "FI", "TR", "AE", "CA", "SE", "RU", "JP", "SG", "PL", "AT",
        "IT", "ES", "AM", "IR", "CH", "NO", "DK", "HK", "KR", "AU", "BG", "RO", "LT", "LV", "EE", "CZ",
        "HU", "UA", "QA", "SA", "IQ", "IL", "BR", "MX", "IE", "BE", "PT", "GR", "CY", "LU", "MD", "RS", "KZ", "GE", "AZ"
    )

    private val COUNTRY_NAMES = listOf(
        "germany" to "DE", "آلمان" to "DE",
        "netherlands" to "NL", "holland" to "NL", "هلند" to "NL",
        "france" to "FR", "فرانسه" to "FR",
        "united kingdom" to "GB", "england" to "GB", "انگلیس" to "GB", "بریتانیا" to "GB",
        "united states" to "US", "usa" to "US", "america" to "US", "آمریکا" to "US", "امریکا" to "US",
        "finland" to "FI", "فنلاند" to "FI",
        "turkey" to "TR", "turkiye" to "TR", "ترکیه" to "TR",
        "emirates" to "AE", "uae" to "AE", "dubai" to "AE", "امارات" to "AE", "دبی" to "AE",
        "canada" to "CA", "کانادا" to "CA",
        "sweden" to "SE", "سوئد" to "SE",
        "russia" to "RU", "روسیه" to "RU",
        "japan" to "JP", "ژاپن" to "JP",
        "singapore" to "SG", "سنگاپور" to "SG",
        "poland" to "PL", "لهستان" to "PL",
        "austria" to "AT", "اتریش" to "AT",
        "italy" to "IT", "ایتالیا" to "IT",
        "spain" to "ES", "اسپانیا" to "ES",
        "armenia" to "AM", "ارمنستان" to "AM",
        "iran" to "IR", "ایران" to "IR",
        "switzerland" to "CH", "سوئیس" to "CH",
        "norway" to "NO", "نروژ" to "NO",
        "hong kong" to "HK", "هنگ کنگ" to "HK",
        "korea" to "KR", "کره جنوبی" to "KR",
        "australia" to "AU", "استرالیا" to "AU",
        "bulgaria" to "BG", "بلغارستان" to "BG",
        "romania" to "RO", "رومانی" to "RO",
        "lithuania" to "LT", "لیتوانی" to "LT",
        "estonia" to "EE", "استونی" to "EE",
        "czech" to "CZ",
        "hungary" to "HU", "مجارستان" to "HU",
        "ukraine" to "UA", "اوکراین" to "UA",
        "qatar" to "QA", "قطر" to "QA",
        "iraq" to "IQ", "عراق" to "IQ",
        "georgia" to "GE", "گرجستان" to "GE",
        "azerbaijan" to "AZ", "آذربایجان" to "AZ",
        "kazakhstan" to "KZ", "قزاقستان" to "KZ",
    )

    private fun toFlag(code: String): String? {
        if (code.length != 2 || !code.all { it in 'A'..'Z' }) return null
        val first = Character.toChars(0x1F1E6 + (code[0] - 'A'))
        val second = Character.toChars(0x1F1E6 + (code[1] - 'A'))
        return String(first) + String(second)
    }
}
