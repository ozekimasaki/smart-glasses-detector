package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants

object ClassicDiscoveryPolicy {
    const val ACTION_FOUND = "android.bluetooth.device.action.FOUND"
    const val ACTION_NAME_CHANGED = "android.bluetooth.device.action.NAME_CHANGED"
    const val ACTION_CLASS_CHANGED = "android.bluetooth.device.action.CLASS_CHANGED"

    fun startDelayMs(immediate: Boolean): Long {
        return if (immediate) {
            0L
        } else {
            Constants.CLASSIC_DISCOVERY_DELAY_MS
        }
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

    fun shouldApplyInquiryUpdate(
        action: String?,
        scanningRequested: Boolean,
        alreadySeenAddress: Boolean
    ): Boolean {
        if (!scanningRequested) {
            return false
        }

        return when (action) {
            ACTION_FOUND -> true
            ACTION_NAME_CHANGED,
            ACTION_CLASS_CHANGED -> alreadySeenAddress
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
