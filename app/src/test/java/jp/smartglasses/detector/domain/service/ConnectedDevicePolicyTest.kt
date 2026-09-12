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
        assertEquals(
            listOf(
                ConnectedDevicePolicy.PROFILE_HEADSET,
                ConnectedDevicePolicy.PROFILE_A2DP,
                ConnectedDevicePolicy.PROFILE_HID_HOST,
                ConnectedDevicePolicy.PROFILE_LE_AUDIO,
                ConnectedDevicePolicy.PROFILE_VOLUME_CONTROL,
                ConnectedDevicePolicy.PROFILE_CSIP_SET_COORDINATOR
            ),
            ConnectedDevicePolicy.proxyProfiles(sdkInt = 33)
        )
        assertEquals(31, ConnectedDevicePolicy.SDK_LE_AUDIO)
        assertEquals(33, ConnectedDevicePolicy.SDK_VOLUME_CONTROL)
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
    fun `acl connected events are classified while scanning`() {
        assertTrue(
            ConnectedDevicePolicy.shouldApplyAclConnected(
                action = ConnectedDevicePolicy.ACTION_ACL_CONNECTED,
                scanningRequested = true
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldApplyAclConnected(
                action = ConnectedDevicePolicy.ACTION_ACL_CONNECTED,
                scanningRequested = false
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldApplyAclConnected(
                action = ClassicDiscoveryPolicy.ACTION_FOUND,
                scanningRequested = true
            )
        )
    }

    @Test
    fun `audio and hid profile connections are classified only when connected`() {
        assertTrue(
            ConnectedDevicePolicy.shouldApplyProfileConnected(
                action = ConnectedDevicePolicy.ACTION_A2DP_CONNECTION_STATE_CHANGED,
                connectionState = ConnectedDevicePolicy.STATE_CONNECTED,
                scanningRequested = true
            )
        )
        assertTrue(
            ConnectedDevicePolicy.shouldApplyProfileConnected(
                action = ConnectedDevicePolicy.ACTION_HID_HOST_CONNECTION_STATE_CHANGED,
                connectionState = ConnectedDevicePolicy.STATE_CONNECTED,
                scanningRequested = true
            )
        )
        assertTrue(
            ConnectedDevicePolicy.shouldApplyProfileConnected(
                action = ConnectedDevicePolicy.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED,
                connectionState = ConnectedDevicePolicy.STATE_CONNECTED,
                scanningRequested = true
            )
        )
        assertTrue(
            ConnectedDevicePolicy.shouldApplyProfileConnected(
                action = ConnectedDevicePolicy.ACTION_VOLUME_CONTROL_CONNECTION_STATE_CHANGED,
                connectionState = ConnectedDevicePolicy.STATE_CONNECTED,
                scanningRequested = true
            )
        )
        assertTrue(
            ConnectedDevicePolicy.shouldApplyProfileConnected(
                action = ConnectedDevicePolicy.ACTION_CSIS_CONNECTION_STATE_CHANGED,
                connectionState = ConnectedDevicePolicy.STATE_CONNECTED,
                scanningRequested = true
            )
        )
        assertTrue(
            ConnectedDevicePolicy.shouldApplyAdapterConnected(
                action = ConnectedDevicePolicy.ACTION_ADAPTER_CONNECTION_STATE_CHANGED,
                adapterConnectionState = ConnectedDevicePolicy.STATE_CONNECTED,
                scanningRequested = true
            )
        )
        assertTrue(
            ConnectedDevicePolicy.shouldPollConnectedDevicesOnAdapterConnected(
                action = ConnectedDevicePolicy.ACTION_ADAPTER_CONNECTION_STATE_CHANGED,
                adapterConnectionState = ConnectedDevicePolicy.STATE_CONNECTED,
                scanningRequested = true
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldPollConnectedDevicesOnAdapterConnected(
                action = ConnectedDevicePolicy.ACTION_ADAPTER_CONNECTION_STATE_CHANGED,
                adapterConnectionState = ConnectedDevicePolicy.STATE_CONNECTED,
                scanningRequested = false
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldApplyAdapterConnected(
                action = ConnectedDevicePolicy.ACTION_ADAPTER_CONNECTION_STATE_CHANGED,
                adapterConnectionState = ConnectedDevicePolicy.STATE_DISCONNECTED,
                scanningRequested = true
            )
        )
        assertTrue(
            ConnectedDevicePolicy.shouldClassifyConnectionEvent(
                action = ConnectedDevicePolicy.ACTION_ADAPTER_CONNECTION_STATE_CHANGED,
                scanningRequested = true,
                adapterConnectionState = ConnectedDevicePolicy.STATE_CONNECTED
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldApplyProfileConnected(
                action = ConnectedDevicePolicy.ACTION_HEADSET_CONNECTION_STATE_CHANGED,
                connectionState = ConnectedDevicePolicy.STATE_DISCONNECTED,
                scanningRequested = true
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldApplyProfileConnected(
                action = ConnectedDevicePolicy.ACTION_A2DP_CONNECTION_STATE_CHANGED,
                connectionState = ConnectedDevicePolicy.STATE_CONNECTED,
                scanningRequested = false
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldApplyProfileConnected(
                action = ConnectedDevicePolicy.ACTION_ACL_CONNECTED,
                connectionState = ConnectedDevicePolicy.STATE_CONNECTED,
                scanningRequested = true
            )
        )
        assertTrue(
            ConnectedDevicePolicy.shouldClassifyConnectionEvent(
                action = ConnectedDevicePolicy.ACTION_ACL_CONNECTED,
                scanningRequested = true
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldClassifyConnectionEvent(
                action = ConnectedDevicePolicy.ACTION_A2DP_CONNECTION_STATE_CHANGED,
                scanningRequested = true,
                connectionState = ConnectedDevicePolicy.STATE_DISCONNECTED
            )
        )
        assertEquals(
            listOf(
                ConnectedDevicePolicy.ACTION_ACL_CONNECTED,
                ConnectedDevicePolicy.ACTION_A2DP_CONNECTION_STATE_CHANGED,
                ConnectedDevicePolicy.ACTION_HEADSET_CONNECTION_STATE_CHANGED,
                ConnectedDevicePolicy.ACTION_HID_HOST_CONNECTION_STATE_CHANGED,
                ConnectedDevicePolicy.ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED,
                ConnectedDevicePolicy.ACTION_VOLUME_CONTROL_CONNECTION_STATE_CHANGED,
                ConnectedDevicePolicy.ACTION_CSIS_CONNECTION_STATE_CHANGED,
                ConnectedDevicePolicy.ACTION_ADAPTER_CONNECTION_STATE_CHANGED,
                ConnectedDevicePolicy.ACTION_BATTERY_LEVEL_CHANGED,
                ConnectedDevicePolicy.ACTION_BOND_STATE_CHANGED
            ),
            ConnectedDevicePolicy.connectionBroadcastActions()
        )
        assertEquals(2, ConnectedDevicePolicy.STATE_CONNECTED)
        assertEquals("android.bluetooth.profile.extra.STATE", ConnectedDevicePolicy.EXTRA_STATE)
        assertEquals(
            "android.bluetooth.adapter.extra.CONNECTION_STATE",
            ConnectedDevicePolicy.EXTRA_ADAPTER_CONNECTION_STATE
        )
        assertEquals(
            "android.bluetooth.device.action.BOND_STATE_CHANGED",
            ConnectedDevicePolicy.ACTION_BOND_STATE_CHANGED
        )
        assertEquals(
            "android.bluetooth.device.extra.BOND_STATE",
            ConnectedDevicePolicy.EXTRA_BOND_STATE
        )
        assertEquals(10, ConnectedDevicePolicy.BOND_NONE)
        assertEquals(12, ConnectedDevicePolicy.BOND_BONDED)
        assertTrue(
            ConnectedDevicePolicy.shouldApplyBatteryLevelChanged(
                action = ConnectedDevicePolicy.ACTION_BATTERY_LEVEL_CHANGED,
                scanningRequested = true
            )
        )
        assertTrue(
            ConnectedDevicePolicy.shouldClassifyConnectionEvent(
                action = ConnectedDevicePolicy.ACTION_BATTERY_LEVEL_CHANGED,
                scanningRequested = true
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldApplyBatteryLevelChanged(
                action = ConnectedDevicePolicy.ACTION_BATTERY_LEVEL_CHANGED,
                scanningRequested = false
            )
        )
        assertEquals(
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED",
            ConnectedDevicePolicy.ACTION_BATTERY_LEVEL_CHANGED
        )
        assertTrue(
            ConnectedDevicePolicy.shouldApplyBonded(
                action = ConnectedDevicePolicy.ACTION_BOND_STATE_CHANGED,
                bondState = ConnectedDevicePolicy.BOND_BONDED,
                scanningRequested = true
            )
        )
        assertTrue(
            ConnectedDevicePolicy.shouldClassifyConnectionEvent(
                action = ConnectedDevicePolicy.ACTION_BOND_STATE_CHANGED,
                scanningRequested = true,
                bondState = ConnectedDevicePolicy.BOND_BONDED
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldApplyBonded(
                action = ConnectedDevicePolicy.ACTION_BOND_STATE_CHANGED,
                bondState = ConnectedDevicePolicy.BOND_NONE,
                scanningRequested = true
            )
        )
        assertFalse(
            ConnectedDevicePolicy.shouldApplyBonded(
                action = ConnectedDevicePolicy.ACTION_BOND_STATE_CHANGED,
                bondState = ConnectedDevicePolicy.BOND_BONDED,
                scanningRequested = false
            )
        )
        assertTrue(ConnectedDevicePolicy.shouldRefreshSdpUuids(0))
        assertFalse(ConnectedDevicePolicy.shouldRefreshSdpUuids(1))
    }

    @Test
    fun `blank addresses are not kept as connected advertisers`() {
        assertFalse(ConnectedDevicePolicy.shouldKeepAddress(""))
        assertFalse(ConnectedDevicePolicy.shouldKeepAddress("   "))
        assertTrue(ConnectedDevicePolicy.shouldKeepAddress("AA:BB:CC:DD:EE:FF"))
    }
}
