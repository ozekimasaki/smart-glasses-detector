package jp.smartglasses.detector.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectedDevicePolicyTest {
    @Test
    fun `polls connected profiles only while scanning with bluetooth and connect permission`() {
        assertTrue(
            ConnectedDevicePolicy.shouldPoll(
                scanningRequested = true,
                bluetoothEnabled = true,
                connectPermissionGranted = true
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldPoll(
                scanningRequested = false,
                bluetoothEnabled = true,
                connectPermissionGranted = true
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldPoll(
                scanningRequested = true,
                bluetoothEnabled = false,
                connectPermissionGranted = true
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldPoll(
                scanningRequested = true,
                bluetoothEnabled = true,
                connectPermissionGranted = false
            )
        )
    }

    @Test
    fun `le audio is requested from android 12`() {
        assertEquals(
            listOf(
                ConnectedDevicePolicy.PROFILE_HEADSET,
                ConnectedDevicePolicy.PROFILE_A2DP,
                ConnectedDevicePolicy.PROFILE_HID_HOST
            ),
            ConnectedDevicePolicy.proxyProfiles(sdkInt = 30)
        )
        assertEquals(
            listOf(
                ConnectedDevicePolicy.PROFILE_HEADSET,
                ConnectedDevicePolicy.PROFILE_A2DP,
                ConnectedDevicePolicy.PROFILE_HID_HOST,
                ConnectedDevicePolicy.PROFILE_LE_AUDIO
            ),
            ConnectedDevicePolicy.proxyProfiles(sdkInt = 31)
        )
        assertEquals(31, ConnectedDevicePolicy.SDK_LE_AUDIO)
        assertEquals(15_000L, ConnectedDevicePolicy.POLL_INTERVAL_MS)
    }

    @Test
    fun `gatt manager profiles exclude hearing aids`() {
        assertEquals(
            listOf(
                ConnectedDevicePolicy.PROFILE_GATT,
                ConnectedDevicePolicy.PROFILE_GATT_SERVER
            ),
            ConnectedDevicePolicy.managerProfiles()
        )
        assertTrue(
            ConnectedDevicePolicy.proxyProfiles(sdkInt = 26).contains(
                ConnectedDevicePolicy.PROFILE_HID_HOST
            )
        )
        assertFalse(
            ConnectedDevicePolicy.proxyProfiles(sdkInt = 35).contains(21)
        )
    }

    @Test
    fun `blank addresses are not kept as connected advertisers`() {
        assertFalse(ConnectedDevicePolicy.shouldKeepAddress(""))
        assertFalse(ConnectedDevicePolicy.shouldKeepAddress("   "))
        assertTrue(ConnectedDevicePolicy.shouldKeepAddress("AA:BB:CC:DD:EE:FF"))
    }
}
