package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionNotificationPolicyTest {
    @Test
    fun `blank identity reuses the shared detection notification id`() {
        assertEquals(
            Constants.NOTIFICATION_ID_DETECTION,
            DetectionNotificationPolicy.notificationId(address = "", name = "  ")
        )
    }

    @Test
    fun `same device keeps a stable notification id`() {
        val first = DetectionNotificationPolicy.notificationId(
            address = "aa:bb:cc:dd:ee:ff",
            name = "Ray-Ban Meta"
        )
        val second = DetectionNotificationPolicy.notificationId(
            address = "AA:BB:CC:DD:EE:FF",
            name = "Ray-Ban Meta"
        )
        assertEquals(first, second)
        assertNotEquals(Constants.NOTIFICATION_ID_SCANNING, first)
        assertNotEquals(Constants.NOTIFICATION_ID_DETECTION, first)
    }

    @Test
    fun `two nearby glasses do not share one notification`() {
        val first = DetectionNotificationPolicy.notificationId(
            address = "AA:BB:CC:DD:EE:01",
            name = "Ray-Ban Meta"
        )
        val second = DetectionNotificationPolicy.notificationId(
            address = "AA:BB:CC:DD:EE:02",
            name = "Even G2_12_L"
        )
        assertNotEquals(first, second)
        assertTrue(DetectionNotificationPolicy.GROUP_KEY.isNotBlank())
    }
}
