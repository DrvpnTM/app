package com.v2ray.ang.handler

import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.random.Random

/**
 * Dr VPN "Advanced (national internet)" clean-IP scanner.
 *
 * Given one or more IPv4 CIDR ranges and a set of ports, it TCP-connects to sampled address:port
 * pairs and reports the reachable ones with latency. Clean-IP scanning targets the CDN ranges that
 * front a config (e.g. Cloudflare); scanning the whole internet from a phone is infeasible and would
 * not find the user's proxy, so large ranges are randomly sampled down to [SAMPLE_LIMIT].
 */
object CleanIpScanner {

    data class Result(val ip: String, val port: Int, val latencyMs: Long)

    data class Progress(
        val done: Int = 0,
        val total: Int = 0,
        val found: List<Result> = emptyList(),
        val running: Boolean = false,
        val error: String? = null,
    )

    /** Common HTTPS/CDN ports offered as defaults; the user can edit the list. */
    val DEFAULT_PORTS = listOf(443, 8443, 2053, 2083, 2087, 2096, 80, 8080, 8880)

    /** Cloudflare's published IPv4 ranges — the usual clean-IP target. */
    val CLOUDFLARE_RANGES = listOf(
        "173.245.48.0/20", "103.21.244.0/22", "103.22.200.0/22", "103.31.4.0/22",
        "141.101.64.0/18", "108.162.192.0/18", "190.93.240.0/20", "188.114.96.0/20",
        "197.234.240.0/22", "198.41.128.0/17", "162.158.0.0/15", "104.16.0.0/13",
        "104.24.0.0/14", "172.64.0.0/13", "131.0.72.0/22",
    )

    /** Addresses probed per scan; the union of ranges is randomly sampled down to this. */
    const val SAMPLE_LIMIT = 2048

    private const val CONNECT_TIMEOUT_MS = 1000
    private const val CONCURRENCY = 256

    private data class Block(val network: Long, val count: Long)

    /** Parses one IPv4 CIDR ("1.2.3.0/24") or bare IP ("/32") into a Block, or null if invalid. */
    private fun parseCidr(cidr: String): Block? {
        val trimmed = cidr.trim()
        if (trimmed.isEmpty()) return null
        val ipPart: String
        val prefix: Int
        if ("/" in trimmed) {
            val parts = trimmed.split("/")
            ipPart = parts[0]
            prefix = parts.getOrNull(1)?.toIntOrNull() ?: return null
        } else {
            ipPart = trimmed
            prefix = 32
        }
        if (prefix < 0 || prefix > 32) return null
        val octets = ipPart.split(".")
        if (octets.size != 4) return null
        val base = octets.map { it.toIntOrNull() ?: return null }
        if (base.any { it !in 0..255 }) return null
        val baseInt = (base[0].toLong() shl 24) or (base[1].toLong() shl 16) or
                (base[2].toLong() shl 8) or base[3].toLong()
        val mask = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        return Block(baseInt and mask, 1L shl (32 - prefix))
    }

    private fun ipString(v: Long): String =
        "${(v shr 24) and 0xFF}.${(v shr 16) and 0xFF}.${(v shr 8) and 0xFF}.${v and 0xFF}"

    /**
     * Turns comma/space/newline-separated CIDRs into up to [SAMPLE_LIMIT] distinct host IPs.
     * When the total space is small it returns all of it; when large it samples at random.
     */
    fun buildHosts(rangesText: String): List<String> {
        val blocks = rangesText.split(",", " ", "\n", "\t")
            .mapNotNull { parseCidr(it) }
        if (blocks.isEmpty()) return emptyList()
        val total = blocks.sumOf { it.count }
        if (total <= 0L) return emptyList()
        if (total <= SAMPLE_LIMIT) {
            val out = ArrayList<String>(total.toInt())
            blocks.forEach { b -> for (i in 0 until b.count) out.add(ipString(b.network + i)) }
            return out.distinct()
        }
        // Sample random offsets across the concatenated address space of all blocks.
        val prefixSums = LongArray(blocks.size)
        var acc = 0L
        for (i in blocks.indices) {
            acc += blocks[i].count
            prefixSums[i] = acc
        }
        val seen = HashSet<String>(SAMPLE_LIMIT * 2)
        var attempts = 0
        val maxAttempts = SAMPLE_LIMIT * 4
        while (seen.size < SAMPLE_LIMIT && attempts < maxAttempts) {
            attempts++
            val r = (Random.nextDouble() * total).toLong().coerceIn(0, total - 1)
            val bi = prefixSums.indexOfFirst { r < it }
            val block = blocks[bi]
            val offsetBase = if (bi == 0) 0L else prefixSums[bi - 1]
            seen.add(ipString(block.network + (r - offsetBase)))
        }
        return seen.toList()
    }

    suspend fun scan(
        rangesText: String,
        ports: List<Int>,
        progress: MutableStateFlow<Progress>,
    ) = withContext(Dispatchers.IO) {
        val hosts = buildHosts(rangesText)
        if (hosts.isEmpty() || ports.isEmpty()) {
            progress.value = Progress(error = "invalid", running = false)
            return@withContext
        }
        val total = hosts.size * ports.size
        progress.value = Progress(total = total, running = true)
        val semaphore = Semaphore(CONCURRENCY)
        var done = 0
        val found = ArrayList<Result>()
        val lock = Any()
        coroutineScope {
            for (host in hosts) {
                for (port in ports) {
                    launch {
                        semaphore.withPermit {
                            val r = probe(host, port)
                            synchronized(lock) {
                                done++
                                if (r != null) found.add(r)
                                if (done % 32 == 0 || r != null || done == total) {
                                    progress.value = Progress(
                                        done = done,
                                        total = total,
                                        found = found.sortedBy { it.latencyMs }.take(50),
                                        running = done < total,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        progress.value = progress.value.copy(
            done = total,
            found = found.sortedBy { it.latencyMs }.take(50),
            running = false,
        )
    }

    private fun probe(ip: String, port: Int): Result? {
        val socket = Socket()
        return try {
            val start = System.currentTimeMillis()
            socket.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS)
            Result(ip, port, System.currentTimeMillis() - start)
        } catch (e: Exception) {
            null
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                LogUtil.d(AppConfig.TAG, "scan socket close failed")
            }
        }
    }
}
