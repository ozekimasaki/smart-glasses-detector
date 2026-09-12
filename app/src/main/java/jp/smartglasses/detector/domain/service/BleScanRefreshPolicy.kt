package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants

object BleScanRefreshPolicy {
    fun intervalMs(@Suppress("UNUSED_PARAMETER") appInForeground: Boolean): Long {
        // 探索は FGS で続ける。画面オフでも 45 秒で張り直し、OEM の黙停止を待たない。
        return Constants.BLE_SCAN_FOREGROUND_REFRESH_INTERVAL_MS
    }
}
