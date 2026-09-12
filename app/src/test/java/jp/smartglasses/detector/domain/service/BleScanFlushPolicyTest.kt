package jp.smartglasses.detector.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleScanFlushPolicyTest {
    @Test
    fun `flushes pending scan results only while hardware scanning is healthy`() {
        assertTrue(
            BleScanFlushPolicy.shouldFlushPendingResults(
                scanningRequested = true,
                hardwareScanRunning = true,
                bluetoothEnabled = true,
                scanPermissionGranted = true
            )
        )
        assertFalse(
            BleScanFlushPolicy.shouldFlushPendingResults(
                scanningRequested = false,
                hardwareScanRunning = true,
                bluetoothEnabled = true,
                scanPermissionGranted = true
            )
        )
        assertFalse(
            BleScanFlushPolicy.shouldFlushPendingResults(
                scanningRequested = true,
                hardwareScanRunning = false,
                bluetoothEnabled = true,
                scanPermissionGranted = true
            )
        )
        assertFalse(
            BleScanFlushPolicy.shouldFlushPendingResults(
                scanningRequested = true,
                hardwareScanRunning = true,
                bluetoothEnabled = false,
                scanPermissionGranted = true
            )
        )
        assertFalse(
            BleScanFlushPolicy.shouldFlushPendingResults(
                scanningRequested = true,
                hardwareScanRunning = true,
                bluetoothEnabled = true,
                scanPermissionGranted = false
            )
        )
    }
}
