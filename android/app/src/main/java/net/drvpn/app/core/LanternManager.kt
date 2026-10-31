package net.drvpn.app.core

import android.content.Context
import net.drvpn.app.AppConfig
import net.drvpn.app.dto.entities.ProfileItem
import net.drvpn.app.enums.EConfigType
import net.drvpn.app.util.LogUtil
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Runs lantern-hop (Lantern's client core, built by compile-lantern.sh) next to Xray for the
 * "Silk Road" extra hop. It is a program of its own, shipped as liblanternhop.so in the app's native
 * library folder, and listens for SOCKS on 127.0.0.1:[AppConfig.PORT_LANTERN_SOCKS].
 *
 * Its own traffic leaves directly: the app is outside its own VPN, so this works unless Android's
 * "block connections without VPN" is on. That is also why it can only sit before the server.
 */
object LanternManager {

    private const val BINARY = "liblanternhop.so"
    private const val WORK_DIR = "lantern"
    private val certificateDirectories = listOf("/apex/com.android.conscrypt/cacerts", "/system/etc/security/cacerts")

    @Volatile
    private var process: Process? = null

    private fun binary(context: Context) = File(context.applicationInfo.nativeLibraryDir, BINARY)

    /** False on an ABI the APK carries no lantern-hop for. */
    fun isSupported(context: Context): Boolean = binary(context).exists()

    /** The Xray side of the hop: a SOCKS outbound to the local listener. */
    fun hopProfile(): ProfileItem = ProfileItem.create(EConfigType.SOCKS).apply {
        remarks = "silkroad"
        server = AppConfig.LOOPBACK
        serverPort = AppConfig.PORT_LANTERN_SOCKS.toString()
    }

    fun isHop(profile: ProfileItem?): Boolean =
        profile?.configType == EConfigType.SOCKS &&
                profile.server == AppConfig.LOOPBACK &&
                profile.serverPort == AppConfig.PORT_LANTERN_SOCKS.toString()

    /** Starts the core, ending one that runs. Lantern keeps its device ID in its own folder. */
    @Synchronized
    fun start(context: Context) {
        stop()
        val bin = binary(context)
        if (!bin.exists()) throw IOException("Silk Road is not available on this device")
        val workDir = File(context.filesDir, WORK_DIR).apply { mkdirs() }
        val args = listOf(
            bin.absolutePath,
            "-socks", "${AppConfig.LOOPBACK}:${AppConfig.PORT_LANTERN_SOCKS}",
            "-data", workDir.absolutePath,
            "-log-level", "info",
        )
        LogUtil.i(AppConfig.TAG, "Lantern: starting")
        val builder = ProcessBuilder(args).directory(workDir).redirectErrorStream(true)
        builder.environment().apply {
            put("HOME", workDir.absolutePath)
            put("TMPDIR", context.cacheDir.absolutePath)
            certificateDirectories.filter { File(it).isDirectory }.joinToString(":").takeIf { it.isNotEmpty() }
                ?.let { put("SSL_CERT_DIR", it) }
        }
        val started = builder.start()
        process = started
        Thread({
            try {
                started.inputStream.bufferedReader().forEachLine { line -> LogUtil.i(AppConfig.TAG, "Lantern: $line") }
            } catch (_: IOException) {
            }
            LogUtil.i(AppConfig.TAG, "Lantern: core ended")
        }, "lantern-output").apply { isDaemon = true }.start()
    }

    @Synchronized
    fun stop() {
        process?.let(::end)
        process = null
    }

    /** Ends the core off the calling thread, for a disconnect. */
    fun stopInBackground() {
        val p = synchronized(this) { process.also { process = null } } ?: return
        Thread({ end(p) }, "lantern-stop").apply { isDaemon = true }.start()
    }

    private fun end(p: Process) {
        p.destroy()
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

    fun isRunning(): Boolean = process?.let(::alive) == true

    /** Whether the core answers on its SOCKS port yet (it fetches its servers first). */
    fun isListening(): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(AppConfig.LOOPBACK, AppConfig.PORT_LANTERN_SOCKS), 500); true }
    } catch (_: IOException) {
        false
    }

    private fun alive(p: Process): Boolean = try {
        p.exitValue()
        false
    } catch (_: IllegalThreadStateException) {
        true
    }

    private fun pidOf(p: Process): Int? = try {
        p.javaClass.getDeclaredField("pid").apply { isAccessible = true }.getInt(p)
    } catch (_: ReflectiveOperationException) {
        null
    }
}
