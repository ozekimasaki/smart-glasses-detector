package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.DetectionMethod
import jp.smartglasses.detector.domain.model.Manufacturer
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.domain.service.DetectionMatchClass
import jp.smartglasses.detector.domain.service.DetectionRssiPolicy
import jp.smartglasses.detector.util.Constants
import jp.smartglasses.detector.util.DetectionRule
import jp.smartglasses.detector.util.ScanSensitivity

internal data class DetectionSignal(
    val deviceName: String?,
    val address: String,
    val companyIds: Set<Int>,
    val rssi: Int,
    val serviceUuids: List<String> = emptyList(),
    val advertisementDataHex: String = "",
    val extraPayloadHex: String = "",
    val appearance: Int? = null
)

internal class SmartGlassesClassifier(
    private val detectionRules: List<DetectionRule> = Constants.SMART_GLASSES_DETECTION_RULES,
    private val genericNameRegexes: List<Regex> = Constants.GENERIC_GLASSES_NAME_REGEXES,
    private val genericNonGlassesNameRegexes: List<Regex> = Constants.GENERIC_NON_GLASSES_NAME_REGEXES
) {
    fun classify(
        signal: DetectionSignal,
        sensitivity: ScanSensitivity = ScanSensitivity.BALANCED
    ): SmartGlassesDevice? {
        val parsedAdvertisement = AdvertisementParser.parseHex(signal.advertisementDataHex)
        val resolvedName = signal.deviceName
            ?: parsedAdvertisement.completeName
            ?: parsedAdvertisement.shortName
        val resolvedAppearance = signal.appearance ?: parsedAdvertisement.appearance
        val resolved = signal.copy(
            deviceName = resolvedName,
            appearance = resolvedAppearance,
            serviceUuids = (signal.serviceUuids + parsedAdvertisement.serviceUuids).distinct(),
            companyIds = signal.companyIds + parsedAdvertisement.companyIds
        )

        return withEligibleRssi(
            signal = resolved,
            device = detectByCompanyId(resolved),
            matchClass = DetectionMatchClass.CATALOG,
            sensitivity = sensitivity
        ) ?: withEligibleRssi(
            signal = resolved,
            device = detectByServiceUuid(resolved),
            matchClass = DetectionMatchClass.CATALOG,
            sensitivity = sensitivity
        ) ?: withEligibleRssi(
            signal = resolved,
            device = detectByPayload(resolved),
            matchClass = DetectionMatchClass.CATALOG,
            sensitivity = sensitivity
        ) ?: withEligibleRssi(
            signal = resolved,
            device = detectByManufacturerSuffix(resolved),
            matchClass = DetectionMatchClass.CATALOG,
            sensitivity = sensitivity
        ) ?: withEligibleRssi(
            signal = resolved,
            device = detectByDeviceName(resolved),
            matchClass = DetectionMatchClass.CATALOG,
            sensitivity = sensitivity
        ) ?: withEligibleRssi(
            signal = resolved,
            device = detectByAppearance(resolved),
            matchClass = DetectionMatchClass.WEAK,
            sensitivity = sensitivity
        ) ?: withEligibleRssi(
            signal = resolved,
            device = detectByHeuristicName(resolved),
            matchClass = DetectionMatchClass.WEAK,
            sensitivity = sensitivity
        )
    }

    private fun withEligibleRssi(
        signal: DetectionSignal,
        device: SmartGlassesDevice?,
        matchClass: DetectionMatchClass,
        sensitivity: ScanSensitivity
    ): SmartGlassesDevice? {
        if (device == null) {
            return null
        }

        if (!DetectionRssiPolicy.isEligible(signal.rssi, sensitivity, matchClass)) {
            return null
        }

        return device
    }

    private fun detectByCompanyId(signal: DetectionSignal): SmartGlassesDevice? {
        for (rule in detectionRules) {
            if (!rule.allowCompanyIdOnly) {
                continue
            }

            val matchedCompanyId = signal.companyIds.firstOrNull { companyId ->
                companyId in rule.companyIds
            } ?: continue

            if (rule.excludesName(signal.deviceName)) {
                continue
            }

            return toDevice(
                signal = signal,
                rule = rule,
                companyId = matchedCompanyId,
                detectionMethod = DetectionMethod.COMPANY_ID
            )
        }

        return null
    }

    private fun detectByServiceUuid(signal: DetectionSignal): SmartGlassesDevice? {
        if (signal.serviceUuids.isEmpty()) {
            return null
        }

        for (rule in detectionRules) {
            if (rule.serviceUuids.isEmpty()) {
                continue
            }

            val matched = signal.serviceUuids.any { signalUuid ->
                rule.serviceUuids.any { ruleUuid -> BleUuid.matches(signalUuid, ruleUuid) }
            }
            if (!matched) {
                continue
            }

            return toDevice(
                signal = signal,
                rule = rule,
                companyId = signal.companyIds.firstOrNull { companyId ->
                    companyId in rule.companyIds
                },
                detectionMethod = DetectionMethod.SERVICE_UUID
            )
        }

        return null
    }

    private fun detectByPayload(signal: DetectionSignal): SmartGlassesDevice? {
        val payloadHex = signal.payloadHex()
        if (payloadHex.isBlank()) {
            return null
        }

        val asciiPayload = AdvertisementParser.asciiFromHex(payloadHex)
        val compactHex = payloadHex.replace(" ", "").uppercase()

        for (rule in detectionRules) {
            if (rule.payloadPatterns.isEmpty()) {
                continue
            }

            val matched = rule.payloadPatterns.any { pattern ->
                asciiPayload.contains(pattern, ignoreCase = true) ||
                    compactHex.contains(
                        pattern.replace("_", "").replace(" ", "").uppercase(),
                        ignoreCase = false
                    )
            }
            if (!matched) {
                continue
            }

            return toDevice(
                signal = signal,
                rule = rule,
                companyId = signal.companyIds.firstOrNull { companyId ->
                    companyId in rule.companyIds
                },
                detectionMethod = DetectionMethod.PAYLOAD
            )
        }

        return null
    }

    private fun detectByManufacturerSuffix(signal: DetectionSignal): SmartGlassesDevice? {
        val payloadHex = signal.payloadHex()
        if (payloadHex.isBlank()) {
            return null
        }

        for (rule in detectionRules) {
            if (rule.manufacturerDataSuffixes.isEmpty()) {
                continue
            }

            val matched = rule.manufacturerDataSuffixes.any { suffix ->
                AdvertisementParser.hasManufacturerDataSuffix(payloadHex, suffix)
            }
            if (!matched) {
                continue
            }

            return toDevice(
                signal = signal,
                rule = rule,
                companyId = signal.companyIds.firstOrNull { companyId ->
                    companyId in rule.companyIds
                },
                detectionMethod = DetectionMethod.PAYLOAD
            )
        }

        return null
    }

    private fun detectByDeviceName(signal: DetectionSignal): SmartGlassesDevice? {
        val deviceName = signal.deviceName ?: return null

        for (rule in detectionRules) {
            if (rule.namePatterns.none { pattern ->
                    deviceName.contains(pattern, ignoreCase = true)
                }
            ) {
                continue
            }

            return toDevice(
                signal = signal,
                rule = rule,
                companyId = signal.companyIds.firstOrNull { companyId ->
                    companyId in rule.companyIds
                },
                detectionMethod = DetectionMethod.DEVICE_NAME
            )
        }

        return null
    }

    private fun detectByAppearance(signal: DetectionSignal): SmartGlassesDevice? {
        if (!AdvertisementParser.isEyeglassesAppearance(signal.appearance)) {
            return null
        }

        val attributedRule = detectionRules.firstOrNull { rule ->
            signal.companyIds.any { companyId -> companyId in rule.companyIds }
        }

        return SmartGlassesDevice(
            name = signal.deviceName
                ?: attributedRule?.manufacturerName
                ?: Constants.GENERIC_SMART_GLASSES_NAME,
            address = signal.address,
            manufacturer = Manufacturer(
                id = signal.companyIds.firstOrNull { companyId ->
                    attributedRule?.companyIds?.contains(companyId) == true
                } ?: signal.companyIds.firstOrNull(),
                name = attributedRule?.manufacturerName ?: Constants.GENERIC_SMART_GLASSES_NAME,
                detectionMethod = DetectionMethod.APPEARANCE
            ),
            rssi = signal.rssi
        )
    }

    private fun detectByHeuristicName(signal: DetectionSignal): SmartGlassesDevice? {
        val deviceName = signal.deviceName ?: return null
        if (genericNonGlassesNameRegexes.any { regex -> regex.containsMatchIn(deviceName) }) {
            return null
        }
        if (genericNameRegexes.none { regex -> regex.containsMatchIn(deviceName) }) {
            return null
        }

        return SmartGlassesDevice(
            name = deviceName,
            address = signal.address,
            manufacturer = Manufacturer(
                id = null,
                name = Constants.GENERIC_SMART_GLASSES_NAME,
                detectionMethod = DetectionMethod.HEURISTIC
            ),
            rssi = signal.rssi
        )
    }

    private fun DetectionRule.excludesName(deviceName: String?): Boolean {
        if (deviceName.isNullOrBlank() || excludedNamePatterns.isEmpty()) {
            return false
        }

        return excludedNamePatterns.any { pattern ->
            deviceName.contains(pattern, ignoreCase = true)
        }
    }

    private fun toDevice(
        signal: DetectionSignal,
        rule: DetectionRule,
        companyId: Int?,
        detectionMethod: DetectionMethod
    ): SmartGlassesDevice {
        return SmartGlassesDevice(
            name = signal.deviceName ?: rule.manufacturerName,
            address = signal.address,
            manufacturer = Manufacturer(
                id = companyId,
                name = rule.manufacturerName,
                detectionMethod = detectionMethod
            ),
            rssi = signal.rssi
        )
    }
}

internal fun DetectionSignal.payloadHex(): String {
    return advertisementDataHex + extraPayloadHex
}
