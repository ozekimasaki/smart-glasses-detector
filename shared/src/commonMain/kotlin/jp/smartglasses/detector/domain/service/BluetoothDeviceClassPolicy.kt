package jp.smartglasses.detector.domain.service

/**
 * Bluetooth Class of Device のうち、機器自身が Glasses と宣言している値。
 *
 * - Wearable / Glasses: Android `BluetoothClass.Device.WEARABLE_GLASSES` (0x0714)
 * - Audio/Video / Glasses: Bluetooth Assigned Numbers 2025-09-17 の A/V minor Glasses (0x0450)
 */
object BluetoothDeviceClassPolicy {
    const val DEVICE_CLASS_MASK = 0x1FFC
    const val WEARABLE_GLASSES = 0x0714
    const val AUDIO_VIDEO_GLASSES = 0x0450

    fun isGlassesDeviceClass(deviceClass: Int?): Boolean {
        val value = deviceClass ?: return false
        val masked = value and DEVICE_CLASS_MASK
        return masked == WEARABLE_GLASSES || masked == AUDIO_VIDEO_GLASSES
    }

    fun preferGlassesDeviceClass(primary: Int?, fallback: Int?): Int? {
        if (isGlassesDeviceClass(primary)) {
            return primary
        }
        if (isGlassesDeviceClass(fallback)) {
            return fallback
        }
        return primary ?: fallback
    }
}
