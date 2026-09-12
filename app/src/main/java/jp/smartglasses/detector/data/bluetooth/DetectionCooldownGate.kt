package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.util.Constants

internal class DetectionCooldownGate(
    private val sameDeviceCooldownMs: Long = Constants.COOLDOWN_SAME_DEVICE_MS,
    private val sameManufacturerCooldownMs: Long = Constants.COOLDOWN_SAME_MANUFACTURER_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val lock = Any()
    private val lastDeviceDetections = mutableMapOf<String, Long>()
    private val lastManufacturerDetections = mutableMapOf<String, Long>()
    private var lastDevicePruneAt = 0L
    private var lastManufacturerPruneAt = 0L

    fun shouldEmitDetection(deviceKey: String, manufacturerKey: String): Boolean = synchronized(lock) {
        val now = clock()
        pruneExpiredEntries(now)

        val lastDeviceDetection = lastDeviceDetections[deviceKey]
        if (lastDeviceDetection != null && now - lastDeviceDetection < sameDeviceCooldownMs) {
            return false
        }

        val distinguishableDevice = deviceKey.startsWith("address:")
        if (!distinguishableDevice) {
            val lastManufacturerDetection = lastManufacturerDetections[manufacturerKey]
            if (lastManufacturerDetection != null &&
                now - lastManufacturerDetection < sameManufacturerCooldownMs
            ) {
                return false
            }
            lastManufacturerDetections[manufacturerKey] = now
        }

        lastDeviceDetections[deviceKey] = now
        true
    }

    fun clear() = synchronized(lock) {
        lastDeviceDetections.clear()
        lastManufacturerDetections.clear()
        lastDevicePruneAt = 0L
        lastManufacturerPruneAt = 0L
    }

    private fun pruneExpiredEntries(now: Long) {
        if (shouldPrune(lastDeviceDetections.size, now, lastDevicePruneAt, sameDeviceCooldownMs)) {
            lastDevicePruneAt = now
            lastDeviceDetections.entries.removeAll { (_, detectedAt) ->
                now - detectedAt >= sameDeviceCooldownMs
            }
        }
        if (
            shouldPrune(
                lastManufacturerDetections.size,
                now,
                lastManufacturerPruneAt,
                sameManufacturerCooldownMs
            )
        ) {
            lastManufacturerPruneAt = now
            lastManufacturerDetections.entries.removeAll { (_, detectedAt) ->
                now - detectedAt >= sameManufacturerCooldownMs
            }
        }
    }

    private fun shouldPrune(
        size: Int,
        now: Long,
        lastPruneAt: Long,
        cooldownMs: Long
    ): Boolean {
        return size >= PRUNE_SIZE_THRESHOLD || now - lastPruneAt >= cooldownMs
    }

    private companion object {
        const val PRUNE_SIZE_THRESHOLD = 64
    }
}
