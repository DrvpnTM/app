package com.v2ray.ang.ui.advanced

import android.app.Application
import androidx.lifecycle.viewModelScope
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

    private val _range = MutableStateFlow(defaultRange())
    val range: StateFlow<String> = _range.asStateFlow()

    private val _ports = MutableStateFlow(CleanIpScanner.DEFAULT_PORTS.joinToString(","))
    val ports: StateFlow<String> = _ports.asStateFlow()

    private var scanJob: Job? = null

    fun setRange(value: String) {
        _range.value = value
    }

    fun setPorts(value: String) {
        _ports.value = value
    }

    /** Uses the selected server's address to suggest a /24 range to scan. */
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
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            CleanIpScanner.scan(_range.value, ports, progress)
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
