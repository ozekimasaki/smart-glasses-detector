package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants

object DetectionNotificationPolicy {
    const val GROUP_KEY = "smart_glasses_detections"

    fun notificationId(
        address: String,
        name: String,
        scanningNotificationId: Int = Constants.NOTIFICATION_ID_SCANNING,
        detectionBaseId: Int = Constants.NOTIFICATION_ID_DETECTION
    ): Int {
        val key = address.trim().uppercase().ifBlank { name.trim() }
        if (key.isBlank()) {
            return detectionBaseId
        }
        val hashed = detectionBaseId + 1 + (key.hashCode() and 0x7FFF)
        return if (hashed == scanningNotificationId) {
            detectionBaseId
        } else {
            hashed
        }
    }
}
