package net.drvpn.app.core

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.drvpn.app.AppConfig
import net.drvpn.app.handler.MmkvManager
import net.drvpn.app.util.HttpUtil
import net.drvpn.app.util.LogUtil
import okhttp3.Request

/**
 * "Dr Open VPN": RiseupVPN, the free, donation-funded OpenVPN service of riseup.net. Needs no
 * account: its API hands out an anonymous client certificate (valid about five weeks) and the list
 * of gateways. Run by OpenVpnSession with the OpenVPN program (compile-openvpn.sh).
 *
 * The API answers are cached, so a connection still works when api.black.riseup.net is filtered
 * but the gateways are not (or after it was reached once through another server).
 */
object RiseupManager {
    private const val EIP_URL = "https://api.black.riseup.net/3/config/eip-service.json"
    private const val CERT_URL = "https://api.black.riseup.net/3/cert"
    private const val CA_URL = "https://black.riseup.net/ca.crt"
    private const val TIMEOUT_MS = 15000

    private const val PREF_EIP = "pref_drvpn_riseup_eip"
    private const val PREF_CA = "pref_drvpn_riseup_ca"
    private const val PREF_CERT = "pref_drvpn_riseup_cert"
    private const val PREF_FETCHED_AT = "pref_drvpn_riseup_fetched_at"

    /** Fetch again after this long; the certificate itself lasts about five weeks. */
    private const val REFRESH_MS = 3L * 24 * 60 * 60 * 1000
    private const val MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000

    /** Ports tried on each gateway, in order: plain OpenVPN first, then the ones filters leave open. */
    private val PORT_ORDER = listOf("udp" to 1194, "tcp" to 80, "udp" to 53, "tcp" to 1194, "udp" to 80, "tcp" to 53)

    /**
     * Gateways closest to Iran, Russia and China first (Europe has the lowest ping from there);
     * each place's gateways are shuffled so the load spreads.
     */
    private val LOCATION_ORDER = listOf("amsterdam", "paris", "montreal", "new york city", "miami")

    data class Gateway(val host: String, val ip: String, val location: String, val endpoints: Set<Pair<String, Int>>)

    data class Provider(val eip: String, val ca: String, val cert: String)

    /**
     * The provider data, fresh when it is older than three days and the API can be reached,
     * otherwise the cached copy (up to a month old). Blocking. Throws when there is neither.
     */
    fun provider(): Provider {
        val cached = cachedProvider()
        val age = System.currentTimeMillis() - MmkvManager.decodeSettingsLong(PREF_FETCHED_AT, 0L)
        if (cached != null && age < REFRESH_MS) return cached
        return try {
            fetch().also {
                MmkvManager.encodeSettings(PREF_EIP, it.eip)
                MmkvManager.encodeSettings(PREF_CA, it.ca)
                MmkvManager.encodeSettings(PREF_CERT, it.cert)
                MmkvManager.encodeSettings(PREF_FETCHED_AT, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "Riseup: API not reachable (${e.javaClass.simpleName})")
            if (cached != null && age < MAX_AGE_MS) cached else throw e
        }
    }

    /** A gateway to time for the server list's ping, from the cached list. */
    fun pingTarget(): Pair<String, Int>? {
        val eip = MmkvManager.decodeSettingsString(PREF_EIP).orEmpty().ifBlank { return null }
        return runCatching { parseGateways(eip) }.getOrNull()
            ?.firstOrNull { ("tcp" to 80) in it.endpoints }
            ?.let { it.ip to 80 }
    }

    private fun cachedProvider(): Provider? {
        val eip = MmkvManager.decodeSettingsString(PREF_EIP).orEmpty()
        val ca = MmkvManager.decodeSettingsString(PREF_CA).orEmpty()
        val cert = MmkvManager.decodeSettingsString(PREF_CERT).orEmpty()
        return if (eip.isBlank() || ca.isBlank() || cert.isBlank()) null else Provider(eip, ca, cert)
    }

    private fun fetch(): Provider {
        val client = HttpUtil.buildOkHttpClient(TIMEOUT_MS, 0, null, null, followRedirects = true)
        fun get(url: String): String = client.newCall(Request.Builder().url(url).get().build()).execute().use {
            if (!it.isSuccessful) error("HTTP ${it.code} from $url")
            it.body.string()
        }
        val eip = get(EIP_URL)
        require(parseGateways(eip).isNotEmpty()) { "no gateways" }
        val cert = get(CERT_URL)
        require(privateKey(cert) != null && certificate(cert) != null) { "bad client certificate" }
        val ca = get(CA_URL)
        require(ca.contains("BEGIN CERTIFICATE")) { "bad CA" }
        return Provider(eip, ca, cert)
    }

    /** The gateways that speak plain OpenVPN, with the ports they listen on. */
    fun parseGateways(eip: String): List<Gateway> {
        val root = JsonParser.parseString(eip).asJsonObject
        return root.getAsJsonArray("gateways")?.mapNotNull { e ->
            val g = e as? JsonObject ?: return@mapNotNull null
            val ip = g.get("ip_address")?.asString ?: return@mapNotNull null
            val transports = g.getAsJsonObject("capabilities")?.getAsJsonArray("transport") ?: return@mapNotNull null
            val endpoints = transports.mapNotNull { it as? JsonObject }
                .filter { it.get("type")?.asString == "openvpn" }
                .flatMap { t ->
                    val protocols = t.getAsJsonArray("protocols")?.map { it.asString.lowercase() }.orEmpty()
                    val ports = t.getAsJsonArray("ports")?.mapNotNull { it.asString.toIntOrNull() }.orEmpty()
                    protocols.flatMap { proto -> ports.map { proto to it } }
                }.toSet()
            if (endpoints.isEmpty()) return@mapNotNull null
            Gateway(g.get("host")?.asString.orEmpty(), ip, g.get("location")?.asString.orEmpty(), endpoints)
        }.orEmpty()
    }

    fun privateKey(pem: String): String? =
        Regex("-----BEGIN (RSA |EC )?PRIVATE KEY-----[\\s\\S]+?-----END (RSA |EC )?PRIVATE KEY-----").find(pem)?.value

    fun certificate(pem: String): String? =
        Regex("-----BEGIN CERTIFICATE-----[\\s\\S]+?-----END CERTIFICATE-----").find(pem)?.value

    /**
     * The OpenVPN config for [provider]: Riseup's own options, every gateway (shuffled, or only
     * [location]'s) on each port, and the management socket at [managementPath] that the app
     * listens on (OpenVPN connects to it: "management-client").
     */
    fun buildConfig(provider: Provider, managementPath: String, location: String? = null): String {
        val all = parseGateways(provider.eip)
        val gateways = all.filter { location.isNullOrBlank() || it.location.equals(location, true) }
            .ifEmpty { all }
            .shuffled()
            .sortedBy { g -> LOCATION_ORDER.indexOf(g.location.lowercase()).let { if (it < 0) LOCATION_ORDER.size else it } }
        val key = privateKey(provider.cert) ?: error("no private key")
        val cert = certificate(provider.cert) ?: error("no certificate")
        val options = JsonParser.parseString(provider.eip).asJsonObject.getAsJsonObject("openvpn_configuration")

        return buildString {
            appendLine("client")
            appendLine("dev tun")
            appendLine("tls-client")
            appendLine("persist-tun")
            appendLine("remote-cert-tls server")
            // Riseup's own options (cipher, auth, tls-cipher, keepalive, ...). Ours below win.
            options?.entrySet()?.forEach { (name, value) ->
                if (name in setOf("dev", "verb", "rcvbuf", "sndbuf") || !Regex("[a-z0-9-]+").matches(name)) return@forEach
                when {
                    value.isJsonPrimitive && value.asJsonPrimitive.isBoolean -> if (value.asBoolean) appendLine(name)
                    value.isJsonPrimitive -> {
                        val v = value.asString.replace('\n', ' ').trim()
                        appendLine(if (v.isEmpty()) name else "$name $v")
                    }
                }
            }
            appendLine("verb 3")
            // Pushed by Riseup for Windows; OpenVPN elsewhere rejects it.
            appendLine("pull-filter ignore \"block-outside-dns\"")
            appendLine("server-poll-timeout 10")
            appendLine("connect-retry 2 10")
            appendLine("resolv-retry infinite")
            PORT_ORDER.forEach { endpoint ->
                gateways.filter { endpoint in it.endpoints }.forEach { g ->
                    appendLine("remote ${g.ip} ${endpoint.second} ${endpoint.first}")
                }
            }
            appendLine("management $managementPath unix")
            appendLine("management-client")
            appendLine("management-hold")
            appendLine("<ca>")
            appendLine(provider.ca.trim())
            appendLine("</ca>")
            appendLine("<cert>")
            appendLine(cert)
            appendLine("</cert>")
            appendLine("<key>")
            appendLine(key)
            appendLine("</key>")
        }
    }
}
