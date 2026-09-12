package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.util.Constants
import jp.smartglasses.detector.util.ScanSensitivity

/**
 * iOS / Android から同じ検出ルールで分類するための入口。
 */
object SmartGlassesDetection {
    private val classifier = SmartGlassesClassifier()

    fun classify(
        address: String,
        rssi: Int,
        deviceName: String? = null,
        advertisementBytes: ByteArray = byteArrayOf(),
        companyIds: Set<Int> = emptySet(),
        serviceUuids: List<String> = emptyList(),
        appearance: Int? = null,
        deviceClass: Int? = null,
        sensitivity: ScanSensitivity = ScanSensitivity.BALANCED
    ): SmartGlassesDevice? {
        return classifier.classify(
            DetectionSignal(
                deviceName = deviceName,
                address = address,
                companyIds = companyIds,
                rssi = rssi,
                serviceUuids = serviceUuids,
                advertisementBytes = advertisementBytes,
                appearance = appearance,
                deviceClass = deviceClass
            ),
            sensitivity = sensitivity
        )
    }

    /**
     * Swift / Kotlin Native 向け。ByteArray や Set の相互運用を避け、広告は HEX と CSV で渡す。
     * [appearance] / [deviceClass] は不明なら [UNSET_OPTIONAL_INT]。
     */
    fun classifyAdvertisement(
        address: String,
        rssi: Int,
        deviceName: String?,
        advertisementHex: String,
        extraCompanyIdsCsv: String,
        extraServiceUuidsCsv: String,
        appearance: Int,
        deviceClass: Int,
        sensitivityName: String
    ): SmartGlassesDevice? {
        val advertisementBytes = AdvertisementParser.hexToBytes(advertisementHex) ?: byteArrayOf()
        return classify(
            address = address,
            rssi = rssi,
            deviceName = deviceName,
            advertisementBytes = advertisementBytes,
            companyIds = parseIntCsv(extraCompanyIdsCsv),
            serviceUuids = parseCsv(extraServiceUuidsCsv),
            appearance = appearance.takeUnless { value -> value == UNSET_OPTIONAL_INT },
            deviceClass = deviceClass.takeUnless { value -> value == UNSET_OPTIONAL_INT },
            sensitivity = parseSensitivity(sensitivityName)
        )
    }

    fun catalogServiceUuids(): List<String> {
        return Constants.SMART_GLASSES_DETECTION_RULES
            .flatMap { rule -> rule.serviceUuids }
            .distinct()
    }

    fun catalogServiceUuidsCsv(): String {
        return catalogServiceUuids().joinToString(",")
    }

    private fun parseSensitivity(name: String): ScanSensitivity {
        return ScanSensitivity.entries.firstOrNull { sensitivity ->
            sensitivity.name.equals(name.trim(), ignoreCase = true)
        } ?: ScanSensitivity.BALANCED
    }

    private fun parseIntCsv(value: String): Set<Int> {
        if (value.isBlank()) {
            return emptySet()
        }
        return value.split(',')
            .mapNotNull { token -> token.trim().toIntOrNull() }
            .toSet()
    }

    private fun parseCsv(value: String): List<String> {
        if (value.isBlank()) {
            return emptyList()
        }
        return value.split(',')
            .map { token -> token.trim() }
            .filter { token -> token.isNotEmpty() }
    }

    const val UNSET_OPTIONAL_INT = Int.MIN_VALUE
}
