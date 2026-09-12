package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants

object ClassicDiscoveryPolicy {
    const val ACTION_FOUND = "android.bluetooth.device.action.FOUND"
    const val ACTION_NAME_CHANGED = "android.bluetooth.device.action.NAME_CHANGED"
    const val ACTION_CLASS_CHANGED = "android.bluetooth.device.action.CLASS_CHANGED"
    const val ACTION_UUID = "android.bluetooth.device.action.UUID"
    const val ACTION_ALIAS_CHANGED = "android.bluetooth.device.action.ALIAS_CHANGED"

    fun startDelayMs(immediate: Boolean): Long {
        return if (immediate) {
            0L
        } else {
            Constants.CLASSIC_DISCOVERY_DELAY_MS
        }
    }

    fun cancelToRestartDelayMs(): Long {
        return 800L
    }

    fun failedStartRetryDelayMs(): Long {
        return 10_000L
    }

    fun shouldStartClassicDiscovery(alreadyStartedThisSession: Boolean): Boolean {
        return !alreadyStartedThisSession
    }

    fun shouldRefreshClassicDiscovery(
        lastStartedAtMs: Long,
        nowMs: Long,
        intervalMs: Long
    ): Boolean {
        if (lastStartedAtMs <= 0L || intervalMs <= 0L) {
            return false
        }
        return nowMs - lastStartedAtMs >= intervalMs
    }

    fun refreshIntervalMs(appInForeground: Boolean): Long {
        return if (appInForeground) {
            Constants.CLASSIC_DISCOVERY_REFRESH_INTERVAL_MS
        } else {
            Constants.BLE_SCAN_REFRESH_INTERVAL_MS
        }
    }

    fun shouldAttemptAfterBleLaunchFailure(): Boolean {
        return true
    }

    fun shouldRememberSeenAdvertiser(address: String): Boolean {
        return address.isNotBlank()
    }

    fun shouldApplyInquiryUpdate(
        action: String?,
        scanningRequested: Boolean,
        alreadySeenAddress: Boolean,
        deviceClass: Int? = null
    ): Boolean {
        if (!scanningRequested) {
            return false
        }

        return when (action) {
            ACTION_FOUND -> true
            // Classic inquiry または BLE 広告で一度見た機器の遅延名前
            ACTION_NAME_CHANGED -> alreadySeenAddress
            ACTION_ALIAS_CHANGED -> alreadySeenAddress
            ACTION_UUID -> alreadySeenAddress
            ACTION_CLASS_CHANGED ->
                alreadySeenAddress || BluetoothDeviceClassPolicy.isGlassesDeviceClass(deviceClass)
            else -> false
        }
    }

    fun resolveRssi(extraRssi: Int, previouslySeenRssi: Int?): Int {
        return if (extraRssi != Constants.UNKNOWN_RSSI_DBM) {
            extraRssi
        } else {
            previouslySeenRssi ?: Constants.UNKNOWN_RSSI_DBM
        }
    }
}
