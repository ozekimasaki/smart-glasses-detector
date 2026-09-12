package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicDiscoveryPolicyTest {
    @Test
    fun `classic discovery runs once per scanning session`() {
        assertTrue(ClassicDiscoveryPolicy.shouldStartClassicDiscovery(alreadyStartedThisSession = false))
        assertFalse(ClassicDiscoveryPolicy.shouldStartClassicDiscovery(alreadyStartedThisSession = true))
    }

    @Test
    fun `classic discovery waits so ble advertisements are not starved at start`() {
        assertEquals(15_000L, Constants.CLASSIC_DISCOVERY_DELAY_MS)
    }

    @Test
    fun `classic discovery is refreshed so late-powered classic glasses are not missed`() {
        assertFalse(
            ClassicDiscoveryPolicy.shouldRefreshClassicDiscovery(
                lastStartedAtMs = 0L,
                nowMs = 120_000L,
                intervalMs = Constants.CLASSIC_DISCOVERY_REFRESH_INTERVAL_MS
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldRefreshClassicDiscovery(
                lastStartedAtMs = 10_000L,
                nowMs = 100_000L,
                intervalMs = Constants.CLASSIC_DISCOVERY_REFRESH_INTERVAL_MS
            )
        )
        assertTrue(
            ClassicDiscoveryPolicy.shouldRefreshClassicDiscovery(
                lastStartedAtMs = 10_000L,
                nowMs = 10_000L + Constants.CLASSIC_DISCOVERY_REFRESH_INTERVAL_MS,
                intervalMs = Constants.CLASSIC_DISCOVERY_REFRESH_INTERVAL_MS
            )
        )
        assertEquals(
            Constants.CLASSIC_DISCOVERY_REFRESH_INTERVAL_MS,
            ClassicDiscoveryPolicy.refreshIntervalMs(appInForeground = true)
        )
        assertEquals(
            Constants.BLE_SCAN_REFRESH_INTERVAL_MS,
            ClassicDiscoveryPolicy.refreshIntervalMs(appInForeground = false)
        )
    }
}

