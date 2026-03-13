package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.DetectionMethod
import jp.smartglasses.detector.domain.model.Manufacturer
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.util.Constants
import jp.smartglasses.detector.util.DetectionRule

internal data class DetectionSignal(
    val deviceName: String?,
    val address: String,
    val companyIds: Set<Int>,
    val rssi: Int,
    val serviceUuids: List<String> = emptyList(),
    val advertisementDataHex: String = ""
)

internal class SmartGlassesClassifier(
    private val detectionRules: List<DetectionRule> = Constants.SMART_GLASSES_DETECTION_RULES,
    private val minDetectionRssiDbm: Int = Constants.MIN_DETECTION_RSSI_DBM
) {
    fun classify(signal: DetectionSignal): SmartGlassesDevice? {
        if (signal.rssi < minDetectionRssiDbm) {
            return null
        }

        return detectByCompanyId(signal) ?: detectByDeviceName(signal)
    }

    private fun detectByCompanyId(signal: DetectionSignal): SmartGlassesDevice? {
        for (rule in detectionRules) {
            if (!rule.allowCompanyIdOnly) {
                continue
            }

            val matchedCompanyId = signal.companyIds.firstOrNull { companyId ->
                companyId in rule.companyIds
            } ?: continue

            return SmartGlassesDevice(
                name = signal.deviceName ?: rule.manufacturerName,
                address = signal.address,
                manufacturer = Manufacturer(
                    id = matchedCompanyId,
                    name = rule.manufacturerName,
                    detectionMethod = DetectionMethod.COMPANY_ID
                ),
                rssi = signal.rssi
            )
        }

        return null
    }

    private fun detectByDeviceName(signal: DetectionSignal): SmartGlassesDevice? {
        val deviceName = signal.deviceName ?: return null

        for (rule in detectionRules) {
            if (rule.namePatterns.none { pattern -> deviceName.contains(pattern, ignoreCase = true) }) {
                continue
            }

            return SmartGlassesDevice(
                name = deviceName,
                address = signal.address,
                manufacturer = Manufacturer(
                    id = null,
                    name = rule.manufacturerName,
                    detectionMethod = DetectionMethod.DEVICE_NAME
                ),
                rssi = signal.rssi
            )
        }

        return null
    }
}
