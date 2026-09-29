package net.drvpn.app.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class GeoIpLookupTest {

    private fun varint(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while (v >= 0x80) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write(v.toInt())
    }

    private fun lenField(out: ByteArrayOutputStream, field: Int, bytes: ByteArray) {
        varint(out, ((field shl 3) or 2).toLong())
        varint(out, bytes.size.toLong())
        out.write(bytes)
    }

    private fun cidr(ip: String, prefix: Int): ByteArray {
        val out = ByteArrayOutputStream()
        lenField(out, 1, ip.split('.').map { it.toInt().toByte() }.toByteArray())
        varint(out, (2 shl 3).toLong())
        varint(out, prefix.toLong())
        return out.toByteArray()
    }

    /** Builds a GeoIPList: entries of (code, [(ip, prefix)]). */
    private fun geoip(vararg entries: Pair<String, List<Pair<String, Int>>>): ByteArray {
        val list = ByteArrayOutputStream()
        for ((code, ranges) in entries) {
            val entry = ByteArrayOutputStream()
            lenField(entry, 1, code.toByteArray())
            ranges.forEach { (ip, p) -> lenField(entry, 2, cidr(ip, p)) }
            lenField(list, 1, entry.toByteArray())
        }
        return list.toByteArray()
    }

    private val db = geoip(
        "US" to listOf("8.8.8.0" to 24, "104.16.0.0" to 12),
        "cloudflare" to listOf("104.16.0.0" to 13),
        "DE" to listOf("5.0.0.0" to 8),
        "NL" to listOf("5.1.2.0" to 24),
        "private" to listOf("10.0.0.0" to 8),
    )

    private fun lookup(vararg ips: String) = GeoIpLookup.lookup(ByteArrayInputStream(db), ips.toList())

    @Test
    fun findsCountry() {
        assertEquals("US", lookup("8.8.8.8")["8.8.8.8"])
    }

    @Test
    fun mostSpecificRangeWins() {
        val r = lookup("5.1.2.3", "5.9.9.9")
        assertEquals("NL", r["5.1.2.3"])
        assertEquals("DE", r["5.9.9.9"])
    }

    @Test
    fun cdnRangeIsReportedAsCdnNotItsCountry() {
        assertEquals(GeoIpLookup.CDN, lookup("104.16.1.1")["104.16.1.1"])
        // Outside the CDN block but inside the US one.
        assertEquals("US", lookup("104.24.0.1")["104.24.0.1"])
    }

    @Test
    fun nonCountryTagsAndUnknownAddressesAreIgnored() {
        val r = lookup("10.1.2.3", "9.9.9.9", "not-an-ip", "2001:db8::1")
        assertNull(r["10.1.2.3"])
        assertNull(r["9.9.9.9"])
        assertEquals(0, r.size)
    }
}
