package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.ScanSensitivity

object ForegroundScanBoostPolicy {
    fun effectiveSensitivity(
        userSensitivity: ScanSensitivity,
        appInForeground: Boolean
    ): ScanSensitivity {
        if (appInForeground) {
            return ScanSensitivity.HIGH_ACCURACY
        }
        return userSensitivity
    }
}
