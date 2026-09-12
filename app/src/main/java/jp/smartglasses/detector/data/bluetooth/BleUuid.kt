package jp.smartglasses.detector.data.bluetooth

internal object BleUuid {
    private const val BASE_UUID_SUFFIX = "-0000-1000-8000-00805F9B34FB"

    fun normalize(value: String): String {
        val hex = value.trim()
            .removePrefix("urn:uuid:")
            .removePrefix("URN:UUID:")
            .replace("-", "")
            .removePrefix("0x")
            .removePrefix("0X")
            .uppercase()

        return when (hex.length) {
            4 -> "0000$hex$BASE_UUID_SUFFIX"
            8 -> "$hex$BASE_UUID_SUFFIX"
            32 -> format128Bit(hex)
            else -> value.trim().uppercase()
        }
    }

    fun merge(vararg groups: Collection<String>): List<String> {
        val merged = linkedSetOf<String>()
        for (group in groups) {
            for (value in group) {
                if (value.isNotBlank()) {
                    merged += normalize(value)
                }
            }
        }
        return merged.toList()
    }

    fun matches(signalUuid: String, ruleUuid: String): Boolean {
        return normalize(signalUuid) == normalize(ruleUuid)
    }

    private fun format128Bit(hex: String): String {
        return buildString(36) {
            append(hex, 0, 8)
            append('-')
            append(hex, 8, 12)
            append('-')
            append(hex, 12, 16)
            append('-')
            append(hex, 16, 20)
            append('-')
            append(hex, 20, 32)
        }
    }
}
