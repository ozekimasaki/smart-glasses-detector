package jp.smartglasses.detector.data.export

import jp.smartglasses.detector.domain.model.DetectionLog
import jp.smartglasses.detector.domain.model.DiagnosticLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticLogExporterSupportTest {
    @Test
    fun `falls back to detection logs when diagnostic logs are empty`() {
        val merged = mergeShareableLogs(
            diagnosticLogs = emptyList(),
            detectionLogs = listOf(
                DetectionLog(
                    deviceName = "Ray-Ban Meta",
                    deviceAddress = "AA:BB:CC:DD:EE:01",
                    manufacturerName = "Meta",
                    rssi = -55,
                    distance = "近い",
                    detectedAt = 20L
                ),
                DetectionLog(
                    deviceName = "Ray-Ban Meta",
                    deviceAddress = "AA:BB:CC:DD:EE:01",
                    manufacturerName = "Meta",
                    rssi = -58,
                    distance = "近い",
                    detectedAt = 10L
                ),
                DetectionLog(
                    deviceName = "XREAL Air",
                    deviceAddress = "AA:BB:CC:DD:EE:02",
                    manufacturerName = "XREAL",
                    rssi = -60,
                    distance = "少し離れている",
                    detectedAt = 30L
                )
            ),
            limit = 10
        )

        assertEquals(2, merged.size)
        assertEquals("XREAL Air", merged[0].advertisedName)
        assertEquals("Ray-Ban Meta", merged[1].advertisedName)
    }

    @Test
    fun `prefers richer diagnostic logs over detection fallback for same device`() {
        val merged = mergeShareableLogs(
            diagnosticLogs = listOf(
                DiagnosticLog(
                    advertisedName = "Ray-Ban Meta",
                    deviceAddress = "AA:BB:CC:DD:EE:01",
                    companyIds = "0x01AB",
                    serviceUuids = "1234",
                    advertisementDataHex = "020106",
                    rssi = -55,
                    detectedAt = 20L
                )
            ),
            detectionLogs = listOf(
                DetectionLog(
                    deviceName = "Ray-Ban Meta",
                    deviceAddress = "AA:BB:CC:DD:EE:01",
                    manufacturerName = "Meta",
                    rssi = -50,
                    distance = "近い",
                    detectedAt = 30L
                )
            ),
            limit = 10
        )

        assertEquals(1, merged.size)
        assertEquals("0x01AB", merged.first().companyIds)
        assertTrue(merged.first().advertisementDataHex.isNotBlank())
    }
}
