package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants
import jp.smartglasses.detector.util.ScanSensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionRssiPolicyTest {
    @Test
    fun `unknown rssi is always eligible`() {
        assertTrue(
            DetectionRssiPolicy.isEligible(
                rssi = Constants.UNKNOWN_RSSI_DBM,
                sensitivity = ScanSensitivity.LOW_POWER,
                matchClass = DetectionMatchClass.WEAK
            )
        )
    }

    @Test
    fun `balanced catalog accepts distant known glasses that old floor rejected`() {
        assertTrue(
            DetectionRssiPolicy.isEligible(
                rssi = -90,
                sensitivity = ScanSensitivity.BALANCED,
                matchClass = DetectionMatchClass.CATALOG
            )
        )
        assertFalse(
            DetectionRssiPolicy.isEligible(
                rssi = -90,
                sensitivity = ScanSensitivity.BALANCED,
                matchClass = DetectionMatchClass.WEAK
            )
        )
    }

    @Test
    fun `low power keeps catalog detections nearby`() {
        assertFalse(
            DetectionRssiPolicy.isEligible(
                rssi = -90,
                sensitivity = ScanSensitivity.LOW_POWER,
                matchClass = DetectionMatchClass.CATALOG
            )
        )
        assertTrue(
            DetectionRssiPolicy.isEligible(
                rssi = -80,
                sensitivity = ScanSensitivity.LOW_POWER,
                matchClass = DetectionMatchClass.CATALOG
            )
        )
    }

    @Test
    fun `high accuracy widens both catalog and heuristic floors`() {
        assertTrue(
            DetectionRssiPolicy.isEligible(
                rssi = -110,
                sensitivity = ScanSensitivity.HIGH_ACCURACY,
                matchClass = DetectionMatchClass.CATALOG
            )
        )
        assertTrue(
            DetectionRssiPolicy.isEligible(
                rssi = -85,
                sensitivity = ScanSensitivity.HIGH_ACCURACY,
                matchClass = DetectionMatchClass.WEAK
            )
        )
        assertFalse(
            DetectionRssiPolicy.isEligible(
                rssi = -90,
                sensitivity = ScanSensitivity.HIGH_ACCURACY,
                matchClass = DetectionMatchClass.WEAK
            )
        )
    }

    @Test
    fun `threshold table matches the documented scan sensitivity copy`() {
        assertEquals(
            -80,
            DetectionRssiPolicy.minimumRssiDbm(
                ScanSensitivity.LOW_POWER,
                DetectionMatchClass.CATALOG
            )
        )
        assertEquals(
            -100,
            DetectionRssiPolicy.minimumRssiDbm(
                ScanSensitivity.BALANCED,
                DetectionMatchClass.CATALOG
            )
        )
        assertEquals(
            -110,
            DetectionRssiPolicy.minimumRssiDbm(
                ScanSensitivity.HIGH_ACCURACY,
                DetectionMatchClass.CATALOG
            )
        )
    }
}
