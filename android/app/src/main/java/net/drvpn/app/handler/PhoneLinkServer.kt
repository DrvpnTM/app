package net.drvpn.app.handler

import net.drvpn.app.AppConfig
import net.drvpn.app.util.LogUtil
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.security.SecureRandom
import kotlin.concurrent.thread

/**
 * "Send from phone" for TVs: a tiny HTTP server on the local network that serves one page where the
 * user pastes a subscription link from their phone. It only runs while the dialog is open, only
 * answers on a random one-time path, accepts a single link and then stops.
 */
class PhoneLinkServer(private val onLink: (String) -> Unit) {

    private var server: ServerSocket? = null
    @Volatile private var running = false
    val token: String = randomToken()

    /** Starts listening; returns the URL to show as a QR code, or null if the TV has no LAN address. */
    fun start(): String? {
        val ip = localIpv4() ?: return null
        val socket = ServerSocket(0).also { it.soTimeout = 1000 }
        server = socket
        running = true
        thread(name = "PhoneLinkServer", isDaemon = true) {
            while (running) {
                try {
                    socket.accept().use { handle(it) }
                } catch (_: SocketTimeoutException) {
                } catch (e: Exception) {
                    if (running) LogUtil.w(AppConfig.TAG, "PhoneLinkServer: ${e.javaClass.simpleName}")
                }
            }
            runCatching { socket.close() }
        }
        return "http://$ip:${socket.localPort}/$token"
    }

    fun stop() {
        running = false
        runCatching { server?.close() }
        server = null
    }

    private fun handle(client: Socket) {
        client.soTimeout = 5000
        val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
        val requestLine = reader.readLine() ?: return
        val parts = requestLine.split(' ')
        if (parts.size < 2) return
        val method = parts[0]
        val path = parts[1].substringBefore('?')
        var contentLength = 0
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            if (line.startsWith("Content-Length:", ignoreCase = true)) {
                contentLength = line.substringAfter(':').trim().toIntOrNull() ?: 0
            }
        }
        val out = client.getOutputStream()
        fun respond(code: String, html: String) {
            val body = html.toByteArray(Charsets.UTF_8)
            out.write("HTTP/1.1 $code\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
            out.write(body)
            out.flush()
        }
        if (path != "/$token") {
            respond("404 Not Found", "")
            return
        }
        when (method) {
            "GET" -> respond("200 OK", page(done = false))
            "POST" -> {
                if (contentLength !in 1..16_384) {
                    respond("400 Bad Request", page(done = false))
                    return
                }
                val buf = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val n = reader.read(buf, read, contentLength - read)
                    if (n < 0) break
                    read += n
                }
                val form = String(buf, 0, read)
                val link = form.split('&')
                    .firstOrNull { it.startsWith("link=") }
                    ?.substringAfter("link=")
                    ?.let { URLDecoder.decode(it, "UTF-8").trim() }
                    .orEmpty()
                if (link.isEmpty()) {
                    respond("200 OK", page(done = false))
                } else {
                    respond("200 OK", page(done = true))
                    running = false
                    onLink(link)
                }
            }
            else -> respond("405 Method Not Allowed", "")
        }
    }

    private fun page(done: Boolean): String {
        val content = if (done) {
            """<div class="ok">✓</div><h2>ارسال شد / Sent</h2><p>اشتراک به تلویزیون اضافه شد. این صفحه را ببندید.<br>The subscription was added to your TV. You can close this page.</p>"""
        } else {
            """<h2>ارسال اشتراک به Dr VPN</h2>
<p>لینک اشتراک یا کانفیگ را اینجا بچسبانید و «ارسال» را بزنید.<br>Paste your subscription link or config and tap Send.</p>
<form method="post"><textarea name="link" rows="5" placeholder="https://… / vless://…" required></textarea>
<button type="submit">ارسال به تلویزیون / Send to TV</button></form>"""
        }
        return """<!doctype html><html dir="auto"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Dr VPN</title><style>
body{font-family:-apple-system,system-ui,sans-serif;background:#F2F2F7;margin:0;padding:24px;color:#000}
.card{background:#fff;border-radius:16px;padding:20px;max-width:520px;margin:auto;text-align:center}
textarea{width:100%;box-sizing:border-box;border:1px solid #D1D1D6;border-radius:12px;padding:12px;font-size:16px}
button{margin-top:12px;width:100%;padding:14px;border:0;border-radius:14px;background:#007AFF;color:#fff;font-size:17px;font-weight:600}
.ok{font-size:56px;color:#34C759}p{color:#3C3C43}
</style></head><body><div class="card">$content</div></body></html>"""
    }

    companion object {
        private fun randomToken(): String {
            val chars = "abcdefghijkmnpqrstuvwxyz23456789"
            val rnd = SecureRandom()
            return (1..10).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
        }

        /** The TV's Wi-Fi/Ethernet IPv4 address (ignores the VPN tun interface). */
        fun localIpv4(): String? = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback && !it.name.startsWith("tun") && !it.isVirtual }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
                ?.hostAddress
        }.getOrNull()
    }
}
