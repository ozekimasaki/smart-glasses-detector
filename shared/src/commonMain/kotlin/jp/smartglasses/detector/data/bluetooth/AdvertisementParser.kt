package jp.smartglasses.detector.data.bluetooth

data class ParsedAdvertisement(
    val appearance: Int? = null,
    val completeName: String? = null,
    val shortName: String? = null,
    val serviceUuids: List<String> = emptyList(),
    val companyIds: Set<Int> = emptySet(),
    val deviceClass: Int? = null
) {
    fun merge(other: ParsedAdvertisement): ParsedAdvertisement {
        return ParsedAdvertisement(
            appearance = appearance ?: other.appearance,
            completeName = completeName ?: other.completeName,
            shortName = shortName ?: other.shortName,
            serviceUuids = BleUuid.merge(serviceUuids, other.serviceUuids),
            companyIds = companyIds + other.companyIds,
            deviceClass = deviceClass ?: other.deviceClass
        )
    }
}

object AdvertisementParser {
    const val AD_TYPE_SHORT_NAME = 0x08
    const val AD_TYPE_COMPLETE_NAME = 0x09
    const val AD_TYPE_APPEARANCE = 0x19
    const val AD_TYPE_INCOMPLETE_16BIT_UUIDS = 0x02
    const val AD_TYPE_COMPLETE_16BIT_UUIDS = 0x03
    const val AD_TYPE_INCOMPLETE_32BIT_UUIDS = 0x04
    const val AD_TYPE_COMPLETE_32BIT_UUIDS = 0x05
    const val AD_TYPE_INCOMPLETE_128BIT_UUIDS = 0x06
    const val AD_TYPE_COMPLETE_128BIT_UUIDS = 0x07
    const val AD_TYPE_SOLICITATION_16BIT_UUIDS = 0x14
    const val AD_TYPE_SOLICITATION_128BIT_UUIDS = 0x15
    const val AD_TYPE_SOLICITATION_32BIT_UUIDS = 0x1F
    const val AD_TYPE_SERVICE_DATA_16BIT = 0x16
    const val AD_TYPE_SERVICE_DATA_32BIT = 0x20
    const val AD_TYPE_SERVICE_DATA_128BIT = 0x21
    const val AD_TYPE_BROADCAST_NAME = 0x30
    const val AD_TYPE_CLASS_OF_DEVICE = 0x0D
    const val AD_TYPE_MANUFACTURER_SPECIFIC = 0xFF

    const val APPEARANCE_EYEGLASSES_MIN = 0x01C0
    const val APPEARANCE_EYEGLASSES_MAX = 0x01FF

    private val HEX_DIGITS = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    )

    fun isEyeglassesAppearance(appearance: Int?): Boolean {
        val value = appearance ?: return false
        return value in APPEARANCE_EYEGLASSES_MIN..APPEARANCE_EYEGLASSES_MAX
    }

    fun parse(bytes: ByteArray?): ParsedAdvertisement {
        if (bytes == null || bytes.isEmpty()) {
            return ParsedAdvertisement()
        }

        var appearance: Int? = null
        var completeName: String? = null
        var shortName: String? = null
        var deviceClass: Int? = null
        val serviceUuids = linkedSetOf<String>()
        val companyIds = mutableSetOf<Int>()

        bytes.forEachRecord { type, start, end ->
            consumeRecord(
                type = type,
                data = bytes,
                start = start,
                end = end,
                appearance = { value -> appearance = appearance ?: value },
                completeName = { value -> completeName = completeName ?: value },
                shortName = { value -> shortName = shortName ?: value },
                deviceClass = { value -> deviceClass = deviceClass ?: value },
                serviceUuids = serviceUuids,
                companyIds = companyIds
            )
        }

        return ParsedAdvertisement(
            appearance = appearance,
            completeName = completeName,
            shortName = shortName,
            serviceUuids = serviceUuids.toList(),
            companyIds = companyIds,
            deviceClass = deviceClass
        )
    }

    fun parseAdvertisingDataMap(entries: Map<Int, ByteArray>): ParsedAdvertisement {
        var appearance: Int? = null
        var completeName: String? = null
        var shortName: String? = null
        var deviceClass: Int? = null
        val serviceUuids = linkedSetOf<String>()
        val companyIds = mutableSetOf<Int>()

        for ((type, data) in entries) {
            consumeRecord(
                type = type,
                data = data,
                start = 0,
                end = data.size,
                appearance = { value -> appearance = appearance ?: value },
                completeName = { value -> completeName = completeName ?: value },
                shortName = { value -> shortName = shortName ?: value },
                deviceClass = { value -> deviceClass = deviceClass ?: value },
                serviceUuids = serviceUuids,
                companyIds = companyIds
            )
        }

        return ParsedAdvertisement(
            appearance = appearance,
            completeName = completeName,
            shortName = shortName,
            serviceUuids = serviceUuids.toList(),
            companyIds = companyIds,
            deviceClass = deviceClass
        )
    }

    fun parseHex(hex: String): ParsedAdvertisement {
        return parse(hexToBytes(hex))
    }

    fun hasManufacturerDataSuffix(hex: String, suffix: Int): Boolean {
        return hasManufacturerDataSuffix(hexToBytes(hex), suffix)
    }

    fun hasManufacturerDataSuffix(bytes: ByteArray?, suffix: Int): Boolean {
        if (bytes == null || bytes.isEmpty()) {
            return false
        }
        val high = (suffix shr 8) and 0xFF
        val low = suffix and 0xFF
        var matched = false
        bytes.forEachRecord { type, start, end ->
            if (type == AD_TYPE_MANUFACTURER_SPECIFIC && end - start >= 4) {
                if ((bytes[end - 2].toInt() and 0xFF) == high &&
                    (bytes[end - 1].toInt() and 0xFF) == low
                ) {
                    matched = true
                }
            }
        }
        return matched
    }

    fun asciiFromHex(hex: String): String {
        return asciiFromBytes(hexToBytes(hex) ?: return "")
    }

    fun asciiFromBytes(bytes: ByteArray): String {
        return buildString(bytes.size) {
            for (byte in bytes) {
                val code = byte.toInt() and 0xFF
                if (code in 0x20..0x7E) {
                    append(code.toChar())
                } else {
                    append(' ')
                }
            }
        }
    }

    fun encodeHex(bytes: ByteArray): String {
        if (bytes.isEmpty()) {
            return ""
        }
        val chars = CharArray(bytes.size * 2)
        var index = 0
        for (byte in bytes) {
            val value = byte.toInt() and 0xFF
            chars[index++] = HEX_DIGITS[value ushr 4]
            chars[index++] = HEX_DIGITS[value and 0x0F]
        }
        return chars.concatToString()
    }

    fun encodeManufacturerSpecificTlv(companyId: Int, payload: ByteArray = byteArrayOf()): String {
        return encodeHex(encodeManufacturerSpecificTlvBytes(companyId, payload))
    }

    fun encodeManufacturerSpecificTlvBytes(
        companyId: Int,
        payload: ByteArray = byteArrayOf()
    ): ByteArray {
        val payloadSize = minOf(payload.size, 253)
        val length = 1 + 2 + payloadSize
        val encoded = ByteArray(1 + length)
        encoded[0] = length.toByte()
        encoded[1] = AD_TYPE_MANUFACTURER_SPECIFIC.toByte()
        encoded[2] = (companyId and 0xFF).toByte()
        encoded[3] = ((companyId shr 8) and 0xFF).toByte()
        if (payloadSize > 0) {
            payload.copyInto(encoded, destinationOffset = 4, endIndex = payloadSize)
        }
        return encoded
    }

    fun hexToBytes(hex: String): ByteArray? {
        val normalized = hex.replace(" ", "").replace("_", "")
        val length = normalized.length
        if (length == 0 || length % 2 != 0) {
            return null
        }

        val bytes = ByteArray(length / 2)
        var index = 0
        while (index < length) {
            val high = hexNibble(normalized[index])
            val low = hexNibble(normalized[index + 1])
            if (high < 0 || low < 0) {
                return null
            }
            bytes[index / 2] = ((high shl 4) or low).toByte()
            index += 2
        }
        return bytes
    }

    fun containsBytes(haystack: ByteArray, needle: ByteArray): Boolean {
        if (needle.isEmpty()) {
            return true
        }
        if (needle.size > haystack.size) {
            return false
        }
        val lastStart = haystack.size - needle.size
        outer@ for (start in 0..lastStart) {
            for (index in needle.indices) {
                if (haystack[start + index] != needle[index]) {
                    continue@outer
                }
            }
            return true
        }
        return false
    }

    fun matchesPayloadPattern(
        asciiPayload: String,
        payloadBytes: ByteArray,
        pattern: String,
        compactHex: () -> String
    ): Boolean {
        if (asciiPayload.contains(pattern, ignoreCase = true)) {
            return true
        }
        val compactPattern = pattern.replace("_", "").replace(" ", "").uppercase()
        if (compactPattern.isEmpty()) {
            return false
        }
        if (compactPattern.length % 2 == 0 && compactPattern.all(::isHexChar)) {
            val needle = hexToBytes(compactPattern)
            if (needle != null && containsBytes(payloadBytes, needle)) {
                return true
            }
        }
        return compactHex().contains(compactPattern)
    }

    private fun consumeRecord(
        type: Int,
        data: ByteArray,
        start: Int,
        end: Int,
        appearance: (Int) -> Unit,
        completeName: (String) -> Unit,
        shortName: (String) -> Unit,
        deviceClass: (Int) -> Unit,
        serviceUuids: MutableSet<String>,
        companyIds: MutableSet<Int>
    ) {
        when (type) {
            AD_TYPE_APPEARANCE -> parseAppearance(data, start, end)?.let(appearance)
            AD_TYPE_CLASS_OF_DEVICE -> parseClassOfDevice(data, start, end)?.let(deviceClass)
            AD_TYPE_COMPLETE_NAME,
            AD_TYPE_BROADCAST_NAME -> decodeUtf8(data, start, end)?.let(completeName)
            AD_TYPE_SHORT_NAME -> decodeUtf8(data, start, end)?.let(shortName)
            AD_TYPE_INCOMPLETE_16BIT_UUIDS,
            AD_TYPE_COMPLETE_16BIT_UUIDS,
            AD_TYPE_SOLICITATION_16BIT_UUIDS -> {
                serviceUuids += parseUuid16List(data, start, end)
            }
            AD_TYPE_INCOMPLETE_32BIT_UUIDS,
            AD_TYPE_COMPLETE_32BIT_UUIDS,
            AD_TYPE_SOLICITATION_32BIT_UUIDS -> {
                serviceUuids += parseUuid32List(data, start, end)
            }
            AD_TYPE_INCOMPLETE_128BIT_UUIDS,
            AD_TYPE_COMPLETE_128BIT_UUIDS,
            AD_TYPE_SOLICITATION_128BIT_UUIDS -> {
                serviceUuids += parseUuid128List(data, start, end)
            }
            AD_TYPE_SERVICE_DATA_16BIT -> if (end - start >= 2) {
                serviceUuids += BleUuid.normalize(hex4(unsignedLe16(data, start)))
            }
            AD_TYPE_SERVICE_DATA_32BIT -> if (end - start >= 4) {
                serviceUuids += BleUuid.normalize(hex8(unsignedLe32(data, start)))
            }
            AD_TYPE_SERVICE_DATA_128BIT -> parseUuid128(data, start, end)?.let { uuid ->
                serviceUuids += uuid
            }
            AD_TYPE_MANUFACTURER_SPECIFIC -> if (end - start >= 2) {
                companyIds += unsignedLe16(data, start)
            }
        }
    }

    private inline fun ByteArray.forEachRecord(
        consume: (type: Int, start: Int, end: Int) -> Unit
    ) {
        var offset = 0
        while (offset < size) {
            val length = this[offset].toInt() and 0xFF
            if (length == 0) {
                break
            }

            val recordEnd = offset + 1 + length
            if (recordEnd > size) {
                break
            }

            val type = this[offset + 1].toInt() and 0xFF
            consume(type, offset + 2, recordEnd)
            offset = recordEnd
        }
    }

    private fun parseAppearance(data: ByteArray, start: Int, end: Int): Int? {
        if (end - start < 2) {
            return null
        }
        return unsignedLe16(data, start)
    }

    private fun parseClassOfDevice(data: ByteArray, start: Int, end: Int): Int? {
        if (end - start < 3) {
            return null
        }
        return (data[start].toInt() and 0xFF) or
            ((data[start + 1].toInt() and 0xFF) shl 8) or
            ((data[start + 2].toInt() and 0xFF) shl 16)
    }

    private fun parseUuid16List(data: ByteArray, start: Int, end: Int): List<String> {
        val uuids = mutableListOf<String>()
        var offset = start
        while (offset + 2 <= end) {
            uuids += BleUuid.normalize(hex4(unsignedLe16(data, offset)))
            offset += 2
        }
        return uuids
    }

    private fun parseUuid32List(data: ByteArray, start: Int, end: Int): List<String> {
        val uuids = mutableListOf<String>()
        var offset = start
        while (offset + 4 <= end) {
            uuids += BleUuid.normalize(hex8(unsignedLe32(data, offset)))
            offset += 4
        }
        return uuids
    }

    private fun parseUuid128List(data: ByteArray, start: Int, end: Int): List<String> {
        val uuids = mutableListOf<String>()
        var offset = start
        while (offset + 16 <= end) {
            parseUuid128(data, offset, offset + 16)?.let { uuid -> uuids += uuid }
            offset += 16
        }
        return uuids
    }

    private fun parseUuid128(data: ByteArray, start: Int, end: Int = data.size): String? {
        if (start + 16 > end) {
            return null
        }
        val chars = CharArray(32)
        var index = 0
        for (byteIndex in 15 downTo 0) {
            val value = data[start + byteIndex].toInt() and 0xFF
            chars[index++] = HEX_DIGITS[value ushr 4]
            chars[index++] = HEX_DIGITS[value and 0x0F]
        }
        return BleUuid.normalize(chars.concatToString())
    }

    private fun unsignedLe16(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun hex4(value: Int): String {
        return value.toString(16).uppercase().padStart(4, '0')
    }

    private fun hex8(value: Long): String {
        return (value and 0xFFFFFFFFL).toString(16).uppercase().padStart(8, '0')
    }

    private fun unsignedLe32(data: ByteArray, offset: Int): Long {
        return (data[offset].toLong() and 0xFFL) or
            ((data[offset + 1].toLong() and 0xFFL) shl 8) or
            ((data[offset + 2].toLong() and 0xFFL) shl 16) or
            ((data[offset + 3].toLong() and 0xFFL) shl 24)
    }

    private fun decodeUtf8(data: ByteArray, start: Int, end: Int): String? {
        if (end <= start) {
            return null
        }
        val decoded = data.decodeToString(startIndex = start, endIndex = end).trim { char ->
            char <= ' ' || char == '\u0000'
        }
        return decoded.ifBlank { null }
    }

    private fun hexNibble(char: Char): Int {
        return when (char) {
            in '0'..'9' -> char - '0'
            in 'A'..'F' -> char - 'A' + 10
            in 'a'..'f' -> char - 'a' + 10
            else -> -1
        }
    }

    private fun isHexChar(char: Char): Boolean {
        return hexNibble(char) >= 0
    }
}
