package net.drvpn.app.handler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import net.drvpn.app.util.HttpUtil
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

/**
 * "Check & speed test" screen logic. Dr VPN's own process is excluded from the tunnel, so every
 * request goes through the core's local HTTP inbound while connected: the numbers are the tunnel's.
 * Without a connection the speed test measures the plain network (and says so); the DNS leak check
 * needs the tunnel.
 *
 * Contacts, only when the user starts a test: speed.cloudflare.com (speed), bash.ws (DNS leak) and,
 * for the leak verdict, api.ip.sb once outside the tunnel to learn the user's own provider.
 */
object NetworkCheckManager {
    private const val SPEED = "https://speed.cloudflare.com"
    private const val LEAK = "https://bash.ws"
    private const val PHASE_MS = 10_000L

    /** How requests leave the device: through the core ([port] > 0) or directly. */
    class Route(val port: Int, val user: String?, val pass: String?) {
        val viaVpn get() = port > 0
    }

    /** The core's HTTP inbound when it is listening, else a direct route. */
    fun currentRoute(): Route {
        val port = SettingsManager.getHttpPort()
        val listening = port > 0 && runCatching {
            Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 400) }
        }.isSuccess
        return if (listening) Route(port, SettingsManager.getSocksUsername(), SettingsManager.getSocksPassword())
        else Route(0, null, null)
    }

    private fun client(route: Route, timeoutMs: Int): OkHttpClient =
        HttpUtil.buildOkHttpClient(timeoutMs, route.port, route.user, route.pass, followRedirects = true)

    /** Median time to the response headers of an empty download, in ms. */
    suspend fun ping(route: Route): Long = withContext(Dispatchers.IO) {
        val http = client(route, 8000)
        val samples = (1..5).mapNotNull {
            runCatching {
                val start = System.nanoTime()
                http.newCall(Request.Builder().url("$SPEED/__down?bytes=0").build()).execute().use { }
                (System.nanoTime() - start) / 1_000_000
            }.getOrNull()
        }.sorted()
        if (samples.isEmpty()) -1L else samples[samples.size / 2]
    }

    /** Download speed in Mbps over up to 10 s; [onProgress] gets (Mbps so far, phase fraction). */
    suspend fun download(route: Route, onProgress: (Double, Float) -> Unit): Double = withContext(Dispatchers.IO) {
        val http = client(route, 15000)
        val request = Request.Builder().url("$SPEED/__down?bytes=100000000").build()
        var bytes = 0L
        val start = System.nanoTime()
        http.newCall(request).execute().use { response ->
            val body = response.body ?: return@use
            val buffer = ByteArray(64 * 1024)
            val input = body.byteStream()
            var lastReport = 0L
            while (true) {
                coroutineContext.ensureActive()
                val n = input.read(buffer)
                if (n < 0) break
                bytes += n
                val elapsed = (System.nanoTime() - start) / 1_000_000
                if (elapsed - lastReport >= 200) {
                    lastReport = elapsed
                    onProgress(mbps(bytes, elapsed), (elapsed.toFloat() / PHASE_MS).coerceAtMost(1f))
                }
                if (elapsed >= PHASE_MS) break
            }
        }
        mbps(bytes, (System.nanoTime() - start) / 1_000_000)
    }

    /** Upload speed in Mbps: random bytes streamed for up to 10 s. */
    suspend fun upload(route: Route, onProgress: (Double, Float) -> Unit): Double = withContext(Dispatchers.IO) {
        val http = client(route, 20000)
        val chunk = Random.nextBytes(64 * 1024)
        var sent = 0L
        val start = System.nanoTime()
        val body = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun contentLength() = -1L
            override fun writeTo(sink: BufferedSink) {
                var lastReport = 0L
                while (true) {
                    val elapsed = (System.nanoTime() - start) / 1_000_000
                    if (elapsed >= PHASE_MS || sent >= 60_000_000) break
                    sink.write(chunk)
                    sink.flush()
                    sent += chunk.size
                    if (elapsed - lastReport >= 200) {
                        lastReport = elapsed
                        onProgress(mbps(sent, elapsed), (elapsed.toFloat() / PHASE_MS).coerceAtMost(1f))
                    }
                }
            }
        }
        runCatching {
            http.newCall(Request.Builder().url("$SPEED/__up").post(body).build()).execute().use { }
        }
        mbps(sent, (System.nanoTime() - start) / 1_000_000)
    }

    private fun mbps(bytes: Long, ms: Long): Double = if (ms <= 0) 0.0 else bytes * 8.0 / ms / 1000.0

    class DnsServer(val ip: String, val country: String, val countryName: String, val isp: String)

    class LeakResult(
        /** Public address the test site saw (the VPN's exit when connected). */
        val exit: DnsServer?,
        /** Resolvers that looked up the test names. */
        val servers: List<DnsServer>,
        /** true = the user's own ISP (or country) resolved the names, false = it did not, null = unknown. */
        val leaking: Boolean?,
        /** Name of the user's own internet provider, when it could be read. */
        val ownIsp: String? = null,
    )

    /** The user's own connection (outside the tunnel): ASN number, ISP name, country code. */
    private class OwnNetwork(val asn: Int?, val isp: String?, val country: String?)

    private fun ownNetwork(): OwnNetwork? = runCatching {
        val http = client(Route(0, null, null), 8000)
        val body = http.newCall(Request.Builder().url("https://api.ip.sb/geoip").build()).execute()
            .use { it.body?.string() } ?: return null
        val o = JsonParser.parseString(body).asJsonObject
        OwnNetwork(
            asn = o.str("asn").toIntOrNull()?.takeIf { it > 0 },
            isp = o.str("isp").ifBlank { o.str("asn_organization") }.ifBlank { null },
            country = o.str("country_code").uppercase(Locale.ROOT).ifBlank { null },
        )
    }.getOrNull()

    /** "AS15169 Google LLC" -> 15169. */
    internal fun asnNumber(text: String): Int? =
        Regex("""AS(\d+)""", RegexOption.IGNORE_CASE).find(text)?.groupValues?.get(1)?.toIntOrNull()

    /**
     * A leak means lookups reached the user's own provider: a resolver in the same network (ASN) as
     * the user's connection, or in the user's country while the exit is elsewhere. Resolvers of a
     * public DNS abroad (e.g. Google) are not a leak even if they sit in a different country than
     * the exit. Falls back to the test site's own conclusion when the own network is unknown.
     */
    internal fun judge(parsed: LeakResult, ownAsn: Int?, ownCountry: String?): Boolean? {
        if (parsed.servers.isEmpty()) return null
        if (ownAsn == null && ownCountry == null) return parsed.leaking
        if (ownAsn != null && parsed.servers.any { asnNumber(it.isp) == ownAsn }) return true
        val exitCountry = parsed.exit?.country
        if (ownCountry != null && exitCountry != null && exitCountry != ownCountry &&
            parsed.servers.any { it.country == ownCountry }
        ) return true
        return false
    }

    /**
     * DNS leak check (the bash.ws method): look up ten unique names through the tunnel, then ask the
     * test site which DNS resolvers asked its name server about them.
     */
    suspend fun dnsLeak(route: Route): LeakResult = withContext(Dispatchers.IO) {
        val http = client(route, 10000)
        val id = http.newCall(Request.Builder().url("$LEAK/id").build()).execute().use { it.body?.string() }
            ?.trim().orEmpty()
        require(id.isNotEmpty() && id.all { it.isLetterOrDigit() }) { "no test id" }
        val probe = client(route, 4000)
        coroutineScope {
            (1..10).map { n ->
                async {
                    // Only the name lookup matters; the request itself is expected to fail.
                    runCatching { probe.newCall(Request.Builder().url("http://$n.$id.bash.ws/").build()).execute().close() }
                }
            }.awaitAll()
        }
        val json = http.newCall(Request.Builder().url("$LEAK/dnsleak/test/$id?json").build()).execute()
            .use { it.body?.string() }.orEmpty()
        val parsed = parseLeak(json)
        val own = ownNetwork()
        LeakResult(parsed.exit, parsed.servers, judge(parsed, own?.asn, own?.country), own?.isp)
    }

    internal fun parseLeak(json: String): LeakResult {
        val array = JsonParser.parseString(json).asJsonArray
        var exit: DnsServer? = null
        val servers = mutableListOf<DnsServer>()
        var leaking: Boolean? = null
        for (element in array) {
            val o = element.asJsonObject
            val entry = DnsServer(
                ip = o.str("ip"),
                country = o.str("country").uppercase(Locale.ROOT),
                countryName = o.str("country_name"),
                isp = o.str("asn"),
            )
            when (o.str("type")) {
                "ip" -> exit = entry
                "dns" -> servers += entry
                "conclusion" -> {
                    val text = o.str("ip").lowercase(Locale.ROOT)
                    leaking = when {
                        "not leaking" in text -> false
                        "leak" in text -> true
                        else -> null
                    }
                }
            }
        }
        return LeakResult(exit, servers, if (servers.isEmpty()) null else leaking)
    }

    private fun JsonObject.str(key: String): String =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
}
