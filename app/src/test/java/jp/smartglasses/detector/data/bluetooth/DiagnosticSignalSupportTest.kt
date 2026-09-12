package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.DetectionMethod
import jp.smartglasses.detector.domain.model.DiagnosticLog
import jp.smartglasses.detector.domain.model.deduplicationKey
import jp.smartglasses.detector.domain.model.hasPayload
import jp.smartglasses.detector.util.Constants
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
        assertEquals("AA:BB:CC:DD:EE:FF", signal.toDiagnosticLog().deviceAddress)
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
        assertFalse(signal.toDiagnosticLog().hasPayload())
    }

    @Test
    fun `reconstructed manufacturer payload counts as diagnostic payload`() {
        val signal = DetectionSignal(
            deviceName = null,
            address = "",
            companyIds = emptySet(),
            rssi = -60,
            extraPayloadHex = "05FF41523939"
        )

        assertTrue(signal.hasDiagnosticPayload())
        assertEquals("05FF41523939", signal.toDiagnosticLog().advertisementDataHex)
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
    fun `classic discovery signal converts to diagnostic log`() {
        val diagnosticLog = ClassicDiscoverySignal(
            deviceName = "WH-1000XM5",
            address = "11:22:33:44:55:66",
            rssi = -48
        ).toDiagnosticLog(detectedAt = 123L)

        assertEquals("WH-1000XM5", diagnosticLog.advertisedName)
        assertEquals("11:22:33:44:55:66", diagnosticLog.deviceAddress)
        assertEquals("", diagnosticLog.companyIds)
        assertEquals("", diagnosticLog.serviceUuids)
        assertEquals("", diagnosticLog.advertisementDataHex)
        assertEquals(-48, diagnosticLog.rssi)
        assertEquals(123L, diagnosticLog.detectedAt)
    }

    @Test
    fun `classic inquiry extra name is used when cached name is blank`() {
        val signal = ClassicDiscoverySignal(
            deviceName = null,
            extraName = "Nimo-A1B2",
            address = "11:22:33:44:55:77",
            rssi = -52,
            deviceClass = 0x0714
        ).toDetectionSignal()

        assertEquals("Nimo-A1B2", signal.deviceName)
        assertEquals(0x0714, signal.deviceClass)
    }

    @Test
    fun `classic advertised name wins over a stale cached name`() {
        val signal = ClassicDiscoverySignal(
            deviceName = "Unknown",
            extraName = "Solos AirGo3 1234",
            address = "11:22:33:44:55:78",
            rssi = -50
        ).toDetectionSignal()

        assertEquals("Solos AirGo3 1234", signal.deviceName)
    }

    @Test
    fun `classified smart glasses still produce a diagnostic log`() {
        val signal = DetectionSignal(
            deviceName = "Ray-Ban Meta",
            address = "AA:BB:CC:DD:EE:10",
            companyIds = setOf(0x01AB),
            rssi = -60,
            advertisementDataHex = "020106"
        )
        val processed = ScanSignalProcessor().process(signal)

        assertNotNull(processed.detectedDevice)
        assertEquals("Ray-Ban Meta", signal.toDiagnosticLog().advertisedName)
    }

    @Test
    fun `classic delayed extra name classifies mentra nimo`() {
        val signal = ClassicDiscoverySignal(
            deviceName = null,
            address = "AA:BB:CC:DD:EE:99",
            rssi = -55,
            extraName = "NIMO-1234"
        ).toDetectionSignal()
        val processed = ScanSignalProcessor().process(signal)

        assertEquals("Mentra", processed.detectedDevice?.manufacturer?.name)
        assertEquals(DetectionMethod.DEVICE_NAME, processed.detectedDevice?.manufacturer?.detectionMethod)
        assertEquals("NIMO-1234", signal.toDiagnosticLog().advertisedName)
    }

    @Test
    fun `classic connected sdp uuid classifies rokid without advertisement bytes`() {
        val signal = ClassicDiscoverySignal(
            deviceName = null,
            address = "AA:BB:CC:DD:EE:9A",
            rssi = Constants.UNKNOWN_RSSI_DBM,
            serviceUuids = listOf("00009100-0000-1000-8000-00805F9B34FB")
        ).toDetectionSignal()
        val processed = ScanSignalProcessor().process(signal)

        assertEquals("Rokid", processed.detectedDevice?.manufacturer?.name)
        assertEquals(DetectionMethod.SERVICE_UUID, processed.detectedDevice?.manufacturer?.detectionMethod)
        assertEquals("00009100-0000-1000-8000-00805F9B34FB", signal.toDiagnosticLog().serviceUuids)
    }

    @Test
    fun `address-based diagnostic key does not require advertisement hex`() {
        val signal = DetectionSignal(
            deviceName = null,
            address = "aa:bb:cc:dd:ee:ff",
            companyIds = emptySet(),
            rssi = -60,
            advertisementBytes = byteArrayOf(0x02, 0x01, 0x06)
        )

        assertEquals("address:AA:BB:CC:DD:EE:FF", signal.diagnosticDeduplicationKey())
        assertEquals("020106", signal.toDiagnosticLog().advertisementDataHex)
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
