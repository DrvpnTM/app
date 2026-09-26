package com.v2ray.ang.ui.advanced

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.AppLog
import com.v2ray.ang.handler.CleanIpScanner
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.base.BaseViewModel
import com.v2ray.ang.R
import com.v2ray.ang.extension.toast
import com.v2ray.ang.extension.toastSuccess
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress

class AdvancedScanViewModel(application: Application) : BaseViewModel(application) {

    val progress = MutableStateFlow(CleanIpScanner.Progress())

    private val _range = MutableStateFlow(savedRange() ?: defaultRange())
    val range: StateFlow<String> = _range.asStateFlow()

    private val _ports = MutableStateFlow(CleanIpScanner.DEFAULT_PORTS.joinToString(","))
    val ports: StateFlow<String> = _ports.asStateFlow()

    /** Host index to resume from for the current range; 0 means start from the beginning. */
    private val _resumeIndex = MutableStateFlow(savedOffset())
    val resumeIndex: StateFlow<Long> = _resumeIndex.asStateFlow()

    private var scanJob: Job? = null

    private fun log(text: String) = AppLog.add(text)

    init {
        // Show the total for the current range up front so the counter reads 1 … N.
        progress.value = progress.value.copy(
            total = CleanIpScanner.totalHosts(_range.value),
            done = _resumeIndex.value,
        )
    }

    fun setRange(value: String) {
        _range.value = value
        resetResume()
        progress.value = progress.value.copy(total = CleanIpScanner.totalHosts(value), done = 0)
    }

    fun setPorts(value: String) {
        _ports.value = value
    }

    fun useCloudflareRanges() = setRange(CleanIpScanner.CLOUDFLARE_RANGES.joinToString(","))

    fun useServerRange() = setRange(defaultRange())

    /** "Whole world": every routable IPv4 address, scanned in full (the user stops when satisfied). */
    fun useWorldRange() = setRange("0.0.0.0/0")

    private fun savedRange(): String? =
        MmkvManager.decodeSettingsString(AppConfig.PREF_DRVPN_SCAN_RANGE)?.takeIf { it.isNotBlank() }

    private fun savedOffset(): Long =
        MmkvManager.decodeSettingsString(AppConfig.PREF_DRVPN_SCAN_OFFSET)?.toLongOrNull() ?: 0L

    private fun resetResume() {
        _resumeIndex.value = 0L
        MmkvManager.encodeSettings(AppConfig.PREF_DRVPN_SCAN_OFFSET, "0")
    }

    private fun defaultRange(): String {
        val guid = MmkvManager.getSelectServer() ?: return ""
        val host = MmkvManager.decodeServerConfig(guid)?.server?.trim().orEmpty()
        if (host.isEmpty()) return ""
        val ip = if (Utils.isPureIpAddress(host)) host else runCatching {
            InetAddress.getByName(host).hostAddress
        }.getOrNull().orEmpty()
        val octets = ip.split(".")
        return if (octets.size == 4) "${octets[0]}.${octets[1]}.${octets[2]}.0/24" else ""
    }

    fun startScan() {
        if (progress.value.running) return
        val ports = _ports.value.split(",", " ", "\n")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..65535 }
            .distinct()
        val rangeText = _range.value
        val start = _resumeIndex.value
        MmkvManager.encodeSettings(AppConfig.PREF_DRVPN_SCAN_RANGE, rangeText)
        log(app.getString(R.string.advanced_scan_log_started))
        var lastFound = progress.value.found.size
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            launch {
                progress.collect { p ->
                    if (p.found.size > lastFound) {
                        p.found.drop(lastFound).forEach { r ->
                            log("${r.ip}:${r.port} · ${r.latencyMs} ms")
                        }
                        lastFound = p.found.size
                    }
                    if (!p.running && p.total > 0 && p.done >= p.total) {
                        log(app.getString(R.string.advanced_scan_log_finished))
                    }
                }
            }
            CleanIpScanner.scan(rangeText, ports, start, progress) { next ->
                _resumeIndex.value = next
                MmkvManager.encodeSettings(AppConfig.PREF_DRVPN_SCAN_OFFSET, next.toString())
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        progress.value = progress.value.copy(running = false)
        log(app.getString(R.string.advanced_scan_log_stopped))
    }

    /**
     * Rewrites the currently selected server's address/port to [ip]:[port] and reports whether the
     * caller should restart the service. Returns true on success.
     */
    fun applyToSelectedServer(ip: String, port: Int): Boolean {
        val guid = MmkvManager.getSelectServer()
        if (guid == null) {
            toast(R.string.advanced_scan_no_selected)
            return false
        }
        val profile = MmkvManager.decodeServerConfig(guid)
        if (profile == null) {
            toast(R.string.advanced_scan_no_selected)
            return false
        }
        profile.server = ip
        profile.serverPort = port.toString()
        MmkvManager.encodeServerConfig(guid, profile)
        toastSuccess(R.string.advanced_scan_applied)
        return true
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
