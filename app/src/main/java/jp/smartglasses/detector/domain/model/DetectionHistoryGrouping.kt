package jp.smartglasses.detector.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DetectionHistoryGrouping {
    const val TODAY_LABEL = "今日"
    const val YESTERDAY_LABEL = "昨日"

    fun groupByDate(
        logs: List<DetectionLog>,
        nowMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): Map<String, List<DetectionLog>> {
        if (logs.isEmpty()) {
            return emptyMap()
        }

        val today = startOfDay(nowMs, timeZone)
        val yesterday = startOfDay(nowMs - 24 * 60 * 60 * 1000L, timeZone)
        val dateFormat = SimpleDateFormat("M月d日", locale).apply {
            this.timeZone = timeZone
        }

        return logs.groupBy { log ->
            val logDay = startOfDay(log.detectedAt, timeZone)
            when (logDay) {
                today -> TODAY_LABEL
                yesterday -> YESTERDAY_LABEL
                else -> dateFormat.format(Date(log.detectedAt))
            }
        }
    }

    internal fun startOfDay(timestamp: Long, timeZone: TimeZone): Long {
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
