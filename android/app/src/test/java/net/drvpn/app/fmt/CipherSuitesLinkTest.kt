package net.drvpn.app.fmt

import android.util.Log
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic

/** cipherSuites travel in share links as "cs", as in PattN/PattNG. */
class CipherSuitesLinkTest {
    private lateinit var mockLog: MockedStatic<Log>

    @Before
    fun setUp() {
        mockLog = mockStatic(Log::class.java, Mockito.RETURNS_DEFAULTS)
    }

    @After
    fun tearDown() {
        mockLog.close()
    }

    private val suites = "TLS_AES_128_GCM_SHA256:TLS_CHACHA20_POLY1305_SHA256"

    @Test
    fun vlessLinkKeepsCipherSuitesAndUnsafeFingerprint() {
        val link = "vless://11111111-2222-3333-4444-555555555555@example.com:443" +
            "?encryption=none&security=tls&sni=example.com&fp=unsafe&cs=$suites&type=tcp#test"

        val profile = VlessFmt.parse(link)!!
        assertEquals("unsafe", profile.fingerPrint)
        assertEquals(suites, profile.cipherSuites)

        // toUri leaves out the scheme; the caller adds it.
        val again = VlessFmt.parse("vless://" + VlessFmt.toUri(profile))!!
        assertEquals("unsafe", again.fingerPrint)
        assertEquals(suites, again.cipherSuites)
    }

    @Test
    fun linkWithoutCipherSuitesWritesNoCsParameter() {
        val link = "vless://11111111-2222-3333-4444-555555555555@example.com:443" +
            "?encryption=none&security=tls&fp=chrome&type=tcp#test"

        val profile = VlessFmt.parse(link)!!
        assertNull(profile.cipherSuites)
        val uri = VlessFmt.toUri(profile)
        assertFalse(uri.contains("cs="))
        assertTrue(uri.contains("fp=chrome"))
    }
}
