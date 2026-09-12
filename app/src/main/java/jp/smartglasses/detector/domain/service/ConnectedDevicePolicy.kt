package jp.smartglasses.detector.domain.service

/**
 * 広告を止めたあとも接続中のグラスを拾うための方針。
 *
 * Android の [android.bluetooth.BluetoothProfile] 定数と一致させる。
 * domain 層は Android 型に依存しない。
 */
object ConnectedDevicePolicy {
    const val PROFILE_HEADSET = 1
    const val PROFILE_A2DP = 2
    const val PROFILE_HID_HOST = 4
    const val PROFILE_GATT = 7
    const val PROFILE_GATT_SERVER = 8
    const val PROFILE_LE_AUDIO = 22
    const val PROFILE_VOLUME_CONTROL = 23
    const val PROFILE_CSIP_SET_COORDINATOR = 25
    const val SDK_LE_AUDIO = 31
    const val SDK_VOLUME_CONTROL = 33
    const val POLL_INTERVAL_MS = 15_000L
    const val STATE_DISCONNECTED = 0
    const val STATE_CONNECTED = 2
    const val ACTION_ACL_CONNECTED = "android.bluetooth.device.action.ACL_CONNECTED"
    const val ACTION_A2DP_CONNECTION_STATE_CHANGED =
        "android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED"
    const val ACTION_HEADSET_CONNECTION_STATE_CHANGED =
        "android.bluetooth.headset.profile.action.CONNECTION_STATE_CHANGED"
    const val ACTION_HID_HOST_CONNECTION_STATE_CHANGED =
        "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED"
    const val ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED =
        "android.bluetooth.action.LE_AUDIO_CONNECTION_STATE_CHANGED"
    const val ACTION_VOLUME_CONTROL_CONNECTION_STATE_CHANGED =
        "android.bluetooth.volume-control.profile.action.CONNECTION_STATE_CHANGED"
    const val ACTION_CSIS_CONNECTION_STATE_CHANGED =
        "android.bluetooth.action.CSIS_CONNECTION_STATE_CHANGED"
    const val ACTION_ADAPTER_CONNECTION_STATE_CHANGED =
        "android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED"
    const val ACTION_BATTERY_LEVEL_CHANGED =
        "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
    const val ACTION_BOND_STATE_CHANGED = "android.bluetooth.device.action.BOND_STATE_CHANGED"
    const val EXTRA_STATE = "android.bluetooth.profile.extra.STATE"
    const val EXTRA_ADAPTER_CONNECTION_STATE = "android.bluetooth.adapter.extra.CONNECTION_STATE"
    const val EXTRA_BOND_STATE = "android.bluetooth.device.extra.BOND_STATE"
    const val BOND_NONE = 10
    const val BOND_BONDED = 12

    val PROFILE_CONNECTION_ACTIONS = setOf(
        ACTION_A2DP_CONNECTION_STATE_CHANGED,
        ACTION_HEADSET_CONNECTION_STATE_CHANGED,
        ACTION_HID_HOST_CONNECTION_STATE_CHANGED,
        ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED,
        ACTION_VOLUME_CONTROL_CONNECTION_STATE_CHANGED,
        ACTION_CSIS_CONNECTION_STATE_CHANGED
    )

    fun connectionBroadcastActions(): List<String> {
        return listOf(
            ACTION_ACL_CONNECTED,
            ACTION_A2DP_CONNECTION_STATE_CHANGED,
            ACTION_HEADSET_CONNECTION_STATE_CHANGED,
            ACTION_HID_HOST_CONNECTION_STATE_CHANGED,
            ACTION_LE_AUDIO_CONNECTION_STATE_CHANGED,
            ACTION_VOLUME_CONTROL_CONNECTION_STATE_CHANGED,
            ACTION_CSIS_CONNECTION_STATE_CHANGED,
            ACTION_ADAPTER_CONNECTION_STATE_CHANGED,
            ACTION_BATTERY_LEVEL_CHANGED,
            ACTION_BOND_STATE_CHANGED
        )
    }

    fun shouldPoll(
        scanningRequested: Boolean,
        bluetoothEnabled: Boolean,
        connectPermissionGranted: Boolean
    ): Boolean {
        return scanningRequested && bluetoothEnabled && connectPermissionGranted
    }

    fun proxyProfiles(sdkInt: Int): List<Int> {
        val profiles = mutableListOf(PROFILE_HEADSET, PROFILE_A2DP, PROFILE_HID_HOST)
        if (sdkInt >= SDK_LE_AUDIO) {
            profiles += PROFILE_LE_AUDIO
        }
        if (sdkInt >= SDK_VOLUME_CONTROL) {
            profiles += PROFILE_VOLUME_CONTROL
            profiles += PROFILE_CSIP_SET_COORDINATOR
        }
        return profiles
    }

    fun managerProfiles(): List<Int> {
        return listOf(PROFILE_GATT, PROFILE_GATT_SERVER)
    }

    fun shouldKeepAddress(address: String): Boolean {
        return address.isNotBlank()
    }

    fun shouldApplyAclConnected(action: String?, scanningRequested: Boolean): Boolean {
        return scanningRequested && action == ACTION_ACL_CONNECTED
    }

    fun shouldApplyBatteryLevelChanged(action: String?, scanningRequested: Boolean): Boolean {
        return scanningRequested && action == ACTION_BATTERY_LEVEL_CHANGED
    }

    fun shouldApplyProfileConnected(
        action: String?,
        connectionState: Int,
        scanningRequested: Boolean
    ): Boolean {
        return scanningRequested &&
            action in PROFILE_CONNECTION_ACTIONS &&
            connectionState == STATE_CONNECTED
    }

    fun shouldApplyAdapterConnected(
        action: String?,
        adapterConnectionState: Int,
        scanningRequested: Boolean
    ): Boolean {
        return scanningRequested &&
            action == ACTION_ADAPTER_CONNECTION_STATE_CHANGED &&
            adapterConnectionState == STATE_CONNECTED
    }

    fun shouldPollConnectedDevicesOnAdapterConnected(
        action: String?,
        adapterConnectionState: Int,
        scanningRequested: Boolean
    ): Boolean {
        return shouldApplyAdapterConnected(action, adapterConnectionState, scanningRequested)
    }

    fun shouldApplyBonded(
        action: String?,
        bondState: Int,
        scanningRequested: Boolean
    ): Boolean {
        return scanningRequested &&
            action == ACTION_BOND_STATE_CHANGED &&
            bondState == BOND_BONDED
    }

    fun shouldClassifyConnectionEvent(
        action: String?,
        scanningRequested: Boolean,
        connectionState: Int = STATE_DISCONNECTED,
        adapterConnectionState: Int = STATE_DISCONNECTED,
        bondState: Int = BOND_NONE
    ): Boolean {
        return shouldApplyAclConnected(action, scanningRequested) ||
            shouldApplyProfileConnected(action, connectionState, scanningRequested) ||
            shouldApplyAdapterConnected(action, adapterConnectionState, scanningRequested) ||
            shouldApplyBatteryLevelChanged(action, scanningRequested) ||
            shouldApplyBonded(action, bondState, scanningRequested)
    }

    fun shouldRefreshSdpUuids(cachedUuidCount: Int): Boolean {
        return cachedUuidCount <= 0
    }
}
