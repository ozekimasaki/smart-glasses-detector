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
    fun `classic discovery can start again after a hardware pause`() {
        assertTrue(
            ClassicDiscoveryPolicy.shouldStartClassicDiscovery(alreadyStartedThisSession = false)
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldStartClassicDiscovery(alreadyStartedThisSession = true)
        )
        assertEquals(
            Constants.CLASSIC_DISCOVERY_DELAY_MS,
            ClassicDiscoveryPolicy.startDelayMs(immediate = false)
        )
    }

    @Test
    fun `classic discovery waits so ble advertisements are not starved at start`() {
        assertEquals(15_000L, Constants.CLASSIC_DISCOVERY_DELAY_MS)
        assertEquals(
            Constants.CLASSIC_DISCOVERY_DELAY_MS,
            ClassicDiscoveryPolicy.startDelayMs(immediate = false)
        )
        assertEquals(0L, ClassicDiscoveryPolicy.startDelayMs(immediate = true))
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

    @Test
    fun `name and class updates are applied only for devices already found in inquiry`() {
        assertTrue(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_FOUND,
                scanningRequested = true,
                alreadySeenAddress = false
            )
        )
        assertTrue(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_NAME_CHANGED,
                scanningRequested = true,
                alreadySeenAddress = true
            )
        )
        assertTrue(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_CLASS_CHANGED,
                scanningRequested = true,
                alreadySeenAddress = true
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_NAME_CHANGED,
                scanningRequested = true,
                alreadySeenAddress = false
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_NAME_CHANGED,
                scanningRequested = false,
                alreadySeenAddress = true
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_FOUND,
                scanningRequested = false,
                alreadySeenAddress = true
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = "android.bluetooth.device.action.BOND_STATE_CHANGED",
                scanningRequested = true,
                alreadySeenAddress = true
            )
        )
    }

    @Test
    fun `delayed name updates keep the rssi from the original inquiry`() {
        assertEquals(
            -62,
            ClassicDiscoveryPolicy.resolveRssi(
                extraRssi = Constants.UNKNOWN_RSSI_DBM,
                previouslySeenRssi = -62
            )
        )
        assertEquals(
            -70,
            ClassicDiscoveryPolicy.resolveRssi(
                extraRssi = -70,
                previouslySeenRssi = -62
            )
        )
        assertEquals(
            Constants.UNKNOWN_RSSI_DBM,
            ClassicDiscoveryPolicy.resolveRssi(
                extraRssi = Constants.UNKNOWN_RSSI_DBM,
                previouslySeenRssi = null
            )
        )
    }

    @Test
    fun `inquiry update actions match android bluetooth device broadcasts`() {
        assertEquals("android.bluetooth.device.action.FOUND", ClassicDiscoveryPolicy.ACTION_FOUND)
        assertEquals(
            "android.bluetooth.device.action.NAME_CHANGED",
            ClassicDiscoveryPolicy.ACTION_NAME_CHANGED
        )
        assertEquals(
            "android.bluetooth.device.action.CLASS_CHANGED",
            ClassicDiscoveryPolicy.ACTION_CLASS_CHANGED
        )
    }
}

