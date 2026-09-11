package jp.smartglasses.detector.data.bluetooth

internal data class ParsedAdvertisement(
    val appearance: Int? = null,
    val completeName: String? = null,
    val shortName: String? = null
)

internal object AdvertisementParser {
    const val AD_TYPE_SHORT_NAME = 0x08
    const val AD_TYPE_COMPLETE_NAME = 0x09
    const val AD_TYPE_APPEARANCE = 0x19

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
                    appearance = (data[0].toInt() and 0xFF) or
                        ((data[1].toInt() and 0xFF) shl 8)
                }
                AD_TYPE_COMPLETE_NAME -> completeName = decodeUtf8(data)
                AD_TYPE_SHORT_NAME -> shortName = decodeUtf8(data)
            }
            offset = recordEnd
        }

        return ParsedAdvertisement(
            appearance = appearance,
            completeName = completeName,
            shortName = shortName
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

    private fun decodeUtf8(data: ByteArray): String? {
        val decoded = data.toString(Charsets.UTF_8).trim { char ->
            char <= ' ' || char == '\u0000'
        }
        return decoded.ifBlank { null }
    }
}
