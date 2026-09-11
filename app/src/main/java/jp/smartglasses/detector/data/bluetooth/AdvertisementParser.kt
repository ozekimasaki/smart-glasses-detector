package jp.smartglasses.detector.data.bluetooth

internal data class ParsedAdvertisement(
    val appearance: Int? = null,
    val completeName: String? = null,
    val shortName: String? = null,
    val serviceUuids: List<String> = emptyList(),
    val companyIds: Set<Int> = emptySet()
)

internal object AdvertisementParser {
    const val AD_TYPE_SHORT_NAME = 0x08
    const val AD_TYPE_COMPLETE_NAME = 0x09
    const val AD_TYPE_APPEARANCE = 0x19
    const val AD_TYPE_INCOMPLETE_16BIT_UUIDS = 0x02
    const val AD_TYPE_COMPLETE_16BIT_UUIDS = 0x03
    const val AD_TYPE_INCOMPLETE_32BIT_UUIDS = 0x04
    const val AD_TYPE_COMPLETE_32BIT_UUIDS = 0x05
    const val AD_TYPE_INCOMPLETE_128BIT_UUIDS = 0x06
    const val AD_TYPE_COMPLETE_128BIT_UUIDS = 0x07
    const val AD_TYPE_SERVICE_DATA_16BIT = 0x16
    const val AD_TYPE_SERVICE_DATA_32BIT = 0x20
    const val AD_TYPE_SERVICE_DATA_128BIT = 0x21
    const val AD_TYPE_MANUFACTURER_SPECIFIC = 0xFF

    const val APPEARANCE_EYEGLASSES_MIN = 0x01C0
    const val APPEARANCE_EYEGLASSES_MAX = 0x01FF

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
        val serviceUuids = linkedSetOf<String>()
        val companyIds = mutableSetOf<Int>()
        var offset = 0

        while (offset < bytes.size) {
            val length = bytes[offset].toInt() and 0xFF
            if (length == 0) {
                break
            }

            val recordEnd = offset + 1 + length
            if (recordEnd > bytes.size) {
                break
            }

            val type = bytes[offset + 1].toInt() and 0xFF
            val data = bytes.copyOfRange(offset + 2, recordEnd)
            when (type) {
                AD_TYPE_APPEARANCE -> if (data.size >= 2) {
                    appearance = unsignedLe16(data, 0)
                }
                AD_TYPE_COMPLETE_NAME -> completeName = decodeUtf8(data)
                AD_TYPE_SHORT_NAME -> shortName = decodeUtf8(data)
                AD_TYPE_INCOMPLETE_16BIT_UUIDS,
                AD_TYPE_COMPLETE_16BIT_UUIDS -> {
                    serviceUuids += parseUuid16List(data)
                }
                AD_TYPE_INCOMPLETE_32BIT_UUIDS,
                AD_TYPE_COMPLETE_32BIT_UUIDS -> {
                    serviceUuids += parseUuid32List(data)
                }
                AD_TYPE_INCOMPLETE_128BIT_UUIDS,
                AD_TYPE_COMPLETE_128BIT_UUIDS -> {
                    serviceUuids += parseUuid128List(data)
                }
                AD_TYPE_SERVICE_DATA_16BIT -> if (data.size >= 2) {
                    serviceUuids += BleUuid.normalize("%04X".format(unsignedLe16(data, 0)))
                }
                AD_TYPE_SERVICE_DATA_32BIT -> if (data.size >= 4) {
                    serviceUuids += BleUuid.normalize(hex8(unsignedLe32(data, 0)))
                }
                AD_TYPE_SERVICE_DATA_128BIT -> parseUuid128(data, 0)?.let { uuid ->
                    serviceUuids += uuid
                }
                AD_TYPE_MANUFACTURER_SPECIFIC -> if (data.size >= 2) {
                    companyIds += unsignedLe16(data, 0)
                }
            }
            offset = recordEnd
        }

        return ParsedAdvertisement(
            appearance = appearance,
            completeName = completeName,
            shortName = shortName,
            serviceUuids = serviceUuids.toList(),
            companyIds = companyIds
        )
    }

    fun parseHex(hex: String): ParsedAdvertisement {
        return parse(hexToBytes(hex))
    }

    fun asciiFromHex(hex: String): String {
        val bytes = hexToBytes(hex) ?: return ""
        return buildString {
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

    fun hexToBytes(hex: String): ByteArray? {
        val normalized = hex.replace(" ", "").replace("_", "")
        if (normalized.isEmpty() || normalized.length % 2 != 0) {
            return null
        }
        if (!normalized.all { char ->
                char.isDigit() || char in 'a'..'f' || char in 'A'..'F'
            }
        ) {
            return null
        }

        return ByteArray(normalized.length / 2) { index ->
            normalized.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }

    private fun parseUuid16List(data: ByteArray): List<String> {
        val uuids = mutableListOf<String>()
        var offset = 0
        while (offset + 2 <= data.size) {
            uuids += BleUuid.normalize("%04X".format(unsignedLe16(data, offset)))
            offset += 2
        }
        return uuids
    }

    private fun parseUuid32List(data: ByteArray): List<String> {
        val uuids = mutableListOf<String>()
        var offset = 0
        while (offset + 4 <= data.size) {
            uuids += BleUuid.normalize(hex8(unsignedLe32(data, offset)))
            offset += 4
        }
        return uuids
    }

    private fun parseUuid128List(data: ByteArray): List<String> {
        val uuids = mutableListOf<String>()
        var offset = 0
        while (offset + 16 <= data.size) {
            parseUuid128(data, offset)?.let { uuid -> uuids += uuid }
            offset += 16
        }
        return uuids
    }

    private fun parseUuid128(data: ByteArray, offset: Int): String? {
        if (offset + 16 > data.size) {
            return null
        }
        val bigEndianHex = buildString(32) {
            for (index in 15 downTo 0) {
                append(
                    (data[offset + index].toInt() and 0xFF)
                        .toString(16)
                        .uppercase()
                        .padStart(2, '0')
                )
            }
        }
        return BleUuid.normalize(bigEndianHex)
    }

    private fun unsignedLe16(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8)
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

    private fun decodeUtf8(data: ByteArray): String? {
        val decoded = data.toString(Charsets.UTF_8).trim { char ->
            char <= ' ' || char == '\u0000'
        }
        return decoded.ifBlank { null }
    }
}
