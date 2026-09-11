package jp.smartglasses.detector.data.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvertisementParserTest {
    @Test
    fun `parses little endian eyeglasses appearance`() {
        val parsed = AdvertisementParser.parse(
            byteArrayOf(
                0x02, 0x01, 0x06,
                0x03, 0x19, 0xC0.toByte(), 0x01
            )
        )

        assertEquals(0x01C0, parsed.appearance)
        assertTrue(AdvertisementParser.isEyeglassesAppearance(parsed.appearance))
    }

    @Test
    fun `parses complete local name`() {
        val name = "XREAL One"
        val nameBytes = name.encodeToByteArray()
        val record = byteArrayOf((nameBytes.size + 1).toByte(), 0x09) + nameBytes

        val parsed = AdvertisementParser.parse(record)

        assertEquals("XREAL One", parsed.completeName)
    }

    @Test
    fun `phone appearance is not eyeglasses`() {
        val parsed = AdvertisementParser.parse(
            byteArrayOf(0x03, 0x19, 0x40, 0x00)
        )

        assertEquals(0x0040, parsed.appearance)
        assertFalse(AdvertisementParser.isEyeglassesAppearance(parsed.appearance))
    }

    @Test
    fun `ascii payload extracts printable manufacturer strings`() {
        val ascii = AdvertisementParser.asciiFromHex(
            "020106" + "META_RB_GLASS".encodeToByteArray().joinToString("") { byte ->
                (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
            }
        )

        assertTrue(ascii.contains("META_RB_GLASS"))
    }

    @Test
    fun `invalid hex returns null bytes`() {
        assertNull(AdvertisementParser.hexToBytes("ABC"))
        assertEquals(ParsedAdvertisement(), AdvertisementParser.parseHex("GG"))
    }
}

class BleUuidTest {
    @Test
    fun `normalizes 16 bit assigned numbers to bluetooth base uuid`() {
        assertEquals(
            "0000FD5F-0000-1000-8000-00805F9B34FB",
            BleUuid.normalize("0xfd5f")
        )
        assertTrue(
            BleUuid.matches(
                "0000fd5f-0000-1000-8000-00805f9b34fb",
                "FD5F"
            )
        )
    }

    @Test
    fun `normalizes 128 bit uuids`() {
        assertEquals(
            "7905FFF0-B5CE-4E99-A40F-4B1E122D00D0",
            BleUuid.normalize("7905fff0b5ce4e99a40f4b1e122d00d0")
        )
    }
}
