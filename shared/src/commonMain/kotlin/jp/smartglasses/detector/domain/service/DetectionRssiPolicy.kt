package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants
import jp.smartglasses.detector.util.ScanSensitivity

enum class DetectionMatchClass {
    CATALOG,
    WEAK
}

object DetectionRssiPolicy {
    fun isEligible(
        rssi: Int,
        sensitivity: ScanSensitivity,
        matchClass: DetectionMatchClass
    ): Boolean {
        if (rssi == Constants.UNKNOWN_RSSI_DBM) {
            return true
        }

        return rssi >= minimumRssiDbm(sensitivity, matchClass)
    }

    fun minimumRssiDbm(
        sensitivity: ScanSensitivity,
        matchClass: DetectionMatchClass
    ): Int {
        return when (matchClass) {
            DetectionMatchClass.CATALOG -> when (sensitivity) {
                ScanSensitivity.LOW_POWER -> CATALOG_LOW_POWER_RSSI_DBM
                ScanSensitivity.BALANCED -> CATALOG_BALANCED_RSSI_DBM
                ScanSensitivity.HIGH_ACCURACY -> CATALOG_HIGH_ACCURACY_RSSI_DBM
            }
            DetectionMatchClass.WEAK -> when (sensitivity) {
                ScanSensitivity.LOW_POWER -> WEAK_LOW_POWER_RSSI_DBM
                ScanSensitivity.BALANCED -> WEAK_BALANCED_RSSI_DBM
                ScanSensitivity.HIGH_ACCURACY -> WEAK_HIGH_ACCURACY_RSSI_DBM
            }
        }
    }

    const val CATALOG_LOW_POWER_RSSI_DBM = -80
    const val CATALOG_BALANCED_RSSI_DBM = -100
    const val CATALOG_HIGH_ACCURACY_RSSI_DBM = -110
    const val WEAK_LOW_POWER_RSSI_DBM = -65
    const val WEAK_BALANCED_RSSI_DBM = -75
    const val WEAK_HIGH_ACCURACY_RSSI_DBM = -85
}
