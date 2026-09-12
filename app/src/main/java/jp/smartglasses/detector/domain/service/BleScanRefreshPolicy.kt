package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants

object BleScanRefreshPolicy {
    fun intervalMs(appInForeground: Boolean): Long {
        return if (appInForeground) {
            Constants.BLE_SCAN_FOREGROUND_REFRESH_INTERVAL_MS
        } else {
            Constants.BLE_SCAN_REFRESH_INTERVAL_MS
        }
    }
}
