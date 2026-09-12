package jp.smartglasses.detector.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DetectionHistoryGroupingTest {
    private val tokyo = TimeZone.getTimeZone("Asia/Tokyo")

    @Test
    fun `empty logs stay empty`() {
        assertEquals(
            emptyMap<String, List<DetectionLog>>(),
            DetectionHistoryGrouping.groupByDate(
                logs = emptyList(),
                nowMs = timestamp(2026, Calendar.SEPTEMBER, 11, 21, 0),
                timeZone = tokyo
            )
        )
    }

    @Test
    fun `groups today yesterday and older dates`() {
        val now = timestamp(2026, Calendar.SEPTEMBER, 11, 21, 0)
        val todayLog = log(timestamp(2026, Calendar.SEPTEMBER, 11, 9, 30))
        val yesterdayLog = log(timestamp(2026, Calendar.SEPTEMBER, 10, 18, 0))
        val olderLog = log(timestamp(2026, Calendar.JANUARY, 15, 12, 0))

        val grouped = DetectionHistoryGrouping.groupByDate(
            logs = listOf(todayLog, yesterdayLog, olderLog),
            nowMs = now,
            timeZone = tokyo,
            locale = Locale.JAPAN
        )

        assertEquals(
            listOf(
                DetectionHistoryGrouping.TODAY_LABEL,
                DetectionHistoryGrouping.YESTERDAY_LABEL,
                "1月15日"
            ),
            grouped.keys.toList()
        )
        assertEquals(listOf(todayLog), grouped[DetectionHistoryGrouping.TODAY_LABEL])
        assertEquals(listOf(yesterdayLog), grouped[DetectionHistoryGrouping.YESTERDAY_LABEL])
        assertEquals(listOf(olderLog), grouped["1月15日"])
    }

    private fun timestamp(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long {
        val calendar = Calendar.getInstance(tokyo)
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun log(detectedAt: Long): DetectionLog {
        return DetectionLog(
            deviceName = "Even G1",
            deviceAddress = "AA:BB:CC:DD:EE:01",
            manufacturerName = "Even Realities",
            rssi = -60,
            distance = "近く",
            detectedAt = detectedAt
        )
    }
}
