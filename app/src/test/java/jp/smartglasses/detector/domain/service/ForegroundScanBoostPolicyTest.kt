package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.ScanSensitivity
import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundScanBoostPolicyTest {
    @Test
    fun `uses high accuracy while the app is visible`() {
        assertEquals(
            ScanSensitivity.HIGH_ACCURACY,
            ForegroundScanBoostPolicy.effectiveSensitivity(
                userSensitivity = ScanSensitivity.LOW_POWER,
                appInForeground = true
            )
        )
        assertEquals(
            ScanSensitivity.HIGH_ACCURACY,
            ForegroundScanBoostPolicy.effectiveSensitivity(
                userSensitivity = ScanSensitivity.BALANCED,
                appInForeground = true
            )
        )
    }

    @Test
    fun `keeps the user sensitivity while the app is in the background`() {
        assertEquals(
            ScanSensitivity.LOW_POWER,
            ForegroundScanBoostPolicy.effectiveSensitivity(
                userSensitivity = ScanSensitivity.LOW_POWER,
                appInForeground = false
            )
        )
        assertEquals(
            ScanSensitivity.BALANCED,
            ForegroundScanBoostPolicy.effectiveSensitivity(
                userSensitivity = ScanSensitivity.BALANCED,
                appInForeground = false
            )
        )
        assertEquals(
            ScanSensitivity.HIGH_ACCURACY,
            ForegroundScanBoostPolicy.effectiveSensitivity(
                userSensitivity = ScanSensitivity.HIGH_ACCURACY,
                appInForeground = false
            )
        )
    }
}
