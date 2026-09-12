package jp.smartglasses.detector.domain.service

object ScanResumePolicy {
    fun shouldResume(
        wasScanning: Boolean,
        backgroundEnabled: Boolean,
        hasPermissions: Boolean,
        appInForeground: Boolean = false,
        bluetoothEnabled: Boolean = true,
        locationServicesEnabled: Boolean = true
    ): Boolean {
        if (!wasScanning || !hasPermissions || !bluetoothEnabled || !locationServicesEnabled) {
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

    fun shouldRestartService(hardwareScanning: Boolean): Boolean {
        return !hardwareScanning
    }

    fun shouldPauseForLostEnvironment(
        hasPermissions: Boolean,
        bluetoothEnabled: Boolean,
        locationServicesEnabled: Boolean
    ): Boolean {
        return !hasPermissions || !bluetoothEnabled || !locationServicesEnabled
    }

    fun shouldRestartHardwareScan(
        hasPermissions: Boolean,
        bluetoothEnabled: Boolean,
        locationServicesEnabled: Boolean,
        hardwareScanning: Boolean
    ): Boolean {
        return hasPermissions &&
            bluetoothEnabled &&
            locationServicesEnabled &&
            !hardwareScanning
    }

    fun shouldRefreshHardwareOnDuplicateStart(scanAlreadyActive: Boolean): Boolean {
        return scanAlreadyActive
    }

    fun shouldResetScanSession(alreadyRequested: Boolean): Boolean {
        return !alreadyRequested
    }

    fun shouldRestartAfterRecreation(
        persistedIntent: Boolean,
        backgroundEnabled: Boolean,
        appInForeground: Boolean
    ): Boolean {
        return persistedIntent && (backgroundEnabled || appInForeground)
    }

    fun shouldKeepForegroundServiceDuringEnvironmentPause(
        backgroundEnabled: Boolean,
        appInForeground: Boolean
    ): Boolean {
        return backgroundEnabled || appInForeground
    }

    fun shouldDeferRecreationUntilSettingsReady(settingsReady: Boolean): Boolean {
        return !settingsReady
    }

    fun shouldPromoteForegroundWhileSettingsLoad(
        settingsReady: Boolean,
        explicitStart: Boolean,
        explicitStop: Boolean
    ): Boolean {
        return !settingsReady && !explicitStart && !explicitStop
    }

    fun shouldRestartSticky(
        explicitStart: Boolean,
        explicitStop: Boolean,
        settingsReady: Boolean,
        persistedScanningState: Boolean
    ): Boolean {
        if (explicitStop) {
            return false
        }
        if (explicitStart || !settingsReady) {
            return true
        }
        return persistedScanningState
    }

    const val ACTION_BLUETOOTH_STATE_CHANGED = "android.bluetooth.adapter.action.STATE_CHANGED"
    const val ACTION_LOCATION_MODE_CHANGED = "android.location.MODE_CHANGED"
    const val ACTION_USER_UNLOCKED = "android.intent.action.USER_UNLOCKED"
    const val BLUETOOTH_STATE_ON = 12

    val HANDLED_ACTIONS = setOf(
        "android.intent.action.BOOT_COMPLETED",
        "android.intent.action.MY_PACKAGE_REPLACED",
        "android.intent.action.QUICKBOOT_POWERON",
        "com.htc.intent.action.QUICKBOOT_POWERON",
        ACTION_USER_UNLOCKED,
        ACTION_BLUETOOTH_STATE_CHANGED,
        ACTION_LOCATION_MODE_CHANGED
    )
}
