package jp.smartglasses.detector.domain.model

data class SmartGlassesDevice(
    val name: String,
    val address: String,
    val manufacturer: Manufacturer,
    val rssi: Int,
    val detectedAt: Long = System.currentTimeMillis()
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
    FAR("離れている")
}
