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
    const val PROFILE_GATT = 7
    const val PROFILE_GATT_SERVER = 8
    const val PROFILE_LE_AUDIO = 22
    const val SDK_LE_AUDIO = 31
    const val POLL_INTERVAL_MS = 15_000L

    fun shouldPoll(
        scanningRequested: Boolean,
        bluetoothEnabled: Boolean,
        connectPermissionGranted: Boolean
    ): Boolean {
        return scanningRequested && bluetoothEnabled && connectPermissionGranted
    }

    fun proxyProfiles(sdkInt: Int): List<Int> {
        val profiles = mutableListOf(PROFILE_HEADSET, PROFILE_A2DP)
        if (sdkInt >= SDK_LE_AUDIO) {
            profiles += PROFILE_LE_AUDIO
        }
        return profiles
    }

    fun managerProfiles(): List<Int> {
        return listOf(PROFILE_GATT, PROFILE_GATT_SERVER)
    }

    fun shouldKeepAddress(address: String): Boolean {
        return address.isNotBlank()
    }
}
