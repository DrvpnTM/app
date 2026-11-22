package net.drvpn.app.core

import android.content.Context
import net.drvpn.app.AppConfig
import net.drvpn.app.dto.entities.ProfileItem
import net.drvpn.app.enums.EConfigType
import net.drvpn.app.handler.MmkvManager
import net.drvpn.app.util.LogUtil
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * "Dr Anti-DPI": ByeDPI (hufrea/byedpi, built by compile-byedpi.sh) runs next to Xray as a local
 * SOCKS proxy on 127.0.0.1:[AppConfig.PORT_BYEDPI_SOCKS]. Xray reaches the server through it, and
 * it splits, reorders or fakes the first packets of each connection so DPI does not recognise the
 * server's protocol. It needs no server of its own, so it only sits before the server.
 *
 * Its own traffic leaves directly: the app is outside its own VPN, like Lantern's.
 */
object ByeDpiManager {

    private const val BINARY = "libbyedpi.so"

    /**
     * Disorder the first packet; when a server resets or times out after the first request, split
     * the TLS record at the SNI instead (ByeDPI's own example, and a common choice in Russia and Iran).
     */
    const val DEFAULT_ARGS = "--disorder 1 --auto=torst --tlsrec 1+s"

    /** Options the user may not set: the listener is the app's, the rest would break the hop. */
    private val RESERVED = setOf("-i", "--ip", "-p", "--port", "-D", "--daemon", "-w", "--pidfile", "-E", "--transparent", "-y", "--cache-dump")

    @Volatile
    private var process: Process? = null

    private fun binary(context: Context) = File(context.applicationInfo.nativeLibraryDir, BINARY)

    /** False on an ABI the APK carries no ByeDPI for. */
    fun isSupported(context: Context): Boolean = binary(context).exists()

    /** The Xray side of the hop: a SOCKS outbound to the local listener. */
    fun hopProfile(): ProfileItem = ProfileItem.create(EConfigType.SOCKS).apply {
        remarks = "antidpi"
        server = AppConfig.LOOPBACK
        serverPort = AppConfig.PORT_BYEDPI_SOCKS.toString()
    }

    fun isHop(profile: ProfileItem?): Boolean =
        profile?.configType == EConfigType.SOCKS &&
                profile.server == AppConfig.LOOPBACK &&
                profile.serverPort == AppConfig.PORT_BYEDPI_SOCKS.toString()

    /** The user's ByeDPI options, or [DEFAULT_ARGS]. */
    fun userArgs(): String = MmkvManager.decodeSettingsString(AppConfig.PREF_BYEDPI_ARGS).orEmpty().trim().ifEmpty { DEFAULT_ARGS }

    /** [text] split as a shell would, or null when it is empty or sets an option the app owns. */
    fun parseArgs(text: String): List<String>? {
        val words = AetherManager.splitCommand(text.trim())
        if (words.isEmpty()) return null
        if (words.any { w -> w.substringBefore('=') in RESERVED }) return null
        return words
    }

    /** Starts ByeDPI, ending one that runs. Falls back to the defaults when the user's options are bad. */
    @Synchronized
    fun start(context: Context) {
        stop()
        val bin = binary(context)
        if (!bin.exists()) throw IOException("Dr Anti-DPI is not available on this device")
        val options = parseArgs(userArgs()) ?: parseArgs(DEFAULT_ARGS)!!
        val args = listOf(bin.absolutePath, "--ip", AppConfig.LOOPBACK, "--port", AppConfig.PORT_BYEDPI_SOCKS.toString()) + options
        LogUtil.i(AppConfig.TAG, "Anti-DPI: starting with ${options.joinToString(" ")}")
        val started = ProcessBuilder(args).directory(context.cacheDir).redirectErrorStream(true).start()
        process = started
        Thread({
            try {
                started.inputStream.bufferedReader().forEachLine { line -> LogUtil.i(AppConfig.TAG, "Anti-DPI: $line") }
            } catch (_: IOException) {
            }
            LogUtil.i(AppConfig.TAG, "Anti-DPI: ended")
        }, "byedpi-output").apply { isDaemon = true }.start()
        // It listens within milliseconds; Xray's first connections should find it there.
        val deadline = System.currentTimeMillis() + 1_000
        while (!isListening() && alive(started) && System.currentTimeMillis() < deadline) Thread.sleep(20)
        if (!alive(started)) LogUtil.w(AppConfig.TAG, "Anti-DPI: ByeDPI stopped right away (bad options?)")
    }

    @Synchronized
    fun stop() {
        process?.let(::end)
        process = null
    }

    /** Ends ByeDPI off the calling thread, for a disconnect. */
    fun stopInBackground() {
        val p = synchronized(this) { process.also { process = null } } ?: return
        Thread({ end(p) }, "byedpi-stop").apply { isDaemon = true }.start()
    }

    private fun end(p: Process) {
        p.destroy()
        val deadline = System.currentTimeMillis() + 1_000
        while (alive(p) && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(30)
            } catch (_: InterruptedException) {
                break
            }
        }
        if (alive(p)) {
            // destroyForcibly() needs Android 8; on 7 the process is killed by its pid.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                p.destroyForcibly()
            } else {
                runCatching { p.javaClass.getDeclaredField("pid").apply { isAccessible = true }.getInt(p) }
                    .getOrNull()?.let { android.os.Process.killProcess(it) }
            }
        }
    }

    fun isRunning(): Boolean = process?.let(::alive) == true

    fun isListening(): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(AppConfig.LOOPBACK, AppConfig.PORT_BYEDPI_SOCKS), 300); true }
    } catch (_: IOException) {
        false
    }

    private fun alive(p: Process): Boolean = try {
        p.exitValue()
        false
    } catch (_: IllegalThreadStateException) {
        true
    }
}
