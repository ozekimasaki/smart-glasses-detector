package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.DetectionMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmartGlassesClassifierTest {
    private val classifier = SmartGlassesClassifier()

    @Test
    fun `apple company id alone is ignored`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "iPhone",
                address = "AA:BB:CC:DD:EE:01",
                companyIds = setOf(0x004C),
                rssi = -60
            )
        )

        assertNull(detected)
    }

    @Test
    fun `google company ids alone are ignored`() {
        val googleDetected = classifier.classify(
            DetectionSignal(
                deviceName = "Pixel Device",
                address = "AA:BB:CC:DD:EE:02",
                companyIds = setOf(0x00E0),
                rssi = -60
            )
        )
        val googleLlcDetected = classifier.classify(
            DetectionSignal(
                deviceName = "Pixel Device",
                address = "AA:BB:CC:DD:EE:03",
                companyIds = setOf(0x018E),
                rssi = -60
            )
        )

        assertNull(googleDetected)
        assertNull(googleLlcDetected)
    }

    @Test
    fun `meta company id remains detectable`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Ray-Ban Meta",
                address = "AA:BB:CC:DD:EE:04",
                companyIds = setOf(0x01AB),
                rssi = -60
            )
        )

        assertNotNull(detected)
        assertEquals("Meta Platforms", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.COMPANY_ID, detected?.manufacturer?.detectionMethod)
        assertEquals("Ray-Ban Meta", detected?.name)
    }

    @Test
    fun `name pattern detection remains enabled`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "HUAWEI Eyewear 2-1234",
                address = "AA:BB:CC:DD:EE:05",
                companyIds = emptySet(),
                rssi = -60
            )
        )

        assertNotNull(detected)
        assertEquals("Huawei", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.DEVICE_NAME, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `huawei model code names are detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "HWF2003N-3A",
                address = "AA:BB:CC:DD:EE:07",
                companyIds = emptySet(),
                rssi = -60
            )
        )

        assertNotNull(detected)
        assertEquals("Huawei", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.DEVICE_NAME, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `owndays co branded names are detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "OWNDAYS x HUAWEI Eyewear",
                address = "AA:BB:CC:DD:EE:08",
                companyIds = emptySet(),
                rssi = -60
            )
        )

        assertNotNull(detected)
        assertEquals("Huawei", detected?.manufacturer?.name)
    }

    @Test
    fun `signals below rssi threshold are ignored`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Ray-Ban Meta",
                address = "AA:BB:CC:DD:EE:06",
                companyIds = setOf(0x01AB),
                rssi = -80
            )
        )

        assertNull(detected)
    }
}
