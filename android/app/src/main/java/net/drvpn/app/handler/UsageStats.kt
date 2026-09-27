package net.drvpn.app.handler

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Dr VPN daily traffic totals. The VPN process adds each speed-sample delta here; the app reads them
 * for the Usage screen. Stored in the multi-process settings MMKV as "up,down" per day.
 */
object UsageStats {
    private const val KEY_PREFIX = "drvpn_usage_"
    private const val KEEP_DAYS = 62

    data class Day(val date: Date, val up: Long, val down: Long) {
        val total: Long get() = up + down
    }

    private fun dayKey(date: Date) = KEY_PREFIX + SimpleDateFormat("yyyyMMdd", Locale.US).format(date)

    private fun read(date: Date): Pair<Long, Long> {
        val raw = MmkvManager.decodeSettingsString(dayKey(date)).orEmpty()
        val parts = raw.split(',')
        return (parts.getOrNull(0)?.toLongOrNull() ?: 0L) to (parts.getOrNull(1)?.toLongOrNull() ?: 0L)
    }

    /** Adds transferred bytes to today's total. Called from the VPN process. */
    @Synchronized
    fun add(up: Long, down: Long) {
        if (up <= 0L && down <= 0L) return
        val today = Date()
        val (u, d) = read(today)
        MmkvManager.encodeSettings(dayKey(today), "${u + up.coerceAtLeast(0)},${d + down.coerceAtLeast(0)}")
        pruneOld(today)
    }

    private var lastPruneDay = ""

    private fun pruneOld(today: Date) {
        val key = dayKey(today)
        if (key == lastPruneDay) return
        lastPruneDay = key
        val cal = Calendar.getInstance().apply { time = today; add(Calendar.DAY_OF_YEAR, -KEEP_DAYS) }
        repeat(30) {
            MmkvManager.encodeSettings(dayKey(cal.time), "")
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
    }

    /** The last [count] days, oldest first, ending today. */
    fun lastDays(count: Int): List<Day> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(count - 1))
        return (0 until count).map {
            val date = cal.time
            val (u, d) = read(date)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            Day(date, u, d)
        }
    }

    /** Totals for the current calendar month. */
    fun thisMonth(): Day {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        var up = 0L
        var down = 0L
        repeat(today) {
            val (u, d) = read(cal.time)
            up += u
            down += d
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return Day(Date(), up, down)
    }
}
