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

/**
 * Dr VPN "Advanced (national internet)" scanner. Given an IPv4 CIDR range and a set of ports,
 * it TCP-connects to every address:port and reports the reachable ones with their latency.
 * This is a slow, best-effort probe meant for finding a reachable clean IP:port under heavy filtering.
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

    private const val MAX_HOSTS = 4096
    private const val CONNECT_TIMEOUT_MS = 1500
    private const val CONCURRENCY = 48

    /**
     * Expands an IPv4 CIDR (e.g. "1.2.3.0/24") to its host addresses. A bare IP is treated as /32.
     * Prefixes below /16 are rejected to keep the scan bounded.
     */
    fun expandCidr(cidr: String): List<String> {
        val trimmed = cidr.trim()
        val (ipPart, prefixPart) = if ("/" in trimmed) {
            val i = trimmed.split("/")
            i[0] to i[1].toIntOrNull()
        } else trimmed to 32
        val prefix = prefixPart ?: return emptyList()
        if (prefix < 16 || prefix > 32) return emptyList()
        val octets = ipPart.split(".")
        if (octets.size != 4) return emptyList()
        val base = octets.map { it.toIntOrNull() ?: return emptyList() }
        if (base.any { it !in 0..255 }) return emptyList()
        val baseInt = (base[0].toLong() shl 24) or (base[1].toLong() shl 16) or
                (base[2].toLong() shl 8) or base[3].toLong()
        val count = 1L shl (32 - prefix)
        val mask = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        val network = baseInt and mask
        val hosts = ArrayList<String>()
        var i = 0L
        while (i < count && hosts.size < MAX_HOSTS) {
            val v = network + i
            hosts.add("${(v shr 24) and 0xFF}.${(v shr 16) and 0xFF}.${(v shr 8) and 0xFF}.${v and 0xFF}")
            i++
        }
        return hosts
    }

    /**
     * Scans [cidr] over [ports], updating [progress] as it goes. Cancellable via the coroutine scope.
     */
    suspend fun scan(
        cidr: String,
        ports: List<Int>,
        progress: MutableStateFlow<Progress>,
    ) = withContext(Dispatchers.IO) {
        val hosts = expandCidr(cidr)
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
                                if (done % 16 == 0 || r != null || done == total) {
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
            val latency = System.currentTimeMillis() - start
            Result(ip, port, latency)
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
