package jp.smartglasses.detector.domain.service

object ScanResumePolicy {
    fun shouldResume(
        wasScanning: Boolean,
        backgroundEnabled: Boolean,
        hasPermissions: Boolean,
        appInForeground: Boolean = false
    ): Boolean {
        if (!wasScanning || !hasPermissions) {
            return false
        }
        return backgroundEnabled || appInForeground
    }

    fun shouldHandleAction(action: String?, bluetoothState: Int? = null): Boolean {
        if (action == ACTION_BLUETOOTH_STATE_CHANGED) {
            return bluetoothState == BLUETOOTH_STATE_ON
        }

        return action in HANDLED_ACTIONS
    }

    fun shouldKeepScanningIntent(userOrPolicyStop: Boolean): Boolean {
        return !userOrPolicyStop
    }

    fun shouldSwallowBackgroundForegroundStartFailure(fromBackground: Boolean): Boolean {
        return fromBackground
    }

    const val ACTION_BLUETOOTH_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED"
    const val ACTION_LOCATION_MODE_CHANGED = "android.location.MODE_CHANGED"
    const val BLUETOOTH_STATE_ON = 12

    val HANDLED_ACTIONS = setOf(
        "android.intent.action.BOOT_COMPLETED",
        "android.intent.action.MY_PACKAGE_REPLACED",
        "android.intent.action.QUICKBOOT_POWERON",
        "com.htc.intent.action.QUICKBOOT_POWERON",
        ACTION_BLUETOOTH_STATE_CHANGED,
        ACTION_LOCATION_MODE_CHANGED
    )
}
