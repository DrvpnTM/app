package net.drvpn.app.core

import android.content.Context
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.os.ParcelFileDescriptor
import android.system.Os
import net.drvpn.app.AppConfig
import net.drvpn.app.util.LogUtil
import java.io.File
import java.io.FileDescriptor
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs the OpenVPN program (libopenvpn.so, compile-openvpn.sh) and answers it over its management
 * socket. Built for Android, OpenVPN opens no TUN device itself: it sends the addresses, routes and
 * DNS servers it wants as NEED-OK requests, asks for each of its sockets to be protected, and then
 * asks for the TUN fd (OPENTUN), which [Listener.openTun] makes with the VpnService. The protocol is
 * doc/android.txt in OpenVPN's source.
 */
class OpenVpnSession(private val context: Context, private val listener: Listener) {

    interface Listener {
        /** Make the VPN interface for [tun]; null when that fails. */
        fun openTun(tun: TunConfig): ParcelFileDescriptor?

        /** Make [fd] (an OpenVPN socket) bypass the VPN. */
        fun protect(fd: Int): Boolean

        fun onConnected()

        /** OpenVPN has stopped; [error] is why, or null when [stop] was asked for. */
        fun onExit(error: String?)
    }

    /** What OpenVPN asked for before OPENTUN. */
    class TunConfig {
        var ipv4: String? = null
        var prefix4: Int = 32
        var ipv6: String? = null
        var prefix6: Int = 128
        var mtu: Int = 1500
        /** IPv4 routes and their gateways. */
        val routes = mutableListOf<Triple<String, Int, String?>>()
        val routes6 = mutableListOf<Pair<String, Int>>()
        val dns = mutableListOf<String>()
        var domain: String? = null

        /** Everything the VPN interface is made from, to tell whether a reconnect changed it. */
        fun signature(): String = listOf(ipv4, prefix4, ipv6, prefix6, mtu, routes, routes6, dns, domain).joinToString("|")
    }

    /**
     * This session's own folder: a reconnect starts the next session while this one is still
     * ending, and must not lose its socket or config to this one's clean-up.
     */
    private val dir = File(File(context.noBackupFilesDir, ROOT_DIR), java.util.UUID.randomUUID().toString().take(8))
    private val socketFile = File(dir, "mgmt")
    private val configFile = File(dir, "client.ovpn")
    private val stopping = AtomicBoolean(false)
    private val exited = AtomicBoolean(false)
    /** Reconnects show CONNECTED again; the app hears it once. */
    private val connected = AtomicBoolean(false)
    private var process: Process? = null
    private var server: LocalServerSocket? = null
    private var serverSocket: LocalSocket? = null
    private var client: LocalSocket? = null
    private var tun = TunConfig()
    /** The configuration of the VPN interface OpenVPN has now, null before the first OPENTUN. */
    private var openedSignature: String? = null
    private var lastError: String? = null
    private val pendingFds = ArrayDeque<FileDescriptor>()

    val managementPath: String get() = socketFile.absolutePath

    /** Starts OpenVPN with [config] (it must name [managementPath], see RiseupManager.buildConfig). */
    fun start(config: String) {
        removeLeftovers(dir.parentFile)
        dir.mkdirs()
        socketFile.delete()
        configFile.writeText(config)
        configFile.setReadable(false, false)
        configFile.setReadable(true, true)

        val local = LocalSocket()
        local.bind(LocalSocketAddress(socketFile.absolutePath, LocalSocketAddress.Namespace.FILESYSTEM))
        serverSocket = local
        server = LocalServerSocket(local.fileDescriptor)

        val binary = File(context.applicationInfo.nativeLibraryDir, BINARY)
        process = ProcessBuilder(binary.absolutePath, "--config", configFile.absolutePath)
            .directory(dir)
            .redirectErrorStream(true)
            .start()
        thread("openvpn-log") { readLog() }
        thread("openvpn-wait") {
            val code = runCatching { process?.waitFor() }.getOrNull()
            LogUtil.i(AppConfig.TAG, "OpenVPN: exited with $code")
            // Unblocks accept() when OpenVPN ends before it connected.
            closeSockets()
            finish(lastError ?: "OpenVPN stopped (code $code)")
        }
        thread("openvpn-mgmt") { manage() }
    }

    /** Asks OpenVPN to end, then kills it if it does not. */
    fun stop() {
        if (!stopping.compareAndSet(false, true)) return
        runCatching { send("signal SIGTERM") }
        thread("openvpn-stop") {
            val p = process
            if (p != null && !waitFor(p, 3000)) p.destroy()
            closeSockets()
            finish(null)
        }
    }

    private fun finish(error: String?) {
        if (!exited.compareAndSet(false, true)) return
        dir.deleteRecursively()
        listener.onExit(if (stopping.get()) null else error)
    }

    private fun manage() {
        try {
            val socket = server?.accept() ?: return
            client = socket
            LogUtil.i(AppConfig.TAG, "OpenVPN: management connected")
            send("state on")
            send("hold release")
            val input = socket.inputStream
            val buffer = ByteArray(4096)
            val line = StringBuilder()
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                socket.ancillaryFileDescriptors?.forEach { synchronized(pendingFds) { pendingFds.add(it) } }
                for (i in 0 until n) {
                    val c = buffer[i].toInt().toChar()
                    if (c == '\n') {
                        handle(line.toString().trimEnd('\r'))
                        line.setLength(0)
                    } else {
                        line.append(c)
                    }
                }
            }
        } catch (e: Exception) {
            if (!stopping.get()) LogUtil.w(AppConfig.TAG, "OpenVPN: management ended (${e.javaClass.simpleName})")
        }
    }

    private fun handle(line: String) {
        when {
            line.startsWith(">HOLD:") -> send("hold release")
            line.startsWith(">NEED-OK:") -> needOk(line)
            line.startsWith(">STATE:") -> state(line.removePrefix(">STATE:"))
            line.startsWith(">FATAL:") -> {
                lastError = line.removePrefix(">FATAL:")
                LogUtil.e(AppConfig.TAG, "OpenVPN: $lastError")
            }
            line.startsWith(">PASSWORD:") -> {
                lastError = "OpenVPN asked for a password"
                send("signal SIGTERM")
            }
        }
    }

    /** ">STATE:time,NAME,detail,..." */
    private fun state(body: String) {
        val name = body.split(',').getOrNull(1) ?: return
        LogUtil.i(AppConfig.TAG, "OpenVPN: state $name")
        if (name == "CONNECTED" && connected.compareAndSet(false, true)) listener.onConnected()
    }

    /** ">NEED-OK:Need 'TYPE' confirmation MSG:argument" */
    private fun needOk(line: String) {
        val type = line.substringAfter("Need '", "").substringBefore('\'')
        val arg = line.substringAfter("MSG:", "").trim()
        when (type) {
            "PROTECTFD" -> {
                val fd = synchronized(pendingFds) { pendingFds.poll() }
                if (fd != null) {
                    try {
                        val dup = ParcelFileDescriptor.dup(fd)
                        if (!listener.protect(dup.fd)) LogUtil.w(AppConfig.TAG, "OpenVPN: could not protect a socket")
                        dup.close()
                    } finally {
                        runCatching { Os.close(fd) }
                    }
                }
                send("needok 'PROTECTFD' ok")
            }
            "IFCONFIG" -> {
                // "local netmask-or-remote mtu topology"
                val parts = arg.split(' ')
                tun.ipv4 = parts.getOrNull(0)
                tun.mtu = parts.getOrNull(2)?.toIntOrNull() ?: tun.mtu
                tun.prefix4 = when (parts.getOrNull(3)) {
                    "subnet" -> parts.getOrNull(1)?.let(::maskToPrefix) ?: 32
                    "net30" -> 30
                    else -> 32
                }
                send("needok 'IFCONFIG' ok")
            }
            "IFCONFIG6" -> {
                // "address/bits mtu"
                val addr = arg.substringBefore(' ')
                tun.ipv6 = addr.substringBefore('/')
                tun.prefix6 = addr.substringAfter('/', "128").toIntOrNull() ?: 128
                send("needok 'IFCONFIG6' ok")
            }
            "ROUTE" -> {
                // "network netmask gateway"
                val parts = arg.split(' ')
                val net = parts.getOrNull(0)
                val prefix = parts.getOrNull(1)?.let(::maskToPrefix)
                if (net != null && prefix != null) tun.routes.add(Triple(net, prefix, parts.getOrNull(2)))
                send("needok 'ROUTE' ok")
            }
            "ROUTE6" -> {
                // "network/bits device"
                val net = arg.substringBefore(' ')
                val bits = net.substringAfter('/', "").toIntOrNull()
                if (bits != null) tun.routes6.add(net.substringBefore('/') to bits)
                send("needok 'ROUTE6' ok")
            }
            "DNSSERVER", "DNS6SERVER" -> {
                if (arg.isNotBlank()) tun.dns.add(arg)
                send("needok '$type' ok")
            }
            "DNSDOMAIN" -> {
                tun.domain = arg
                send("needok 'DNSDOMAIN' ok")
            }
            // A reconnect that brings the same addresses and routes keeps the VPN interface open,
            // so no traffic slips past the VPN in between (with "persist-tun").
            "PERSIST_TUN_ACTION" -> {
                val keep = openedSignature != null && openedSignature == tun.signature()
                if (keep) tun = TunConfig()
                send("needok 'PERSIST_TUN_ACTION' ${if (keep) "NOACTION" else "OPEN_BEFORE_CLOSE"}")
            }
            "OPENTUN" -> {
                val requested = tun
                tun = TunConfig()
                val pfd = runCatching { listener.openTun(requested) }
                    .onFailure { LogUtil.e(AppConfig.TAG, "OpenVPN: could not open the VPN interface", it) }
                    .getOrNull()
                if (pfd == null) {
                    lastError = "Could not open the VPN interface"
                    send("needok 'OPENTUN' cancel")
                } else {
                    openedSignature = requested.signature()
                    send("needok 'OPENTUN' ok", pfd.fileDescriptor)
                }
            }
            else -> send("needok '$type' ok")
        }
    }

    @Synchronized
    private fun send(command: String, fd: FileDescriptor? = null) {
        val socket = client ?: return
        try {
            if (fd != null) socket.setFileDescriptorsForSend(arrayOf(fd))
            socket.outputStream.write("$command\n".toByteArray())
            socket.outputStream.flush()
        } catch (e: Exception) {
            LogUtil.w(AppConfig.TAG, "OpenVPN: management write failed (${e.javaClass.simpleName})")
        } finally {
            if (fd != null) socket.setFileDescriptorsForSend(null)
        }
    }

    private fun readLog() {
        runCatching {
            process?.inputStream?.bufferedReader()?.forEachLine { LogUtil.i(AppConfig.TAG, "OpenVPN: $it") }
        }
    }

    private fun closeSockets() {
        runCatching { client?.close() }
        runCatching { server?.close() }
        runCatching { serverSocket?.close() }
        client = null
        server = null
        serverSocket = null
        socketFile.delete()
    }

    private fun waitFor(p: Process, ms: Long): Boolean {
        val end = System.currentTimeMillis() + ms
        while (System.currentTimeMillis() < end) {
            if (runCatching { p.exitValue() }.isSuccess) return true
            Thread.sleep(100)
        }
        return false
    }

    private fun thread(name: String, block: () -> Unit) = Thread(block, name).apply { isDaemon = true }.start()

    companion object {
        const val BINARY = "libopenvpn.so"
        private const val ROOT_DIR = "openvpn"

        /** Folders of sessions that ended without cleaning up (the process was killed); never a running one's. */
        private fun removeLeftovers(root: File?) {
            val old = System.currentTimeMillis() - 10 * 60 * 1000
            root?.listFiles()?.filter { it.lastModified() < old }?.forEach { it.deleteRecursively() }
        }

        fun isSupported(context: Context): Boolean = File(context.applicationInfo.nativeLibraryDir, BINARY).exists()

        /**
         * The routes that belong in the VPN: a route through a gateway outside the tunnel's subnet
         * (the server's own address via the phone's network) stays out.
         */
        fun tunnelRoutes(tun: TunConfig): List<Pair<String, Int>> = tun.routes
            .filter { (_, _, gw) -> gw == null || tun.ipv4 == null || inSubnet(gw, tun.ipv4!!, tun.prefix4) }
            .map { (net, prefix, _) -> net to prefix }

        fun inSubnet(ip: String, network: String, prefix: Int): Boolean {
            val a = ipv4ToLong(ip) ?: return false
            val b = ipv4ToLong(network) ?: return false
            val mask = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
            return (a and mask) == (b and mask)
        }

        private fun ipv4ToLong(ip: String): Long? {
            val parts = ip.split('.').mapNotNull { it.toIntOrNull() }
            if (parts.size != 4 || parts.any { it !in 0..255 }) return null
            return parts.fold(0L) { acc, p -> (acc shl 8) or p.toLong() }
        }

        /** 255.255.248.0 → 21; null when it is not a contiguous mask. */
        fun maskToPrefix(mask: String): Int? {
            val bits = ipv4ToLong(mask) ?: return null
            val prefix = java.lang.Long.bitCount(bits)
            val expected = if (prefix == 0) 0L else (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
            return prefix.takeIf { bits == expected }
        }
    }
}
