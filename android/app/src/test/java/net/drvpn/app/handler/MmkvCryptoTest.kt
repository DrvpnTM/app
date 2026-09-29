package net.drvpn.app.handler

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MmkvCryptoTest {

    @Test
    fun blobRoundTrip() {
        val iv = ByteArray(12) { it.toByte() }
        val ct = ByteArray(32) { (it * 7).toByte() }
        val (outIv, outCt) = MmkvCrypto.decodeBlob(MmkvCrypto.encodeBlob(iv, ct))!!
        assertArrayEquals(iv, outIv)
        assertArrayEquals(ct, outCt)
    }

    @Test
    fun rejectsMalformedBlobs() {
        assertNull(MmkvCrypto.decodeBlob(ByteArray(0)))
        assertNull(MmkvCrypto.decodeBlob(byteArrayOf(2, 12)))          // unknown version
        assertNull(MmkvCrypto.decodeBlob(byteArrayOf(1, 0, 5, 5)))     // empty IV
        assertNull(MmkvCrypto.decodeBlob(byteArrayOf(1, 12, 1, 2, 3))) // truncated
    }

    @Test
    fun newKeyIsSixteenAlphanumericChars() {
        val a = MmkvCrypto.newKey()
        val b = MmkvCrypto.newKey()
        assertEquals(16, a.length)
        assertTrue(a.all { it.isLetterOrDigit() && it.code < 128 })
        assertNotEquals(a, b)
    }
}
