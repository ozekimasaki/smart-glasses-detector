package jp.smartglasses.detector.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareScanStatePolicyTest {
    @Test
    fun `hardware scanning is active only while the user wants it and bluetooth is on`() {
        assertTrue(
            HardwareScanStatePolicy.isActive(
                userRequestedScanning = true,
                bluetoothEnabled = true
            )
        )
        assertFalse(
            HardwareScanStatePolicy.isActive(
                userRequestedScanning = true,
                bluetoothEnabled = false
            )
        )
        assertFalse(
            HardwareScanStatePolicy.isActive(
                userRequestedScanning = false,
                bluetoothEnabled = true
            )
        )
        assertFalse(
            HardwareScanStatePolicy.isActive(
                userRequestedScanning = false,
                bluetoothEnabled = false
            )
        )
        assertFalse(
            HardwareScanStatePolicy.isActive(
                userRequestedScanning = true,
                bluetoothEnabled = true,
                scanPermissionGranted = false
            )
        )
        assertFalse(
            HardwareScanStatePolicy.isActive(
                userRequestedScanning = true,
                bluetoothEnabled = true,
                locationServicesSatisfied = false
            )
        )
    }
}
