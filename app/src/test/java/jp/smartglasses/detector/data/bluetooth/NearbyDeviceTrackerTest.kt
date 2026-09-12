package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.DetectionMethod
import jp.smartglasses.detector.domain.model.Manufacturer
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyDeviceTrackerTest {
    @Test
    fun `records replace the same device and keep the strongest first`() {
        var now = 1_000L
        val tracker = NearbyDeviceTracker(ttlMs = 20_000L, clock = { now })

        tracker.record(device(address = "AA:01", rssi = -70, name = "Far"))
        now = 2_000L
        val snapshot = tracker.record(device(address = "AA:02", rssi = -40, name = "Near"))

        assertEquals(listOf("Near", "Far"), snapshot.map { device -> device.name })
    }

    @Test
    fun `same address updates the live snapshot without duplicating`() {
        var now = 1_000L
        val tracker = NearbyDeviceTracker(ttlMs = 20_000L, clock = { now })

        tracker.record(device(address = "AA:01", rssi = -70, name = "Old"))
        now = 2_000L
        val snapshot = tracker.record(device(address = "AA:01", rssi = -45, name = "Updated"))

        assertEquals(1, snapshot.size)
        assertEquals("Updated", snapshot.single().name)
        assertEquals(-45, snapshot.single().rssi)
    }

    @Test
    fun `same rssi name updates still appear in the cached snapshot`() {
        val tracker = NearbyDeviceTracker(ttlMs = 20_000L, clock = { 1_000L })

        tracker.record(device(address = "AA:01", rssi = -50, name = "Old"))
        val snapshot = tracker.record(device(address = "AA:01", rssi = -50, name = "Updated"))

        assertEquals(1, snapshot.size)
        assertEquals("Updated", snapshot.single().name)
        assertEquals(-50, snapshot.single().rssi)
    }

    @Test
    fun `expired devices disappear from the snapshot`() {
        var now = 1_000L
        val tracker = NearbyDeviceTracker(ttlMs = 20_000L, clock = { now })

        tracker.record(device(address = "AA:01", rssi = -50, name = "Stale"))
        now = 21_000L
        val snapshot = tracker.snapshot()

        assertTrue(snapshot.isEmpty())
    }

    @Test
    fun `empty addresses keep different names as separate devices`() {
        val tracker = NearbyDeviceTracker(ttlMs = 20_000L, clock = { 1_000L })

        tracker.record(device(address = "", rssi = -50, name = "Even G1"))
        val snapshot = tracker.record(device(address = "", rssi = -40, name = "INMOAIR3"))

        assertEquals(listOf("INMOAIR3", "Even G1"), snapshot.map { device -> device.name })
    }

    @Test
    fun `clear removes every nearby device`() {
        val tracker = NearbyDeviceTracker(ttlMs = 20_000L, clock = { 1_000L })
        tracker.record(device(address = "AA:01", rssi = -50, name = "Gone"))

        assertTrue(tracker.clear().isEmpty())
        assertTrue(tracker.snapshot().isEmpty())
    }

    @Test
    fun `forget removes a nearby device by address`() {
        val tracker = NearbyDeviceTracker(ttlMs = 20_000L, clock = { 1_000L })
        tracker.record(device(address = "AA:01", rssi = -50, name = "Meta CID"))
        tracker.record(device(address = "AA:02", rssi = -40, name = "Keep"))

        val snapshot = tracker.forget("aa:01")

        assertEquals(listOf("Keep"), snapshot.map { device -> device.name })
    }

    @Test
    fun `nearby ttl covers a foreground classic inquiry interval`() {
        assertTrue(Constants.NEARBY_DEVICE_TTL_MS >= Constants.CLASSIC_DISCOVERY_REFRESH_INTERVAL_MS)
    }

    private fun device(
        address: String,
        rssi: Int,
        name: String
    ): SmartGlassesDevice {
        return SmartGlassesDevice(
            name = name,
            address = address,
            manufacturer = Manufacturer(
                id = 0x01AB,
                name = "Meta Platforms",
                detectionMethod = DetectionMethod.COMPANY_ID
            ),
            rssi = rssi,
            detectedAt = 1L
        )
    }
}
