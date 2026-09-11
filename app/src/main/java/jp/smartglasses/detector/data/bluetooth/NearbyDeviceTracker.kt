package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.util.Constants

internal class NearbyDeviceTracker(
    private val ttlMs: Long = Constants.NEARBY_DEVICE_TTL_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val lock = Any()
    private val devices = LinkedHashMap<String, NearbyEntry>()

    fun record(device: SmartGlassesDevice): List<SmartGlassesDevice> = synchronized(lock) {
        val now = clock()
        devices[buildKey(device)] = NearbyEntry(device = device, seenAt = now)
        snapshotLocked(now)
    }

    fun snapshot(): List<SmartGlassesDevice> = synchronized(lock) {
        snapshotLocked(clock())
    }

    fun clear(): List<SmartGlassesDevice> = synchronized(lock) {
        devices.clear()
        emptyList()
    }

    private fun snapshotLocked(now: Long): List<SmartGlassesDevice> {
        pruneExpiredEntries(now)
        return devices.values
            .sortedByDescending { entry -> entry.device.rssi }
            .map { entry -> entry.device }
    }

    private fun pruneExpiredEntries(now: Long) {
        devices.entries.removeAll { (_, entry) ->
            now - entry.seenAt >= ttlMs
        }
    }

    private fun buildKey(device: SmartGlassesDevice): String {
        val normalizedAddress = device.address.trim().uppercase()
        if (normalizedAddress.isNotEmpty()) {
            return "address:$normalizedAddress"
        }

        return "fallback:${device.manufacturer.name.trim().lowercase()}:${device.name.trim().lowercase()}"
    }

    private data class NearbyEntry(
        val device: SmartGlassesDevice,
        val seenAt: Long
    )
}
