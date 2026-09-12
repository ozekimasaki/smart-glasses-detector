package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeenAdvertiserPolicyTest {
    @Test
    fun `later advertisements keep company ids and replace unknown rssi`() {
        val merged = SeenAdvertiserPolicy.merge(
            existing = SeenAdvertiserPolicy.merge(
                existing = null,
                rssi = -58,
                companyIds = setOf(0x01AB),
                serviceUuids = listOf("0000FD5F-0000-1000-8000-00805F9B34FB")
            ),
            rssi = Constants.UNKNOWN_RSSI_DBM,
            appearance = 0x01C0,
            deviceClass = 0x0714
        )

        assertEquals(-58, merged.rssi)
        assertEquals(setOf(0x01AB), merged.companyIds)
        assertEquals(listOf("0000FD5F-0000-1000-8000-00805F9B34FB"), merged.serviceUuids)
        assertEquals(0x01C0, merged.appearance)
        assertEquals(0x0714, merged.deviceClass)
        assertNull(merged.deviceName)
    }

    @Test
    fun `delayed names stay on the snapshot after a later class change`() {
        val afterName = SeenAdvertiserPolicy.merge(
            existing = SeenAdvertiserPolicy.merge(
                existing = null,
                rssi = -62,
                companyIds = setOf(0x01AB),
                deviceName = null
            ),
            rssi = Constants.UNKNOWN_RSSI_DBM,
            deviceName = "Quest 3"
        )
        val afterClass = SeenAdvertiserPolicy.merge(
            existing = afterName,
            rssi = Constants.UNKNOWN_RSSI_DBM,
            deviceClass = 0x240404
        )

        assertEquals("Quest 3", afterClass.deviceName)
        assertEquals(setOf(0x01AB), afterClass.companyIds)
        assertEquals(0x240404, afterClass.deviceClass)
    }

    @Test
    fun `placeholder names do not erase a previously learned name`() {
        val merged = SeenAdvertiserPolicy.merge(
            existing = SeenAdvertiserPolicy.merge(
                existing = null,
                rssi = -50,
                companyIds = setOf(0x01AB),
                deviceName = "Quest 3"
            ),
            rssi = -48,
            deviceName = "Unknown"
        )

        assertEquals("Quest 3", merged.deviceName)
        assertEquals(-48, merged.rssi)
    }

    @Test
    fun `delayed name updates can retract a nearby false positive`() {
        assertTrue(
            SeenAdvertiserPolicy.shouldForgetNearbyAfterDelayedUpdate(
                action = ClassicDiscoveryPolicy.ACTION_NAME_CHANGED,
                detected = false
            )
        )
        assertTrue(
            SeenAdvertiserPolicy.shouldForgetNearbyAfterDelayedUpdate(
                action = ClassicDiscoveryPolicy.ACTION_CLASS_CHANGED,
                detected = false
            )
        )
        assertFalse(
            SeenAdvertiserPolicy.shouldForgetNearbyAfterDelayedUpdate(
                action = ClassicDiscoveryPolicy.ACTION_FOUND,
                detected = false
            )
        )
        assertFalse(
            SeenAdvertiserPolicy.shouldForgetNearbyAfterDelayedUpdate(
                action = ClassicDiscoveryPolicy.ACTION_NAME_CHANGED,
                detected = true
            )
        )
    }

    @Test
    fun `usable names retract nearby even on later ble advertisements`() {
        assertTrue(
            SeenAdvertiserPolicy.shouldForgetNearbyAfterIdentityUpdate(
                detected = false,
                hasUsableName = true
            )
        )
        assertFalse(
            SeenAdvertiserPolicy.shouldForgetNearbyAfterIdentityUpdate(
                detected = false,
                hasUsableName = false
            )
        )
        assertFalse(
            SeenAdvertiserPolicy.shouldForgetNearbyAfterIdentityUpdate(
                detected = true,
                hasUsableName = true
            )
        )
    }

    @Test
    fun `stale advertiser snapshots expire so rotating addresses do not grow forever`() {
        val fresh = SeenAdvertiserPolicy.merge(
            existing = null,
            rssi = -50,
            companyIds = setOf(0x01AB),
            nowMs = 1_000L
        )
        val expired = SeenAdvertiserPolicy.merge(
            existing = null,
            rssi = -60,
            deviceName = "Quest 3",
            nowMs = 1_000L
        )
        val snapshots = mapOf(
            "AA:01" to fresh.copy(lastSeenAtMs = 119_000L),
            "AA:02" to expired
        )

        assertFalse(
            SeenAdvertiserPolicy.isExpired(
                lastSeenAtMs = 1_000L,
                nowMs = 120_999L,
                ttlMs = 120_000L
            )
        )
        assertTrue(
            SeenAdvertiserPolicy.isExpired(
                lastSeenAtMs = 1_000L,
                nowMs = 121_000L,
                ttlMs = 120_000L
            )
        )
        assertEquals(
            listOf("AA:02"),
            SeenAdvertiserPolicy.expiredAddresses(
                snapshots = snapshots,
                nowMs = 121_000L,
                ttlMs = 120_000L
            )
        )
        assertEquals(1_000L, fresh.lastSeenAtMs)
    }
}
