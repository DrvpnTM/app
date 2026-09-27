package net.drvpn.app.handler

import net.drvpn.app.AppConfig
import net.drvpn.app.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Dr VPN "Advanced (national internet)" clean-IP scanner.
 *
 * Given one or more IPv4 CIDR ranges and a set of ports, it TCP-connects to every IP in the range
 * (no sampling), measures the connect delay and reports the reachable ones sorted by latency.
 * Addresses are streamed, and the scan position is checkpointed so an interrupted scan resumes from
 * where it stopped — important for national-internet use where a full scan may run for a long time.
 */
object CleanIpScanner {

    data class Result(val ip: String, val port: Int, val latencyMs: Long)

    data class Progress(
        val done: Long = 0,
        val total: Long = 0,
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

    private const val CONNECT_TIMEOUT_MS = 1000
    private const val CONCURRENCY = 256
    private const val MAX_RESULTS = 200
    private const val CHECKPOINT_EVERY = 256

    private data class Block(val network: Long, val count: Long)

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

    private fun parseRanges(rangesText: String): List<Block> =
        rangesText.split(",", " ", "\n", "\t").mapNotNull { parseCidr(it) }

    /** Total number of host addresses in [rangesText], for the UI counter (1 … N). */
    fun totalHosts(rangesText: String): Long = parseRanges(rangesText).sumOf { it.count }

    /** Lazily yields every host address across all blocks in order, so nothing large is in memory. */
    private fun hostSequence(blocks: List<Block>): Sequence<String> = sequence {
        for (b in blocks) {
            var i = 0L
            while (i < b.count) {
                yield(ipString(b.network + i))
                i++
            }
        }
    }

    /**
     * Scans from host index [startIndex] onward. [onCheckpoint] is called periodically and at the
     * end with the next index to resume from, so the caller can persist it. Cancelling the coroutine
     * stops the scan; the last checkpoint marks where to resume.
     */
    suspend fun scan(
        rangesText: String,
        ports: List<Int>,
        startIndex: Long,
        progress: MutableStateFlow<Progress>,
        onCheckpoint: (Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val blocks = parseRanges(rangesText)
        if (blocks.isEmpty() || ports.isEmpty()) {
            progress.value = Progress(error = "invalid", running = false)
            return@withContext
        }
        val total = blocks.sumOf { it.count }
        val existing = progress.value.found
        progress.value = Progress(done = startIndex, total = total, found = existing, running = true)
        val semaphore = Semaphore(CONCURRENCY)
        var launched = startIndex
        var completed = startIndex
        val found = ArrayList(existing)
        val lock = Any()
        try {
            coroutineScope {
                val scope: CoroutineScope = this
                var index = 0L
                for (host in hostSequence(blocks)) {
                    if (index < startIndex) {
                        index++
                        continue
                    }
                    coroutineContext.ensureActive()
                    semaphore.acquire()
                    launched++
                    scope.launch {
                        try {
                            val r = probeHost(host, ports)
                            synchronized(lock) {
                                completed++
                                if (r != null && found.size < MAX_RESULTS) found.add(r)
                                if (completed % CHECKPOINT_EVERY == 0L || r != null) {
                                    progress.value = progress.value.copy(
                                        done = completed,
                                        found = found.sortedBy { it.latencyMs }.take(50),
                                        running = true,
                                    )
                                    onCheckpoint(completed)
                                }
                            }
                        } finally {
                            semaphore.release()
                        }
                    }
                    index++
                }
            }
        } finally {
            val reachedEnd = completed >= total
            progress.value = progress.value.copy(
                done = completed,
                found = found.sortedBy { it.latencyMs }.take(50),
                running = false,
            )
            // On completion, reset resume point; otherwise remember it for next time.
            onCheckpoint(if (reachedEnd) 0L else completed)
        }
    }

    /** Probes each port for one host; returns the reachable port with the lowest delay, or null. */
    private fun probeHost(ip: String, ports: List<Int>): Result? {
        var best: Result? = null
        for (port in ports) {
            val r = probe(ip, port) ?: continue
            if (best == null || r.latencyMs < best.latencyMs) best = r
        }
        return best
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
