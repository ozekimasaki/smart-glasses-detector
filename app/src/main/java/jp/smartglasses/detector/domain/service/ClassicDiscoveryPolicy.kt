package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants

object ClassicDiscoveryPolicy {
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
}
