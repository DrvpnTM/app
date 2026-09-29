package net.drvpn.app.handler

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.net.Socket
import java.net.URLEncoder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PhoneLinkServerTest {
    private var received: String? = null
    private val gotLink = CountDownLatch(1)
    private val server = PhoneLinkServer { received = it; gotLink.countDown() }
    private var port = 0
    private var token = ""

    @Before
    fun start() {
        val url = runCatching { server.start() }.getOrNull()
        assumeTrue("needs a LAN address", url != null)
        val rest = url!!.substringAfterLast(':')
        port = rest.substringBefore('/').toInt()
        token = rest.substringAfter('/')
    }

    @After
    fun stop() = server.stop()

    private fun request(raw: String): String =
        Socket("127.0.0.1", port).use { s ->
            s.soTimeout = 5000
            s.getOutputStream().apply { write(raw.toByteArray()); flush() }
            s.getInputStream().bufferedReader().readText()
        }

    private fun post(path: String, body: String) = request(
        "POST $path HTTP/1.1\r\nHost: x\r\nContent-Type: application/x-www-form-urlencoded\r\n" +
            "Content-Length: ${body.toByteArray().size}\r\n\r\n$body"
    )

    @Test
    fun tokenIsTenSafeCharacters() {
        assertEquals(10, token.length)
        assertTrue(token.all { it.isLowerCase() || it.isDigit() })
    }

    @Test
    fun wrongPathIsRejected() {
        assertTrue(request("GET /wrong HTTP/1.1\r\n\r\n").startsWith("HTTP/1.1 404"))
    }

    @Test
    fun pageIsServedOnTheTokenPath() {
        val response = request("GET /$token HTTP/1.1\r\n\r\n")
        assertTrue(response.startsWith("HTTP/1.1 200"))
        assertTrue(response.contains("<form"))
    }

    @Test
    fun postedLinkIsDelivered() {
        val link = "https://panel.example/sub/abc?x=1"
        val response = post("/$token", "link=" + URLEncoder.encode(link, "UTF-8"))
        assertTrue(response.startsWith("HTTP/1.1 200"))
        assertTrue(gotLink.await(3, TimeUnit.SECONDS))
        assertEquals(link, received)
    }

    @Test
    fun oversizedBodyIsRejected() {
        val response = request("POST /$token HTTP/1.1\r\nContent-Length: 999999\r\n\r\n")
        assertTrue(response.startsWith("HTTP/1.1 400"))
        assertEquals(1L, gotLink.count)
    }
}
