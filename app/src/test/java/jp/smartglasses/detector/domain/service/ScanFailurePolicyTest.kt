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
    fun `transient scan errors are retried`() {
        assertTrue(ScanFailurePolicy.isRecoverable(ScanFailurePolicy.SCAN_FAILED_INTERNAL_ERROR))
        assertTrue(ScanFailurePolicy.isRecoverable(ScanFailurePolicy.SCAN_FAILED_SCANNING_TOO_FREQUENTLY))
        assertTrue(ScanFailurePolicy.shouldKeepScanning(ScanFailurePolicy.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES))
        assertFalse(ScanFailurePolicy.isRecoverable(ScanFailurePolicy.SCAN_FAILED_FEATURE_UNSUPPORTED))
        assertFalse(ScanFailurePolicy.shouldKeepScanning(ScanFailurePolicy.SCAN_FAILED_FEATURE_UNSUPPORTED))
    }

    @Test
    fun `retry delay backs off up to thirty seconds`() {
        assertEquals(1_000L, ScanFailurePolicy.retryDelayMs(1))
        assertEquals(2_000L, ScanFailurePolicy.retryDelayMs(2))
        assertEquals(16_000L, ScanFailurePolicy.retryDelayMs(5))
        assertEquals(30_000L, ScanFailurePolicy.retryDelayMs(8))
    }
}
