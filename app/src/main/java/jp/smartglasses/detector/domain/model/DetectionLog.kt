package jp.smartglasses.detector.domain.model

data class DetectionLog(
    val id: Long = 0,
    val deviceName: String,
    val deviceAddress: String,
    val manufacturerName: String,
    val rssi: Int,
    val distance: String,
    val detectedAt: Long
)
