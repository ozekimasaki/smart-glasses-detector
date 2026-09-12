package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.DetectionMethod
import jp.smartglasses.detector.domain.model.Manufacturer
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.domain.service.BluetoothDeviceClassPolicy
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
    val advertisementBytes: ByteArray = byteArrayOf(),
    val extraPayloadBytes: ByteArray = byteArrayOf(),
    val parsedAdvertisement: ParsedAdvertisement? = null,
    val appearance: Int? = null,
    val deviceClass: Int? = null
)

internal class SmartGlassesClassifier(
    private val detectionRules: List<DetectionRule> = Constants.SMART_GLASSES_DETECTION_RULES,
    private val genericStrongNameRegexes: List<Regex> = Constants.GENERIC_STRONG_GLASSES_NAME_REGEXES,
    private val genericWeakNameRegexes: List<Regex> = Constants.GENERIC_WEAK_GLASSES_NAME_REGEXES,
    private val genericNonGlassesNameRegexes: List<Regex> = Constants.GENERIC_NON_GLASSES_NAME_REGEXES,
    private val genericStrongPayloadRegexes: List<Regex> = Constants.GENERIC_STRONG_PAYLOAD_REGEXES
) {
    private val ruleIndex = DetectionRuleIndex(detectionRules)

    fun classify(
        signal: DetectionSignal,
        sensitivity: ScanSensitivity = ScanSensitivity.BALANCED
    ): SmartGlassesDevice? {
        val parsedAdvertisement = signal.parsedAdvertisement
            ?: AdvertisementParser.parse(signal.advertisementBytesOrHex())
        val resolvedName = signal.deviceName
            ?: parsedAdvertisement.completeName
            ?: parsedAdvertisement.shortName
        val resolvedAppearance = signal.appearance ?: parsedAdvertisement.appearance
        val resolved = signal.copy(
            deviceName = resolvedName,
            appearance = resolvedAppearance,
            deviceClass = BluetoothDeviceClassPolicy.preferGlassesDeviceClass(
                signal.deviceClass,
                parsedAdvertisement.deviceClass
            ),
            serviceUuids = BleUuid.merge(signal.serviceUuids, parsedAdvertisement.serviceUuids),
            companyIds = signal.companyIds + parsedAdvertisement.companyIds,
            parsedAdvertisement = parsedAdvertisement
        )

        val payloadBytes = resolved.payloadBytes()
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
            device = detectByPayload(resolved, payloadBytes),
            matchClass = DetectionMatchClass.CATALOG,
            sensitivity = sensitivity
        ) ?: withEligibleRssi(
            signal = resolved,
            device = detectByManufacturerSuffix(resolved, payloadBytes),
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
            matchClass = DetectionMatchClass.CATALOG,
            sensitivity = sensitivity
        ) ?: withEligibleRssi(
            signal = resolved,
            device = detectByHeuristicPayload(resolved, payloadBytes),
            matchClass = DetectionMatchClass.CATALOG,
            sensitivity = sensitivity
        ) ?: withEligibleRssi(
            signal = resolved,
            device = detectByHeuristicName(resolved, genericStrongNameRegexes),
            matchClass = DetectionMatchClass.CATALOG,
            sensitivity = sensitivity
        ) ?: withEligibleRssi(
            signal = resolved,
            device = detectByHeuristicName(resolved, genericWeakNameRegexes),
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
        if (signal.companyIds.isEmpty()) {
            return null
        }

        for (rule in ruleIndex.companyIdOnlyRules(signal.companyIds)) {
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

        for (rule in ruleIndex.uuidRules(signal.serviceUuids)) {
            val matched = signal.serviceUuids.any { signalUuid ->
                rule.serviceUuids.any { ruleUuid -> BleUuid.matches(signalUuid, ruleUuid) }
            }
            if (!matched || rule.excludesName(signal.deviceName)) {
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

    private fun detectByPayload(
        signal: DetectionSignal,
        payloadBytes: ByteArray
    ): SmartGlassesDevice? {
        if (payloadBytes.isEmpty()) {
            return null
        }

        val asciiPayload = AdvertisementParser.asciiFromBytes(payloadBytes)
        var compactHex: String? = null
        val compactHexProvider = {
            compactHex ?: AdvertisementParser.encodeHex(payloadBytes).also { encoded ->
                compactHex = encoded
            }
        }

        for (rule in ruleIndex.payloadRules()) {
            val matched = rule.payloadPatterns.any { pattern ->
                AdvertisementParser.matchesPayloadPattern(
                    asciiPayload = asciiPayload,
                    payloadBytes = payloadBytes,
                    pattern = pattern,
                    compactHex = compactHexProvider
                )
            }
            if (!matched || rule.excludesName(signal.deviceName)) {
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

    private fun detectByManufacturerSuffix(
        signal: DetectionSignal,
        payloadBytes: ByteArray
    ): SmartGlassesDevice? {
        if (payloadBytes.isEmpty()) {
            return null
        }

        for (rule in ruleIndex.suffixRules()) {
            val matched = rule.manufacturerDataSuffixes.any { suffix ->
                AdvertisementParser.hasManufacturerDataSuffix(payloadBytes, suffix)
            }
            if (!matched || rule.excludesName(signal.deviceName)) {
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

        for (rule in ruleIndex.nameRules()) {
            if (rule.excludesName(deviceName) || !rule.matchesDeviceName(deviceName)) {
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
        val looksLikeGlasses = AdvertisementParser.isEyeglassesAppearance(signal.appearance) ||
            BluetoothDeviceClassPolicy.isGlassesDeviceClass(signal.deviceClass)
        if (!looksLikeGlasses) {
            return null
        }

        val matchingRules = ruleIndex.companyIdRules(signal.companyIds)
        if (matchingRules.isNotEmpty() && matchingRules.all { rule -> rule.excludesName(signal.deviceName) }) {
            return null
        }
        val attributedRule = matchingRules.firstOrNull { rule ->
            !rule.excludesName(signal.deviceName)
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

    private fun detectByHeuristicPayload(
        signal: DetectionSignal,
        payloadBytes: ByteArray
    ): SmartGlassesDevice? {
        if (payloadBytes.isEmpty()) {
            return null
        }
        if (isExcludedHeuristic(signal)) {
            return null
        }

        val asciiPayload = AdvertisementParser.asciiFromBytes(payloadBytes)
        if (genericStrongPayloadRegexes.none { regex -> regex.containsMatchIn(asciiPayload) }) {
            return null
        }

        return SmartGlassesDevice(
            name = signal.deviceName ?: Constants.GENERIC_SMART_GLASSES_NAME,
            address = signal.address,
            manufacturer = Manufacturer(
                id = signal.companyIds.firstOrNull(),
                name = Constants.GENERIC_SMART_GLASSES_NAME,
                detectionMethod = DetectionMethod.HEURISTIC
            ),
            rssi = signal.rssi
        )
    }

    private fun detectByHeuristicName(
        signal: DetectionSignal,
        nameRegexes: List<Regex>
    ): SmartGlassesDevice? {
        val deviceName = signal.deviceName ?: return null
        if (isExcludedHeuristic(signal)) {
            return null
        }
        if (nameRegexes.none { regex -> regex.containsMatchIn(deviceName) }) {
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

    private fun isExcludedHeuristic(signal: DetectionSignal): Boolean {
        val deviceName = signal.deviceName
        if (!deviceName.isNullOrBlank() &&
            genericNonGlassesNameRegexes.any { regex -> regex.containsMatchIn(deviceName) }
        ) {
            return true
        }

        val matchingRules = ruleIndex.companyIdRules(signal.companyIds)
        return matchingRules.isNotEmpty() && matchingRules.all { rule ->
            rule.excludesName(signal.deviceName)
        }
    }

    private fun DetectionRule.matchesDeviceName(deviceName: String): Boolean {
        val byPattern = namePatterns.any { pattern ->
            deviceName.contains(pattern, ignoreCase = true)
        }
        val byRegex = nameRegexes.any { regex ->
            regex.containsMatchIn(deviceName)
        }
        return byPattern || byRegex
    }

    private fun DetectionRule.excludesName(deviceName: String?): Boolean {
        if (deviceName.isNullOrBlank()) {
            return false
        }
        if (excludedNamePatterns.isEmpty() && excludedNameRegexes.isEmpty()) {
            return false
        }

        return excludedNamePatterns.any { pattern ->
            deviceName.contains(pattern, ignoreCase = true)
        } || excludedNameRegexes.any { regex ->
            regex.containsMatchIn(deviceName)
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
    if (advertisementDataHex.isNotEmpty() || extraPayloadHex.isNotEmpty()) {
        return advertisementDataHex + extraPayloadHex
    }
    return AdvertisementParser.encodeHex(advertisementBytes) +
        AdvertisementParser.encodeHex(extraPayloadBytes)
}

internal fun DetectionSignal.payloadBytes(): ByteArray {
    if (advertisementBytes.isNotEmpty() || extraPayloadBytes.isNotEmpty()) {
        if (extraPayloadBytes.isEmpty()) {
            return advertisementBytes
        }
        if (advertisementBytes.isEmpty()) {
            return extraPayloadBytes
        }
        return advertisementBytes + extraPayloadBytes
    }
    return AdvertisementParser.hexToBytes(payloadHex()) ?: byteArrayOf()
}

internal fun DetectionSignal.advertisementBytesOrHex(): ByteArray? {
    if (advertisementBytes.isNotEmpty()) {
        return advertisementBytes
    }
    return AdvertisementParser.hexToBytes(advertisementDataHex)
}
