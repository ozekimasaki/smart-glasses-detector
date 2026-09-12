package jp.smartglasses.detector.data.bluetooth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvertisementCopyTest {
    @Test
    fun `copied advertisement bytes stay independent of the original buffer`() {
        val original = byteArrayOf(0x02, 0x01, 0x06, 0x05, 0xFF.toByte(), 0xAB.toByte(), 0x01, 0x47)
        val copied = AdvertisementCopy.copyBytes(original)

        assertArrayEquals(original, copied)
        original[original.lastIndex] = 0x00
        assertEquals(0x47.toByte(), copied.last())
        assertTrue(AdvertisementCopy.copyBytes(null).isEmpty())
    }

    @Test
    fun `copied advertising data map stays independent of the original entries`() {
        val originalPayload = byteArrayOf(0x4C, 0x00, 0x47, 0x4C, 0x41, 0x53, 0x53)
        val original = mapOf(AdvertisementParser.AD_TYPE_MANUFACTURER_SPECIFIC to originalPayload)
        val copied = AdvertisementCopy.copyMap(original)

        assertArrayEquals(originalPayload, copied.getValue(AdvertisementParser.AD_TYPE_MANUFACTURER_SPECIFIC))
        originalPayload[0] = 0x00
        assertEquals(0x4C.toByte(), copied.getValue(AdvertisementParser.AD_TYPE_MANUFACTURER_SPECIFIC)[0])
        assertTrue(AdvertisementCopy.copyMap(null).isEmpty())
    }

    @Test
    fun `extra payload copies manufacturer and service data after the source buffers change`() {
        val manufacturerPayload = byteArrayOf(0x47, 0x4C, 0x41, 0x53, 0x53)
        val serviceData = byteArrayOf(0x52, 0x6F, 0x6B, 0x69, 0x64)
        val extra = AdvertisementCopy.extraPayload(
            manufacturerEntries = listOf(0x01AB to manufacturerPayload),
            serviceDataValues = listOf(serviceData)
        )

        manufacturerPayload[0] = 0x00
        serviceData[0] = 0x00

        val ascii = AdvertisementParser.asciiFromBytes(extra)
        assertTrue(ascii.contains("GLASS"))
        assertTrue(ascii.contains("Rokid"))
        assertFalse(extra.isEmpty())
    }

    @Test
    fun `copied manufacturer entries stay independent of the original payloads`() {
        val originalPayload = byteArrayOf(0x47, 0x4C, 0x41, 0x53, 0x53)
        val copied = AdvertisementCopy.copyManufacturerEntries(
            listOf(0x01AB to originalPayload)
        )

        assertEquals(0x01AB, copied.single().first)
        assertArrayEquals(byteArrayOf(0x47, 0x4C, 0x41, 0x53, 0x53), copied.single().second)
        originalPayload[0] = 0x00
        assertEquals(0x47.toByte(), copied.single().second[0])
        assertTrue(AdvertisementCopy.copyManufacturerEntries(emptyList()).isEmpty())
    }
}
