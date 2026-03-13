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
    private val lastDiagnosticDetections = mutableMapOf<String, Long>()

    fun shouldEmitDetection(deviceKey: String, manufacturerKey: String): Boolean = synchronized(lock) {
        val now = clock()
        pruneExpiredEntries(now)

        val lastDeviceDetection = lastDeviceDetections[deviceKey]
        if (lastDeviceDetection != null && now - lastDeviceDetection < sameDeviceCooldownMs) {
            return false
        }

        val lastManufacturerDetection = lastManufacturerDetections[manufacturerKey]
        if (lastManufacturerDetection != null && now - lastManufacturerDetection < sameManufacturerCooldownMs) {
            return false
        }

        lastDeviceDetections[deviceKey] = now
        lastManufacturerDetections[manufacturerKey] = now
        true
    }

    fun shouldEmitDiagnostic(diagnosticKey: String): Boolean = synchronized(lock) {
        val now = clock()
        pruneExpiredEntries(now)

        val lastDiagnosticDetection = lastDiagnosticDetections[diagnosticKey]
        if (lastDiagnosticDetection != null && now - lastDiagnosticDetection < sameDeviceCooldownMs) {
            return false
        }

        lastDiagnosticDetections[diagnosticKey] = now
        true
    }

    fun clear() = synchronized(lock) {
        lastDeviceDetections.clear()
        lastManufacturerDetections.clear()
        lastDiagnosticDetections.clear()
    }

    private fun pruneExpiredEntries(now: Long) {
        lastDeviceDetections.entries.removeAll { (_, detectedAt) ->
            now - detectedAt >= sameDeviceCooldownMs
        }
        lastManufacturerDetections.entries.removeAll { (_, detectedAt) ->
            now - detectedAt >= sameManufacturerCooldownMs
        }
        lastDiagnosticDetections.entries.removeAll { (_, detectedAt) ->
            now - detectedAt >= sameDeviceCooldownMs
        }
    }
}
