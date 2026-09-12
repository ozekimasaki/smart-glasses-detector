package jp.smartglasses.detector.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothDeviceClassPolicyTest {
    @Test
    fun `wearable glasses class is treated as glasses`() {
        assertTrue(
            BluetoothDeviceClassPolicy.isGlassesDeviceClass(
                BluetoothDeviceClassPolicy.WEARABLE_GLASSES
            )
        )
    }

    @Test
    fun `audio video glasses class added in assigned numbers is treated as glasses`() {
        assertTrue(
            BluetoothDeviceClassPolicy.isGlassesDeviceClass(
                BluetoothDeviceClassPolicy.AUDIO_VIDEO_GLASSES
            )
        )
    }

    @Test
    fun `service class bits do not hide wearable glasses`() {
        assertTrue(
            BluetoothDeviceClassPolicy.isGlassesDeviceClass(0x200714)
        )
    }

    @Test
    fun `headphones and watches are not glasses`() {
        assertFalse(BluetoothDeviceClassPolicy.isGlassesDeviceClass(0x0418))
        assertFalse(BluetoothDeviceClassPolicy.isGlassesDeviceClass(0x0404))
        assertFalse(BluetoothDeviceClassPolicy.isGlassesDeviceClass(0x0704))
        assertFalse(BluetoothDeviceClassPolicy.isGlassesDeviceClass(0x044C))
        assertFalse(BluetoothDeviceClassPolicy.isGlassesDeviceClass(null))
    }

    @Test
    fun `advertised glasses class wins over a generic bluetooth class`() {
        assertEquals(
            BluetoothDeviceClassPolicy.WEARABLE_GLASSES,
            BluetoothDeviceClassPolicy.preferGlassesDeviceClass(
                primary = 0x0418,
                fallback = BluetoothDeviceClassPolicy.WEARABLE_GLASSES
            )
        )
        assertEquals(
            BluetoothDeviceClassPolicy.AUDIO_VIDEO_GLASSES,
            BluetoothDeviceClassPolicy.preferGlassesDeviceClass(
                primary = BluetoothDeviceClassPolicy.AUDIO_VIDEO_GLASSES,
                fallback = 0x0418
            )
        )
        assertEquals(
            0x0418,
            BluetoothDeviceClassPolicy.preferGlassesDeviceClass(
                primary = 0x0418,
                fallback = null
            )
        )
        assertEquals(
            BluetoothDeviceClassPolicy.WEARABLE_GLASSES,
            BluetoothDeviceClassPolicy.preferGlassesDeviceClass(
                primary = null,
                fallback = BluetoothDeviceClassPolicy.WEARABLE_GLASSES
            )
        )
    }
}
