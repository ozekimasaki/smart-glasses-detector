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
        assertEquals(800L, ClassicDiscoveryPolicy.cancelToRestartDelayMs())
        assertEquals(10_000L, ClassicDiscoveryPolicy.failedStartRetryDelayMs())
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
    fun `name and class updates are applied for devices already seen nearby`() {
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
        assertTrue(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_NAME_CHANGED,
                scanningRequested = true,
                alreadySeenAddress = false,
                deviceClass = BluetoothDeviceClassPolicy.WEARABLE_GLASSES
            )
        )
        assertTrue(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_NAME_CHANGED,
                scanningRequested = true,
                alreadySeenAddress = false,
                extraRssi = -58
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_NAME_CHANGED,
                scanningRequested = true,
                alreadySeenAddress = false,
                deviceClass = 0x0418
            )
        )
        assertTrue(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_UUID,
                scanningRequested = true,
                alreadySeenAddress = true
            )
        )
        assertTrue(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_ALIAS_CHANGED,
                scanningRequested = true,
                alreadySeenAddress = true
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_ALIAS_CHANGED,
                scanningRequested = true,
                alreadySeenAddress = false
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_UUID,
                scanningRequested = true,
                alreadySeenAddress = false
            )
        )
        assertTrue(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_UUID,
                scanningRequested = true,
                alreadySeenAddress = false,
                deviceClass = BluetoothDeviceClassPolicy.AUDIO_VIDEO_GLASSES
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_UUID,
                scanningRequested = true,
                alreadySeenAddress = false,
                extraRssi = -55
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_ALIAS_CHANGED,
                scanningRequested = true,
                alreadySeenAddress = false,
                deviceClass = BluetoothDeviceClassPolicy.WEARABLE_GLASSES
            )
        )
        assertTrue(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_CLASS_CHANGED,
                scanningRequested = true,
                alreadySeenAddress = false,
                deviceClass = BluetoothDeviceClassPolicy.WEARABLE_GLASSES
            )
        )
        assertTrue(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_CLASS_CHANGED,
                scanningRequested = true,
                alreadySeenAddress = false,
                deviceClass = BluetoothDeviceClassPolicy.AUDIO_VIDEO_GLASSES
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                action = ClassicDiscoveryPolicy.ACTION_CLASS_CHANGED,
                scanningRequested = true,
                alreadySeenAddress = false,
                deviceClass = 0x0418
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
        assertEquals(
            "android.bluetooth.device.action.UUID",
            ClassicDiscoveryPolicy.ACTION_UUID
        )
        assertEquals(
            "android.bluetooth.device.action.ALIAS_CHANGED",
            ClassicDiscoveryPolicy.ACTION_ALIAS_CHANGED
        )
        assertEquals(
            "android.bluetooth.adapter.action.DISCOVERY_FINISHED",
            ClassicDiscoveryPolicy.ACTION_DISCOVERY_FINISHED
        )
        assertEquals(
            listOf(
                ClassicDiscoveryPolicy.ACTION_FOUND,
                ClassicDiscoveryPolicy.ACTION_NAME_CHANGED,
                ClassicDiscoveryPolicy.ACTION_CLASS_CHANGED,
                ClassicDiscoveryPolicy.ACTION_UUID,
                ClassicDiscoveryPolicy.ACTION_ALIAS_CHANGED,
                ClassicDiscoveryPolicy.ACTION_DISCOVERY_FINISHED
            ),
            ClassicDiscoveryPolicy.inquiryBroadcastActions()
        )
    }

    @Test
    fun `discovery finished polls connected glasses after inquiry`() {
        assertTrue(
            ClassicDiscoveryPolicy.shouldPollConnectedAfterDiscoveryFinished(
                action = ClassicDiscoveryPolicy.ACTION_DISCOVERY_FINISHED,
                scanningRequested = true
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldPollConnectedAfterDiscoveryFinished(
                action = ClassicDiscoveryPolicy.ACTION_DISCOVERY_FINISHED,
                scanningRequested = false
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldPollConnectedAfterDiscoveryFinished(
                action = ClassicDiscoveryPolicy.ACTION_FOUND,
                scanningRequested = true
            )
        )
    }

    @Test
    fun `glasses class updates are remembered for delayed name and uuid`() {
        assertTrue(
            ClassicDiscoveryPolicy.shouldRememberInquiryAdvertiser(
                action = ClassicDiscoveryPolicy.ACTION_FOUND,
                scanningRequested = true,
                fromConnection = false,
                classify = true
            )
        )
        assertTrue(
            ClassicDiscoveryPolicy.shouldRememberInquiryAdvertiser(
                action = ClassicDiscoveryPolicy.ACTION_CLASS_CHANGED,
                scanningRequested = true,
                fromConnection = false,
                classify = true
            )
        )
        assertTrue(
            ClassicDiscoveryPolicy.shouldRememberInquiryAdvertiser(
                action = ClassicDiscoveryPolicy.ACTION_NAME_CHANGED,
                scanningRequested = true,
                fromConnection = false,
                classify = true
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldRememberInquiryAdvertiser(
                action = ClassicDiscoveryPolicy.ACTION_NAME_CHANGED,
                scanningRequested = true,
                fromConnection = false,
                classify = false
            )
        )
        assertTrue(
            ClassicDiscoveryPolicy.shouldRememberInquiryAdvertiser(
                action = ConnectedDevicePolicy.ACTION_ACL_CONNECTED,
                scanningRequested = true,
                fromConnection = true,
                classify = true
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldRememberInquiryAdvertiser(
                action = ClassicDiscoveryPolicy.ACTION_DISCOVERY_FINISHED,
                scanningRequested = true,
                fromConnection = false,
                classify = true
            )
        )
        assertFalse(
            ClassicDiscoveryPolicy.shouldRememberInquiryAdvertiser(
                action = ClassicDiscoveryPolicy.ACTION_FOUND,
                scanningRequested = false,
                fromConnection = false,
                classify = true
            )
        )
    }

    @Test
    fun `classic inquiry still starts when ble launch fails`() {
        assertTrue(ClassicDiscoveryPolicy.shouldAttemptAfterBleLaunchFailure())
    }

    @Test
    fun `blank addresses are not tracked for delayed name updates`() {
        assertFalse(ClassicDiscoveryPolicy.shouldRememberSeenAdvertiser(""))
        assertFalse(ClassicDiscoveryPolicy.shouldRememberSeenAdvertiser("   "))
        assertTrue(ClassicDiscoveryPolicy.shouldRememberSeenAdvertiser("AA:BB:CC:DD:EE:01"))
    }

    @Test
    fun `classic and connection broadcasts are exported so oem stacks still deliver them`() {
        assertTrue(ClassicDiscoveryPolicy.shouldExportDiscoveryReceiver())
    }
}

