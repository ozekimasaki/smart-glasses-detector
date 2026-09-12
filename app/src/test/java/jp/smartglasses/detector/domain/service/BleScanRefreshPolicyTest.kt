package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

class BleScanRefreshPolicyTest {
    @Test
    fun `refreshes more often while the app is visible`() {
        assertEquals(
            Constants.BLE_SCAN_FOREGROUND_REFRESH_INTERVAL_MS,
            BleScanRefreshPolicy.intervalMs(appInForeground = true)
        )
        assertEquals(
            45_000L,
            BleScanRefreshPolicy.intervalMs(appInForeground = true)
        )
    }

    @Test
    fun `keeps the same refresh interval while the app is in the background`() {
        assertEquals(
            Constants.BLE_SCAN_FOREGROUND_REFRESH_INTERVAL_MS,
            BleScanRefreshPolicy.intervalMs(appInForeground = false)
        )
        assertEquals(
            45_000L,
            BleScanRefreshPolicy.intervalMs(appInForeground = false)
        )
    }
}
