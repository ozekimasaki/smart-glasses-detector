package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.DiagnosticLog
import jp.smartglasses.detector.domain.model.deduplicationKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticSignalSupportTest {
    @Test
    fun `advertisement bytes alone count as diagnostic payload`() {
        val signal = DetectionSignal(
            deviceName = null,
            address = "",
            companyIds = emptySet(),
            rssi = -60,
            serviceUuids = emptyList(),
            advertisementDataHex = "020106"
        )

        assertTrue(signal.hasDiagnosticPayload())
    }

    @Test
    fun `device address alone counts as diagnostic payload`() {
        val signal = DetectionSignal(
            deviceName = null,
            address = "AA:BB:CC:DD:EE:FF",
            companyIds = emptySet(),
            rssi = -60,
            serviceUuids = emptyList(),
            advertisementDataHex = ""
        )

        assertTrue(signal.hasDiagnosticPayload())
        assertEquals("AA:BB:CC:DD:EE:FF", signal.toDiagnosticLog()?.deviceAddress)
    }

    @Test
    fun `completely empty signal is ignored`() {
        val signal = DetectionSignal(
            deviceName = null,
            address = "",
            companyIds = emptySet(),
            rssi = -60,
            serviceUuids = emptyList(),
            advertisementDataHex = ""
        )

        assertFalse(signal.hasDiagnosticPayload())
    }

    @Test
    fun `deduplication key uses advertisement data when address is unavailable`() {
        val first = DiagnosticLog(
            advertisedName = "",
            deviceAddress = "",
            companyIds = "",
            serviceUuids = "",
            advertisementDataHex = "020106",
            rssi = -60,
            detectedAt = 1L
        )
        val second = DiagnosticLog(
            advertisedName = "",
            deviceAddress = "",
            companyIds = "",
            serviceUuids = "",
            advertisementDataHex = "020105",
            rssi = -60,
            detectedAt = 2L
        )

        assertNotEquals(first.deduplicationKey(), second.deduplicationKey())
    }

    @Test
    fun `classified smart glasses still produce a diagnostic log`() {
        val processed = ScanSignalProcessor().process(
            DetectionSignal(
                deviceName = "Ray-Ban Meta",
                address = "AA:BB:CC:DD:EE:10",
                companyIds = setOf(0x01AB),
                rssi = -60,
                advertisementDataHex = "020106"
            )
        )

        assertNotNull(processed.detectedDevice)
        assertNotNull(processed.diagnosticLog)
        assertEquals("Ray-Ban Meta", processed.diagnosticLog?.advertisedName)
    }

    @Test
    fun `toDiagnosticLog preserves even an empty signal for diagnostics`() {
        val signal = DetectionSignal(
            deviceName = null,
            address = "",
            companyIds = emptySet(),
            rssi = -60,
            serviceUuids = emptyList(),
            advertisementDataHex = ""
        )

        val diagnosticLog = signal.toDiagnosticLog()

        assertEquals("", diagnosticLog.advertisedName)
        assertEquals("", diagnosticLog.deviceAddress)
        assertEquals("", diagnosticLog.companyIds)
        assertEquals("", diagnosticLog.serviceUuids)
        assertEquals("", diagnosticLog.advertisementDataHex)
        assertEquals(-60, diagnosticLog.rssi)
    }
}
