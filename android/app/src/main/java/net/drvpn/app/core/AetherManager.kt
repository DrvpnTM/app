package net.drvpn.app.core

import android.content.Context
import net.drvpn.app.AppConfig
import net.drvpn.app.dto.entities.ProfileItem
import net.drvpn.app.enums.AetherIpVersion
import net.drvpn.app.enums.AetherProtocol
import net.drvpn.app.enums.AetherPsiphon
import net.drvpn.app.enums.AetherScanMode
import net.drvpn.app.enums.AetherTor
import net.drvpn.app.enums.AetherTransport
import net.drvpn.app.enums.EConfigType
import net.drvpn.app.util.LogUtil
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Runs Aether (PattN's WARP core, built by compile-aether.sh) next to Xray.
 *
 * The core is a program of its own, shipped as libaether.so in the app's native library folder.
 * It listens for SOCKS on 127.0.0.1:[AppConfig.PORT_AETHER_SOCKS], which the Xray outbound of an
 * Aether profile dials, and sends its own traffic through Xray's upstream inbound
 * ([AppConfig.PORT_AETHER_UPSTREAM], routed to "direct"), whose sockets Android lets out of the VPN
 * even when "block connections without VPN" is on. Adapted from PattNG's AetherCoreManager (GPL-3.0).
 */
object AetherManager {

    private const val BINARY = "libaether.so"
    private const val PSIPHON_BINARY = "libpsiphon-tunnel-core.so"
    private const val TRANSPORT_BINARY = "liblyrebird.so"
    private const val WORK_DIR = "aether"
    private val torTransports = listOf("obfs4", "snowflake", "webtunnel", "meek_lite", "obfs3", "scramblesuit")
    private val certificateDirectories = listOf("/apex/com.android.conscrypt/cacerts", "/system/etc/security/cacerts")

    @Volatile
    private var process: Process? = null

    private fun binary(context: Context) = File(context.applicationInfo.nativeLibraryDir, BINARY)

    /** False on an ABI the APK carries no Aether for (32-bit x86), where the app hides Aether. */
    fun isSupported(context: Context): Boolean = binary(context).exists()

    // ---------- Arguments ----------

    /**
     * The command line of [profile], without the program: its own command when it has one
     * (advanced), or one built from its settings. The listener the app dials is always
     * 127.0.0.1:[port], and the core sends its traffic through Xray at [upstreamPort].
     */
    fun arguments(profile: ProfileItem, port: Int = AppConfig.PORT_AETHER_SOCKS, upstreamPort: Int = AppConfig.PORT_AETHER_UPSTREAM): List<String> {
        val command = profile.aetherCommand?.takeIf { it.isNotBlank() }?.let(::splitCommand)
        var args = command ?: buildArguments(profile, port)
        if (command != null) args = withValue(args, dialFlag(args), "${AppConfig.LOOPBACK}:$port")
        if ("--upstream" !in args) args = args + listOf("--upstream", "socks5://${AppConfig.LOOPBACK}:$upstreamPort")
        if ("--log-level" !in args) args = args + listOf("--log-level", "info")
        return args
    }

    /** As PattNG's buildArguments, for the settings Dr VPN has. */
    internal fun buildArguments(profile: ProfileItem, port: Int): List<String> {
        val protocol = AetherProtocol.fromString(profile.aetherProtocol)
        val tor = AetherTor.fromString(profile.aetherTor)
        // Tor and Psiphon at once: Psiphon wins, as PattNG's editor allows only one.
        val psiphon = AetherPsiphon.fromString(profile.aetherPsiphon)
        val torMode = if (psiphon != AetherPsiphon.OFF) AetherTor.OFF else tor
        val dialsPsiphon = psiphon == AetherPsiphon.CHAIN || psiphon == AetherPsiphon.ONLY
        val dialsTor = torMode == AetherTor.CHAIN || torMode == AetherTor.ONLY
        var next = port + 1
        val own = if (dialsPsiphon || dialsTor) next++ else port
        return buildList {
            addAll(listOf("--bind", "${AppConfig.LOOPBACK}:$own"))
            if (psiphon != AetherPsiphon.ONLY && torMode != AetherTor.ONLY) {
                addAll(listOf("--protocol", protocol.core))
                if (protocol == AetherProtocol.GOOL) add("--gool-classic")
                addAll(listOf("--scan", AetherScanMode.fromString(profile.aetherScanMode).type))
                addAll(listOf("--ip", AetherIpVersion.fromString(profile.aetherIpVersion).type))
                if (protocol.overMasque && AetherTransport.fromString(profile.aetherTransport) == AetherTransport.HTTP2) add("--h2")
                val outer = profile.aetherWiwOuter?.trim()?.takeIf { it.isNotEmpty() }
                val inner = profile.aetherWiwInner?.trim()?.takeIf { it.isNotEmpty() }
                when {
                    protocol == AetherProtocol.WG_OVER_MASQUE -> {
                        outer?.let { addAll(listOf("--peer", it)) }
                        inner?.let { addAll(listOf("--gool-peer", it)) }
                    }
                    protocol.twoHops -> {
                        val hop = if (protocol == AetherProtocol.MIM) "--mim" else "--wiw"
                        outer?.let { addAll(listOf("$hop-outer", it)) }
                        inner?.let { addAll(listOf("$hop-inner", it)) }
                        if (outer == null && inner == null) add("$hop-scan")
                    }
                    else -> endpoint(profile)?.let { addAll(listOf("--peer", it)) }
                }
                add("--quick-reconnect")
            }
            when (torMode) {
                AetherTor.OFF -> Unit
                AetherTor.CHAIN -> add("--tor")
                AetherTor.REVERSE -> add("--tor-reverse")
                AetherTor.ONLY -> add("--tor-only")
            }
            when (torMode) {
                AetherTor.CHAIN, AetherTor.ONLY -> addAll(listOf("--tor-bind", "${AppConfig.LOOPBACK}:$port"))
                AetherTor.REVERSE -> addAll(listOf("--tor-bind", "${AppConfig.LOOPBACK}:${next++}"))
                AetherTor.OFF -> Unit
            }
            when (psiphon) {
                AetherPsiphon.OFF -> Unit
                AetherPsiphon.CHAIN -> add("--psiphon")
                AetherPsiphon.REVERSE -> add("--psiphon-reverse")
                AetherPsiphon.ONLY -> add("--psiphon-only")
            }
            when (psiphon) {
                AetherPsiphon.CHAIN, AetherPsiphon.ONLY -> addAll(listOf("--psiphon-bind", "${AppConfig.LOOPBACK}:$port"))
                // Nothing of the app dials Psiphon around the tunnel; the core takes the port it reports.
                AetherPsiphon.REVERSE -> addAll(listOf("--psiphon-bind", "${AppConfig.LOOPBACK}:0"))
                AetherPsiphon.OFF -> Unit
            }
        }
    }

    private fun endpoint(profile: ProfileItem): String? {
        val host = profile.server?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val port = profile.serverPort?.trim()?.takeIf { it.isNotEmpty() } ?: return host
        return if (host.contains(':')) "[$host]:$port" else "$host:$port"
    }

    /** The listener the app dials: Psiphon's or Tor's when one runs inside the tunnel or alone. */
    internal fun dialFlag(args: List<String>): String = when {
        "--psiphon" in args || "--psiphon-only" in args -> "--psiphon-bind"
        "--tor" in args || "--tor-only" in args -> "--tor-bind"
        else -> "--bind"
    }

    /** [args] with [flag] set to [value], replacing a value it had. */
    internal fun withValue(args: List<String>, flag: String, value: String): List<String> {
        val out = args.toMutableList()
        val i = out.indexOfFirst { it == flag || it.startsWith("$flag=") }
        if (i < 0) return out + listOf(flag, value)
        if (out[i].startsWith("$flag=")) {
            out[i] = "$flag=$value"
        } else if (i + 1 < out.size && !out[i + 1].startsWith("--")) {
            out[i + 1] = value
        } else {
            out.add(i + 1, value)
        }
        return out
    }

    /** Words of a command line as a shell splits them (quotes and backslashes), without a leading "aether". */
    internal fun splitCommand(text: String): List<String> {
        val words = mutableListOf<String>()
        val word = StringBuilder()
        var inWord = false
        var quote: Char? = null
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quote != null && c == quote -> quote = null
                quote == '"' && c == '\\' && i + 1 < text.length -> { word.append(text[++i]); inWord = true }
                quote != null -> word.append(c)
                c == '"' || c == '\'' -> { quote = c; inWord = true }
                c == '\\' && i + 1 < text.length -> { word.append(text[++i]); inWord = true }
                c.isWhitespace() -> if (inWord) { words.add(word.toString()); word.clear(); inWord = false }
                else -> { word.append(c); inWord = true }
            }
            i++
        }
        if (inWord) words.add(word.toString())
        if (words.firstOrNull()?.substringAfterLast('/')?.lowercase()?.removeSuffix(".exe") == "aether") words.removeAt(0)
        return words
    }

    // ---------- Running ----------

    /** Starts the core for [profile], ending one that runs. */
    @Synchronized
    fun start(context: Context, profile: ProfileItem) {
        stop(context)
        val bin = binary(context)
        if (!bin.exists()) throw IOException("Aether is not available on this device")
        val workDir = File(context.filesDir, WORK_DIR).apply { mkdirs() }
        val args = arguments(profile)
        LogUtil.i(AppConfig.TAG, "Aether: starting ${args.joinToString(" ")}")
        val builder = ProcessBuilder(listOf(bin.absolutePath) + args)
            .directory(workDir)
            .redirectErrorStream(true)
        builder.environment().apply {
            val libDir = context.applicationInfo.nativeLibraryDir
            File(libDir, PSIPHON_BINARY).takeIf { it.exists() }?.let { put("AETHER_PSIPHON_BIN", it.absolutePath) }
            File(libDir, TRANSPORT_BINARY).takeIf { it.exists() }?.let { transport ->
                put("AETHER_TOR_PT", torTransports.joinToString(";") { "$it=${transport.absolutePath}" })
            }
            certificateDirectories.filter { File(it).isDirectory }.joinToString(":").takeIf { it.isNotEmpty() }
                ?.let { put("SSL_CERT_DIR", it) }
            put("AETHER_PSIPHON_DIR", File(workDir, "psiphon").apply { mkdirs() }.absolutePath)
            put("HOME", workDir.absolutePath)
            put("TMPDIR", context.cacheDir.absolutePath)
            // The WARP keys the core registers and keeps, as PattNG names them.
            put("AETHER_CONFIG", File(workDir, "aether.json").absolutePath)
            put("AETHER_MASQUE_CONFIG", File(workDir, "masque.json").absolutePath)
            put("AETHER_WG_CONFIG", File(workDir, "wireguard.json").absolutePath)
        }
        val started = builder.start()
        process = started
        Thread({
            try {
                started.inputStream.bufferedReader().forEachLine { line -> LogUtil.i(AppConfig.TAG, "Aether: $line") }
            } catch (_: IOException) {
            }
            LogUtil.i(AppConfig.TAG, "Aether: core ended")
        }, "aether-output").apply { isDaemon = true }.start()
    }

    /** Ends the core and the Psiphon and Tor helpers it started, also ones left from an earlier run. */
    @Synchronized
    fun stop(context: Context) {
        process?.let(::end)
        process = null
        reapLeftovers(context)
    }

    /**
     * Ends the running core off the calling thread, for a disconnect. Only that process is ended: a core
     * a quick reconnect starts meanwhile keeps running (its start reaps what this one leaves).
     */
    fun stopInBackground() {
        val p = synchronized(this) { process.also { process = null } } ?: return
        Thread({ end(p) }, "aether-stop").apply { isDaemon = true }.start()
    }

    private fun end(p: Process) {
        run {
            p.destroy()
            // Process.waitFor(timeout) and destroyForcibly() need Android 8; Dr VPN runs on 7 as well.
            val deadline = System.currentTimeMillis() + 2_000
            while (alive(p) && System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(50)
                } catch (_: InterruptedException) {
                    break
                }
            }
            if (alive(p)) pidOf(p)?.let { android.os.Process.killProcess(it) }
        }
    }

    fun isRunning(): Boolean = process?.let(::alive) == true

    private fun alive(p: Process): Boolean = try {
        p.exitValue()
        false
    } catch (_: IllegalThreadStateException) {
        true
    }

    /** The pid of [p]: Process.pid() needs Android 13, so it is read from the implementation's field before. */
    private fun pidOf(p: Process): Int? = try {
        p.javaClass.getDeclaredField("pid").apply { isAccessible = true }.getInt(p)
    } catch (_: ReflectiveOperationException) {
        null
    }

    /** Whether the core answers on its SOCKS port yet (it scans for a working WARP endpoint first). */
    fun isListening(port: Int = AppConfig.PORT_AETHER_SOCKS): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(AppConfig.LOOPBACK, port), 500); true }
    } catch (_: IOException) {
        false
    }

    /**
     * Kills processes of this app run from its native library folder as Aether or one of its helpers,
     * which an earlier session may have left when its process was killed.
     */
    private fun reapLeftovers(context: Context) {
        val libDir = context.applicationInfo.nativeLibraryDir
        val names = listOf(BINARY, PSIPHON_BINARY, TRANSPORT_BINARY).map { "$libDir/$it" }
        val myPid = android.os.Process.myPid()
        File("/proc").listFiles()?.forEach { dir ->
            val pid = dir.name.toIntOrNull() ?: return@forEach
            if (pid == myPid) return@forEach
            val exe = try {
                File(dir, "cmdline").readText().substringBefore('\u0000')
            } catch (_: IOException) {
                return@forEach
            }
            if (names.any { exe == it }) {
                LogUtil.i(AppConfig.TAG, "Aether: ending leftover ${exe.substringAfterLast('/')} ($pid)")
                android.os.Process.killProcess(pid)
            }
        }
    }

    fun isAether(profile: ProfileItem?): Boolean = profile?.configType == EConfigType.AETHER
}
