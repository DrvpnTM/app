package com.v2ray.ang.ui.advanced

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.CleanIpScanner
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.ui.base.BaseViewModel
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
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
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
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
    }
}
