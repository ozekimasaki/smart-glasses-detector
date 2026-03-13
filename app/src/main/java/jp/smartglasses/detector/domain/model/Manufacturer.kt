package jp.smartglasses.detector.domain.model

data class Manufacturer(
    val id: Int?,
    val name: String,
    val detectionMethod: DetectionMethod
)

enum class DetectionMethod {
    COMPANY_ID,
    DEVICE_NAME
}
