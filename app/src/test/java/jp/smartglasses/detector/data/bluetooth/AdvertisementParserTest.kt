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
    fun `parses bluetooth 5 broadcast name as complete name`() {
        val name = "Loomos"
        val nameBytes = name.encodeToByteArray()
        val record = byteArrayOf((nameBytes.size + 1).toByte(), 0x30) + nameBytes

        val parsed = AdvertisementParser.parse(record)

        assertEquals("Loomos", parsed.completeName)
    }

    @Test
    fun `complete local name wins over broadcast name`() {
        val complete = "Even G2_12_L".encodeToByteArray()
        val broadcast = "Loomos".encodeToByteArray()
        val record = byteArrayOf((complete.size + 1).toByte(), 0x09) + complete +
            byteArrayOf((broadcast.size + 1).toByte(), 0x30) + broadcast

        val parsed = AdvertisementParser.parse(record)

        assertEquals("Even G2_12_L", parsed.completeName)
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
    fun `manufacturer tlv encoding copies the payload bytes`() {
        val payload = byteArrayOf(0x11, 0x22, 0x33)
        val encoded = AdvertisementParser.encodeManufacturerSpecificTlvBytes(0x01AB, payload)
        payload[0] = 0x00

        assertEquals(0x11.toByte(), encoded[4])
        assertEquals(0x22.toByte(), encoded[5])
        assertEquals(0x33.toByte(), encoded[6])
    }

    @Test
    fun `copied advertising map bytes survive later mutation`() {
        val original = byteArrayOf(0xC0.toByte(), 0x01)
        val snapshot = mapOf(AdvertisementParser.AD_TYPE_APPEARANCE to original.copyOf())
        original[0] = 0x40

        val parsed = AdvertisementParser.parseAdvertisingDataMap(snapshot)

        assertEquals(0x01C0, parsed.appearance)
        assertTrue(AdvertisementParser.isEyeglassesAppearance(parsed.appearance))
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

    @Test
    fun `parses advertising data map entries`() {
        val parsed = AdvertisementParser.parseAdvertisingDataMap(
            mapOf(
                AdvertisementParser.AD_TYPE_APPEARANCE to byteArrayOf(0xC0.toByte(), 0x01),
                AdvertisementParser.AD_TYPE_COMPLETE_16BIT_UUIDS to byteArrayOf(
                    0x45.toByte(),
                    0xFE.toByte()
                )
            )
        )

        assertEquals(0x01C0, parsed.appearance)
        assertEquals(
            listOf("0000FE45-0000-1000-8000-00805F9B34FB"),
            parsed.serviceUuids
        )
    }

    @Test
    fun `detects activelook manufacturer data suffix`() {
        assertTrue(AdvertisementParser.hasManufacturerDataSuffix("05FFFADA08F2", 0x08F2))
        assertFalse(AdvertisementParser.hasManufacturerDataSuffix("051645FE08F2", 0x08F2))
        assertFalse(AdvertisementParser.hasManufacturerDataSuffix("05FFFADA0000", 0x08F2))
    }

    @Test
    fun `reconstructs manufacturer specific tlv from scan record fields`() {
        val hex = AdvertisementParser.encodeManufacturerSpecificTlv(
            companyId = 0x5241,
            payload = byteArrayOf(0x39, 0x39)
        )

        assertEquals("05FF41523939", hex)
        assertEquals(setOf(0x5241), AdvertisementParser.parseHex(hex).companyIds)
        assertTrue(AdvertisementParser.asciiFromHex(hex).contains("AR99"))
    }

    @Test
    fun `encodeHex round trips little endian advertisements`() {
        val bytes = byteArrayOf(0x05, 0xFF.toByte(), 0xAB.toByte(), 0x01, 0x00, 0x00)

        assertEquals("05FFAB010000", AdvertisementParser.encodeHex(bytes))
        assertTrue(bytes.contentEquals(AdvertisementParser.hexToBytes("05FFAB010000")!!))
        assertEquals(setOf(0x01AB), AdvertisementParser.parse(bytes).companyIds)
    }

    @Test
    fun `detects activelook manufacturer data suffix from bytes`() {
        val bytes = AdvertisementParser.hexToBytes("05FFFADA08F2")

        assertTrue(AdvertisementParser.hasManufacturerDataSuffix(bytes, 0x08F2))
        assertFalse(AdvertisementParser.hasManufacturerDataSuffix(byteArrayOf(0x05, 0x16, 0x45, 0xFE.toByte(), 0x08, 0xF2.toByte()), 0x08F2))
    }

    @Test
    fun `merges advertisement fields from truncated packets`() {
        val merged = AdvertisementParser.parseHex("030345FE").merge(
            AdvertisementParser.parseAdvertisingDataMap(
                mapOf(
                    AdvertisementParser.AD_TYPE_COMPLETE_NAME to "A.Look 000128".encodeToByteArray()
                )
            )
        )

        assertEquals(
            listOf("0000FE45-0000-1000-8000-00805F9B34FB"),
            merged.serviceUuids
        )
        assertEquals("A.Look 000128", merged.completeName)
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
