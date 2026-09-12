package jp.smartglasses.detector.domain.service

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanResumePolicyTest {
    @Test
    fun `resumes only when scanning was active background is on and permissions exist`() {
        assertTrue(
            ScanResumePolicy.shouldResume(
                wasScanning = true,
                backgroundEnabled = true,
                hasPermissions = true
            )
        )
        assertFalse(
            ScanResumePolicy.shouldResume(
                wasScanning = false,
                backgroundEnabled = true,
                hasPermissions = true
            )
        )
        assertFalse(
            ScanResumePolicy.shouldResume(
                wasScanning = true,
                backgroundEnabled = false,
                hasPermissions = true
            )
        )
        assertFalse(
            ScanResumePolicy.shouldResume(
                wasScanning = true,
                backgroundEnabled = true,
                hasPermissions = false
            )
        )
    }

    @Test
    fun `does not resume when bluetooth or location services are unavailable`() {
        assertFalse(
            ScanResumePolicy.shouldResume(
                wasScanning = true,
                backgroundEnabled = true,
                hasPermissions = true,
                bluetoothEnabled = false
            )
        )
        assertFalse(
            ScanResumePolicy.shouldResume(
                wasScanning = true,
                backgroundEnabled = true,
                hasPermissions = true,
                locationServicesEnabled = false
            )
        )
        assertTrue(
            ScanResumePolicy.shouldResume(
                wasScanning = true,
                backgroundEnabled = true,
                hasPermissions = true,
                bluetoothEnabled = true,
                locationServicesEnabled = true
            )
        )
    }

    @Test
    fun `resumes persisted scanning when app is visible even without background`() {
        assertTrue(
            ScanResumePolicy.shouldResume(
                wasScanning = true,
                backgroundEnabled = false,
                hasPermissions = true,
                appInForeground = true
            )
        )
        assertFalse(
            ScanResumePolicy.shouldResume(
                wasScanning = true,
                backgroundEnabled = false,
                hasPermissions = true,
                appInForeground = false
            )
        )
        assertFalse(
            ScanResumePolicy.shouldResume(
                wasScanning = false,
                backgroundEnabled = false,
                hasPermissions = true,
                appInForeground = true
            )
        )
        assertFalse(
            ScanResumePolicy.shouldResume(
                wasScanning = true,
                backgroundEnabled = false,
                hasPermissions = false,
                appInForeground = true
            )
        )
    }

    @Test
    fun `handles boot and package replaced actions`() {
        assertTrue(ScanResumePolicy.shouldHandleAction(Intent.ACTION_BOOT_COMPLETED))
        assertTrue(ScanResumePolicy.shouldHandleAction(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertTrue(ScanResumePolicy.shouldHandleAction(Intent.ACTION_USER_UNLOCKED))
        assertTrue(ScanResumePolicy.shouldHandleAction("android.intent.action.QUICKBOOT_POWERON"))
        assertTrue(ScanResumePolicy.shouldHandleAction("com.htc.intent.action.QUICKBOOT_POWERON"))
        assertFalse(ScanResumePolicy.shouldHandleAction(Intent.ACTION_LOCKED_BOOT_COMPLETED))
        assertFalse(ScanResumePolicy.shouldHandleAction(Intent.ACTION_SCREEN_ON))
        assertFalse(ScanResumePolicy.shouldHandleAction(null))
    }

    @Test
    fun `handles location mode changes so pre-S restore can resume`() {
        assertTrue(
            ScanResumePolicy.shouldHandleAction(ScanResumePolicy.ACTION_LOCATION_MODE_CHANGED)
        )
    }

    @Test
    fun `resumes when bluetooth turns on`() {
        assertTrue(
            ScanResumePolicy.shouldHandleAction(
                ScanResumePolicy.ACTION_BLUETOOTH_STATE_CHANGED,
                ScanResumePolicy.BLUETOOTH_STATE_ON
            )
        )
        assertFalse(
            ScanResumePolicy.shouldHandleAction(
                ScanResumePolicy.ACTION_BLUETOOTH_STATE_CHANGED,
                10
            )
        )
        assertFalse(
            ScanResumePolicy.shouldHandleAction(
                ScanResumePolicy.ACTION_BLUETOOTH_STATE_CHANGED
            )
        )
    }

    @Test
    fun `keeps scanning intent after unexpected stop so foreground resume can restart`() {
        assertTrue(ScanResumePolicy.shouldKeepScanningIntent(userOrPolicyStop = false))
        assertFalse(ScanResumePolicy.shouldKeepScanningIntent(userOrPolicyStop = true))
    }

    @Test
    fun `permission or location loss is an environmental pause that keeps intent`() {
        assertTrue(ScanResumePolicy.shouldKeepScanningIntent(userOrPolicyStop = false))
        assertTrue(
            ScanResumePolicy.shouldResume(
                wasScanning = true,
                backgroundEnabled = true,
                hasPermissions = true
            )
        )
    }

    @Test
    fun `background foreground-service start failures are deferred until visible`() {
        assertTrue(ScanResumePolicy.shouldSwallowBackgroundForegroundStartFailure(fromBackground = true))
        assertFalse(ScanResumePolicy.shouldSwallowBackgroundForegroundStartFailure(fromBackground = false))
    }

    @Test
    fun `does not restart the service when hardware is already scanning`() {
        assertFalse(ScanResumePolicy.shouldRestartService(hardwareScanning = true))
        assertTrue(ScanResumePolicy.shouldRestartService(hardwareScanning = false))
    }

    @Test
    fun `foreground-only scanning resumes when the app becomes visible again`() {
        assertTrue(
            ScanResumePolicy.shouldKeepScanningIntent(userOrPolicyStop = false)
        )
        assertTrue(
            ScanResumePolicy.shouldResume(
                wasScanning = true,
                backgroundEnabled = false,
                hasPermissions = true,
                appInForeground = true
            )
        )
        assertFalse(
            ScanResumePolicy.shouldResume(
                wasScanning = true,
                backgroundEnabled = false,
                hasPermissions = true,
                appInForeground = false
            )
        )
    }

    @Test
    fun `os recreation of the service resumes while visible even without background`() {
        assertTrue(
            ScanResumePolicy.shouldRestartAfterRecreation(
                persistedIntent = true,
                backgroundEnabled = false,
                appInForeground = true
            )
        )
        assertTrue(
            ScanResumePolicy.shouldRestartAfterRecreation(
                persistedIntent = true,
                backgroundEnabled = true,
                appInForeground = false
            )
        )
        assertFalse(
            ScanResumePolicy.shouldRestartAfterRecreation(
                persistedIntent = true,
                backgroundEnabled = false,
                appInForeground = false
            )
        )
        assertFalse(
            ScanResumePolicy.shouldRestartAfterRecreation(
                persistedIntent = false,
                backgroundEnabled = true,
                appInForeground = true
            )
        )
    }
}
