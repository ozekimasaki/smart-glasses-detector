package jp.smartglasses.detector.domain.model

import jp.smartglasses.detector.util.currentTimeMillis

data class SmartGlassesDevice(
    val name: String,
    val address: String,
    val manufacturer: Manufacturer,
    val rssi: Int,
    val detectedAt: Long = currentTimeMillis()
) {
    val distance: Distance
        get() = when {
            rssi >= -50 -> Distance.VERY_CLOSE
            rssi >= -60 -> Distance.CLOSE
            rssi >= -70 -> Distance.MODERATE
            else -> Distance.FAR
        }
}

enum class Distance(val label: String) {
    VERY_CLOSE("とても近い"),
    CLOSE("近い"),
    MODERATE("少し離れている"),
    FAR("離れている");

    companion object {
        fun fromStored(value: String): Distance {
            entries.find { distance ->
                distance.name.equals(value, ignoreCase = true)
            }?.let { return it }

            return when {
                value.contains("とても近") -> VERY_CLOSE
                value.contains("少し") -> MODERATE
                value.contains("近") -> CLOSE
                else -> FAR
            }
        }
    }
}
