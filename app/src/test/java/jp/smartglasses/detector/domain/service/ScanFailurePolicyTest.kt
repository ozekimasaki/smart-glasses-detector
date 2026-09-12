package jp.smartglasses.detector.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanFailurePolicyTest {
    @Test
    fun `already started failures are ignored`() {
        assertTrue(ScanFailurePolicy.shouldIgnore(ScanFailurePolicy.SCAN_FAILED_ALREADY_STARTED))
        assertTrue(ScanFailurePolicy.shouldKeepScanning(ScanFailurePolicy.SCAN_FAILED_ALREADY_STARTED))
        assertFalse(ScanFailurePolicy.isRecoverable(ScanFailurePolicy.SCAN_FAILED_ALREADY_STARTED))
    }

    @Test
    fun `unsupported extended advertising keeps scanning for a legacy fallback`() {
        assertTrue(ScanFailurePolicy.shouldFallbackToLegacy(ScanFailurePolicy.SCAN_FAILED_FEATURE_UNSUPPORTED))
        assertTrue(ScanFailurePolicy.shouldKeepScanning(ScanFailurePolicy.SCAN_FAILED_FEATURE_UNSUPPORTED))
        assertFalse(ScanFailurePolicy.isRecoverable(ScanFailurePolicy.SCAN_FAILED_FEATURE_UNSUPPORTED))
    }

    @Test
    fun `transient scan errors are retried`() {
        assertTrue(ScanFailurePolicy.isRecoverable(ScanFailurePolicy.SCAN_FAILED_INTERNAL_ERROR))
        assertTrue(ScanFailurePolicy.isRecoverable(ScanFailurePolicy.SCAN_FAILED_SCANNING_TOO_FREQUENTLY))
        assertTrue(ScanFailurePolicy.shouldKeepScanning(ScanFailurePolicy.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES))
        assertFalse(ScanFailurePolicy.shouldFallbackToLegacy(ScanFailurePolicy.SCAN_FAILED_INTERNAL_ERROR))
    }

    @Test
    fun `retry delay backs off up to thirty seconds`() {
        assertEquals(1_000L, ScanFailurePolicy.retryDelayMs(1))
        assertEquals(2_000L, ScanFailurePolicy.retryDelayMs(2))
        assertEquals(16_000L, ScanFailurePolicy.retryDelayMs(5))
        assertEquals(30_000L, ScanFailurePolicy.retryDelayMs(8))
    }

    @Test
    fun `unknown scan errors do not keep the hardware scan running`() {
        assertFalse(ScanFailurePolicy.shouldKeepScanning(0))
        assertFalse(ScanFailurePolicy.shouldKeepScanning(99))
        assertFalse(ScanFailurePolicy.isRecoverable(0))
        assertFalse(ScanFailurePolicy.shouldFallbackToLegacy(99))
    }

    @Test
    fun `bluetooth disabled is not a recoverable scan error`() {
        assertFalse(
            ScanFailurePolicy.shouldKeepScanning(
                ScanFailurePolicy.SCAN_ENVIRONMENT_BLUETOOTH_DISABLED
            )
        )
        assertFalse(
            ScanFailurePolicy.isRecoverable(
                ScanFailurePolicy.SCAN_ENVIRONMENT_BLUETOOTH_DISABLED
            )
        )
        assertFalse(
            ScanFailurePolicy.shouldIgnore(
                ScanFailurePolicy.SCAN_ENVIRONMENT_BLUETOOTH_DISABLED
            )
        )
    }

    @Test
    fun `stale bluetooth disabled failures do not pause after bluetooth returns`() {
        assertFalse(
            ScanFailurePolicy.shouldPauseScanning(
                ScanFailurePolicy.SCAN_ENVIRONMENT_BLUETOOTH_DISABLED,
                bluetoothEnabled = true
            )
        )
        assertTrue(
            ScanFailurePolicy.shouldPauseScanning(
                ScanFailurePolicy.SCAN_ENVIRONMENT_BLUETOOTH_DISABLED,
                bluetoothEnabled = false
            )
        )
        assertTrue(ScanFailurePolicy.shouldPauseScanning(99, bluetoothEnabled = true))
        assertFalse(
            ScanFailurePolicy.shouldPauseScanning(
                ScanFailurePolicy.SCAN_FAILED_FEATURE_UNSUPPORTED,
                bluetoothEnabled = true
            )
        )
    }
}
