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

    @Test
    fun `parses 16-bit service data uuid`() {
        val parsed = AdvertisementParser.parse(
            byteArrayOf(0x05, 0x16, 0x45.toByte(), 0xFE.toByte(), 0x00, 0x00)
        )

        assertEquals(
            listOf("0000FE45-0000-1000-8000-00805F9B34FB"),
            parsed.serviceUuids
        )
    }

    @Test
    fun `parses 16-bit service uuids from advertisement`() {
        val parsed = AdvertisementParser.parse(
            byteArrayOf(0x03, 0x03, 0x5F, 0xFD.toByte())
        )

        assertEquals(
            listOf("0000FD5F-0000-1000-8000-00805F9B34FB"),
            parsed.serviceUuids
        )
    }

    @Test
    fun `parses manufacturer company id from advertisement`() {
        val parsed = AdvertisementParser.parse(
            byteArrayOf(0x05, 0xFF.toByte(), 0xAB.toByte(), 0x01, 0x00, 0x00)
        )

        assertEquals(setOf(0x01AB), parsed.companyIds)
    }

    @Test
    fun `parses little endian 128-bit service uuid`() {
        val parsed = AdvertisementParser.parse(
            byteArrayOf(
                0x11, 0x07,
                0xD0.toByte(), 0x00, 0x2D, 0x12, 0x1E, 0x4B, 0x0F, 0xA4.toByte(),
                0x99.toByte(), 0x4E, 0xCE.toByte(), 0xB5.toByte(),
                0xF0.toByte(), 0xFF.toByte(), 0x05, 0x79
            )
        )

        assertEquals(
            listOf("7905FFF0-B5CE-4E99-A40F-4B1E122D00D0"),
            parsed.serviceUuids
        )
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
    fun `normalizes 32 bit assigned numbers`() {
        assertEquals(
            "12345678-0000-1000-8000-00805F9B34FB",
            BleUuid.normalize("0x12345678")
        )
    }

    @Test
    fun `merges and normalizes service uuid sources`() {
        assertEquals(
            listOf(
                "0000FE45-0000-1000-8000-00805F9B34FB",
                "0000FD5F-0000-1000-8000-00805F9B34FB"
            ),
            BleUuid.merge(
                listOf("FE45"),
                listOf("0000fe45-0000-1000-8000-00805f9b34fb"),
                listOf("0xFD5F"),
                listOf(" ")
            )
        )
    }
}
