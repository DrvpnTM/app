package net.drvpn.app.handler

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dr VPN in-app activity log shown from the drawer. Entries are kept for 10 minutes and then dropped,
 * so it stays a short, friendly recent history rather than a growing technical dump.
 */
object AppLog {
    private const val RETENTION_MS = 10 * 60 * 1000L

    private data class Entry(val at: Long, val text: String)
    private val buffer = ArrayDeque<Entry>()
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun add(text: String) {
        val now = System.currentTimeMillis()
        synchronized(buffer) {
            buffer.addLast(Entry(now, text))
            prune(now)
            publish()
        }
    }

    fun clear() {
        synchronized(buffer) {
            buffer.clear()
            _lines.value = emptyList()
        }
    }

    /** Drops entries older than the retention window; call before reading to keep it fresh. */
    fun refresh() {
        synchronized(buffer) {
            prune(System.currentTimeMillis())
            publish()
        }
    }

    private fun prune(now: Long) {
        val cutoff = now - RETENTION_MS
        while (buffer.isNotEmpty() && buffer.first().at < cutoff) buffer.removeFirst()
    }

    private fun publish() {
        _lines.value = buffer.map { "${fmt.format(Date(it.at))}  ${it.text}" }
    }
}
