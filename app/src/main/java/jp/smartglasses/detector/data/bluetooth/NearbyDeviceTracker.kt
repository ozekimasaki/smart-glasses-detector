package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.util.Constants

internal class NearbyDeviceTracker(
    private val ttlMs: Long = Constants.NEARBY_DEVICE_TTL_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val lock = Any()
    private val devices = LinkedHashMap<String, NearbyEntry>()
    private var cachedFingerprint: String? = null
    private var cachedSnapshot: List<SmartGlassesDevice> = emptyList()

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
        cachedFingerprint = null
        cachedSnapshot = emptyList()
        emptyList()
    }

    fun forget(address: String): List<SmartGlassesDevice> = synchronized(lock) {
        val normalizedAddress = address.trim().uppercase()
        if (normalizedAddress.isNotEmpty()) {
            devices.remove("address:$normalizedAddress")
        }
        snapshotLocked(clock())
    }

    private fun snapshotLocked(now: Long): List<SmartGlassesDevice> {
        pruneExpiredEntries(now)
        val fingerprint = buildFingerprint()
        cachedFingerprint?.let { previous ->
            if (previous == fingerprint) {
                return cachedSnapshot
            }
        }
        val snapshot = devices.values
            .sortedByDescending { entry -> entry.device.rssi }
            .map { entry -> entry.device }
        cachedFingerprint = fingerprint
        cachedSnapshot = snapshot
        return snapshot
    }

    private fun buildFingerprint(): String {
        return buildString(devices.size * 24) {
            devices.forEach { (key, entry) ->
                append(key)
                append('\u0001')
                append(entry.device.rssi)
                append('\u0001')
                append(entry.device.name)
                append('\u0002')
            }
        }
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
