package jp.smartglasses.detector.domain.model

data class DiagnosticLog(
    val id: Long = 0,
    val advertisedName: String,
    val deviceAddress: String,
    val companyIds: String,
    val serviceUuids: String,
    val advertisementDataHex: String,
    val rssi: Int,
    val detectedAt: Long
)
