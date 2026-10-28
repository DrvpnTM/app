package net.drvpn.app.handler

import java.io.BufferedInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Locale

/**
 * Offline IP → country lookup over Xray's bundled geoip.dat, so server addresses never leave the
 * device. One streaming pass over the file answers a whole batch of addresses (IPv4).
 *
 * Addresses inside a CDN list (Cloudflare, CloudFront, Fastly) report [CDN]: behind a CDN the real
 * server's country cannot be known, and the CDN's own country would be a misleading flag.
 */
object GeoIpLookup {
    const val CDN = "CDN"
    private val CDN_TAGS = setOf("cloudflare", "cloudfront", "fastly")

    /** Returns ip → ISO country code (or [CDN]) for the addresses found in [file]. */
    fun lookup(file: File, ips: Collection<String>): Map<String, String> {
        if (!file.isFile) return emptyMap()
        return FileInputStream(file).use { lookup(it, ips) }
    }

    fun lookup(input: InputStream, ips: Collection<String>): Map<String, String> {
        val targets = ips.distinct()
            .mapNotNull { ip -> ipv4ToLong(ip)?.let { it to ip } }
            .sortedBy { it.first }
        if (targets.isEmpty()) return emptyMap()
        val keys = LongArray(targets.size) { targets[it].first }
        val country = arrayOfNulls<String>(targets.size)
        val bestPrefix = IntArray(targets.size) { -1 }
        val cdn = BooleanArray(targets.size)

        val reader = ProtoReader(BufferedInputStream(input, 1 shl 16))
        while (true) {
            val tag = reader.readTagOrEof() ?: break
            if (tag.field == 1 && tag.wire == 2) {
                val end = reader.readVarint() + reader.position
                var code = ""
                var reverse = false
                while (reader.position < end) {
                    val t = reader.readTag()
                    when {
                        t.field == 1 && t.wire == 2 -> code = String(reader.readBytes(reader.readVarint().toInt()), Charsets.UTF_8)
                        t.field == 2 && t.wire == 2 && !reverse -> {
                            val cidrEnd = reader.readVarint() + reader.position
                            var ip: ByteArray? = null
                            var prefix = 0
                            while (reader.position < cidrEnd) {
                                val c = reader.readTag()
                                when {
                                    c.field == 1 && c.wire == 2 -> ip = reader.readBytes(reader.readVarint().toInt())
                                    c.field == 2 && c.wire == 0 -> prefix = reader.readVarint().toInt()
                                    else -> reader.skip(c.wire)
                                }
                            }
                            if (ip != null && ip.size == 4) {
                                match(code, ip, prefix, keys, country, bestPrefix, cdn)
                            }
                        }
                        t.field == 3 && t.wire == 0 -> reverse = reader.readVarint() != 0L
                        else -> reader.skip(t.wire)
                    }
                }
            } else {
                reader.skip(tag.wire)
            }
        }

        val result = HashMap<String, String>()
        for (i in targets.indices) {
            val value = if (cdn[i]) CDN else country[i] ?: continue
            result[targets[i].second] = value
        }
        return result
    }

    private fun match(
        rawCode: String,
        ip: ByteArray,
        prefix: Int,
        keys: LongArray,
        country: Array<String?>,
        bestPrefix: IntArray,
        cdn: BooleanArray,
    ) {
        val code = rawCode.lowercase(Locale.ROOT)
        val isCdn = code in CDN_TAGS
        if (!isCdn && code.length != 2) return // skip "private", "telegram", ...
        val base = ((ip[0].toLong() and 0xFF) shl 24) or ((ip[1].toLong() and 0xFF) shl 16) or
            ((ip[2].toLong() and 0xFF) shl 8) or (ip[3].toLong() and 0xFF)
        val p = prefix.coerceIn(0, 32)
        val mask = if (p == 0) 0L else (0xFFFFFFFFL shl (32 - p)) and 0xFFFFFFFFL
        val start = base and mask
        val end = start or (mask.inv() and 0xFFFFFFFFL)
        var i = lowerBound(keys, start)
        while (i < keys.size && keys[i] <= end) {
            if (isCdn) {
                cdn[i] = true
            } else if (p > bestPrefix[i]) {
                bestPrefix[i] = p
                country[i] = code.uppercase(Locale.ROOT)
            }
            i++
        }
    }

    private fun lowerBound(keys: LongArray, value: Long): Int {
        var lo = 0
        var hi = keys.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (keys[mid] < value) lo = mid + 1 else hi = mid
        }
        return lo
    }

    internal fun ipv4ToLong(ip: String): Long? {
        val parts = ip.trim().split('.')
        if (parts.size != 4) return null
        var value = 0L
        for (part in parts) {
            val n = part.toIntOrNull() ?: return null
            if (n !in 0..255) return null
            value = (value shl 8) or n.toLong()
        }
        return value
    }

    private class Tag(val field: Int, val wire: Int)

    /** Minimal protobuf wire-format reader that counts consumed bytes. */
    private class ProtoReader(private val input: InputStream) {
        var position = 0L
            private set

        private fun readByte(): Int {
            val b = input.read()
            if (b < 0) throw EOFException()
            position++
            return b
        }

        fun readVarint(): Long {
            var result = 0L
            var shift = 0
            while (shift < 64) {
                val b = readByte()
                result = result or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
            }
            throw IllegalStateException("Malformed varint")
        }

        fun readTagOrEof(): Tag? {
            val first = input.read()
            if (first < 0) return null
            position++
            var result = (first and 0x7F).toLong()
            var shift = 7
            var b = first
            while (b and 0x80 != 0) {
                b = readByte()
                result = result or ((b and 0x7F).toLong() shl shift)
                shift += 7
            }
            return Tag((result ushr 3).toInt(), (result and 7).toInt())
        }

        fun readTag(): Tag {
            val v = readVarint()
            return Tag((v ushr 3).toInt(), (v and 7).toInt())
        }

        fun readBytes(n: Int): ByteArray {
            val out = ByteArray(n)
            var off = 0
            while (off < n) {
                val r = input.read(out, off, n - off)
                if (r < 0) throw EOFException()
                off += r
            }
            position += n
            return out
        }

        fun skipBytes(n: Long) {
            var left = n
            while (left > 0) {
                val s = input.skip(left)
                if (s <= 0) {
                    readByte()
                    left--
                } else {
                    left -= s
                    position += s
                }
            }
        }

        fun skip(wire: Int) {
            when (wire) {
                0 -> readVarint()
                1 -> skipBytes(8)
                2 -> skipBytes(readVarint())
                5 -> skipBytes(4)
                else -> throw IllegalStateException("Unsupported wire type $wire")
            }
        }
    }
}
