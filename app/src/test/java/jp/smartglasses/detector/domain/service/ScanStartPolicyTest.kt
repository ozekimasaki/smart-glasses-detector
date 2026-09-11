package jp.smartglasses.detector.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanStartPolicyTest {
    @Test
    fun `ready when hardware bluetooth permissions and optional gates are satisfied`() {
        assertEquals(
            ScanStartRequirement.Ready,
            ScanStartPolicy.evaluate(
                hasBleHardware = true,
                bluetoothEnabled = true,
                hasScanPermissions = true,
                requiresLocationServices = false,
                locationServicesEnabled = false,
                hasNotificationPermission = true,
                notificationPrompted = false
            )
        )
    }

    @Test
    fun `blocks in the documented order`() {
        assertEquals(
            ScanStartRequirement.MissingBleHardware,
            ScanStartPolicy.evaluate(
                hasBleHardware = false,
                bluetoothEnabled = false,
                hasScanPermissions = false,
                requiresLocationServices = true,
                locationServicesEnabled = false,
                hasNotificationPermission = false,
                notificationPrompted = false
            )
        )
        assertEquals(
            ScanStartRequirement.BluetoothDisabled,
            ScanStartPolicy.evaluate(
                hasBleHardware = true,
                bluetoothEnabled = false,
                hasScanPermissions = true,
                requiresLocationServices = false,
                locationServicesEnabled = true,
                hasNotificationPermission = true,
                notificationPrompted = false
            )
        )
        assertEquals(
            ScanStartRequirement.MissingScanPermissions,
            ScanStartPolicy.evaluate(
                hasBleHardware = true,
                bluetoothEnabled = true,
                hasScanPermissions = false,
                requiresLocationServices = false,
                locationServicesEnabled = true,
                hasNotificationPermission = true,
                notificationPrompted = false
            )
        )
        assertEquals(
            ScanStartRequirement.LocationDisabled,
            ScanStartPolicy.evaluate(
                hasBleHardware = true,
                bluetoothEnabled = true,
                hasScanPermissions = true,
                requiresLocationServices = true,
                locationServicesEnabled = false,
                hasNotificationPermission = true,
                notificationPrompted = false
            )
        )
        assertEquals(
            ScanStartRequirement.NotificationPermissionNeeded,
            ScanStartPolicy.evaluate(
                hasBleHardware = true,
                bluetoothEnabled = true,
                hasScanPermissions = true,
                requiresLocationServices = false,
                locationServicesEnabled = true,
                hasNotificationPermission = false,
                notificationPrompted = false
            )
        )
        assertEquals(
            ScanStartRequirement.Ready,
            ScanStartPolicy.evaluate(
                hasBleHardware = true,
                bluetoothEnabled = true,
                hasScanPermissions = true,
                requiresLocationServices = false,
                locationServicesEnabled = true,
                hasNotificationPermission = false,
                notificationPrompted = true
            )
        )
    }

    @Test
    fun `ui stays scanning while the user intent or hardware scan is active`() {
        assertTrue(ScanUiStatePolicy.isScanning(persistedIntent = true, hardwareScanning = false))
        assertTrue(ScanUiStatePolicy.isScanning(persistedIntent = false, hardwareScanning = true))
        assertFalse(ScanUiStatePolicy.isScanning(persistedIntent = false, hardwareScanning = false))
    }

    @Test
    fun `background disable stops the service only after leaving the app`() {
        assertFalse(
            BackgroundScanRuntimePolicy.shouldStopService(
                backgroundEnabled = false,
                appInForeground = true
            )
        )
        assertTrue(
            BackgroundScanRuntimePolicy.shouldStopService(
                backgroundEnabled = false,
                appInForeground = false
            )
        )
        assertFalse(
            BackgroundScanRuntimePolicy.shouldStopService(
                backgroundEnabled = true,
                appInForeground = false
            )
        )
    }
}
