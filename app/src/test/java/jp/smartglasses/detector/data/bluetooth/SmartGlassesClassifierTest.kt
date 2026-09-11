package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.DetectionMethod
import jp.smartglasses.detector.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `apple vision pro is detected by name`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Apple Vision Pro",
                address = "AA:BB:CC:DD:EE:09",
                companyIds = setOf(0x004C),
                rssi = -55
            )
        )

        assertNotNull(detected)
        assertEquals("Apple", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.DEVICE_NAME, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `google glass is detected by name`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Glass EE2",
                address = "AA:BB:CC:DD:EE:10",
                companyIds = setOf(0x00E0),
                rssi = -62
            )
        )

        assertNotNull(detected)
        assertEquals("Google", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.DEVICE_NAME, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `vuzix company id is detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:11",
                companyIds = setOf(0x060C),
                rssi = -58
            )
        )

        assertNotNull(detected)
        assertEquals("Vuzix", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.COMPANY_ID, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `even realities is detected by name`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Even G1-2048",
                address = "AA:BB:CC:DD:EE:12",
                companyIds = emptySet(),
                rssi = -61
            )
        )

        assertNotNull(detected)
        assertEquals("Even Realities", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.DEVICE_NAME, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `meta service uuid is detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:13",
                companyIds = emptySet(),
                rssi = -59,
                serviceUuids = listOf("0000fd5f-0000-1000-8000-00805f9b34fb")
            )
        )

        assertNotNull(detected)
        assertEquals("Meta Platforms", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.SERVICE_UUID, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `heycyan service uuid is detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "HeyCyan-A1B2",
                address = "AA:BB:CC:DD:EE:14",
                companyIds = emptySet(),
                rssi = -63,
                serviceUuids = listOf("7905FFF0-B5CE-4E99-A40F-4B1E122D00D0")
            )
        )

        assertNotNull(detected)
        assertEquals("HeyCyan", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.SERVICE_UUID, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `meta ray-ban payload is detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:15",
                companyIds = setOf(0x058E),
                rssi = -57,
                advertisementDataHex = "0201060EFF8E05544553544D4554415F52425F474C415353"
            )
        )

        assertNotNull(detected)
        assertEquals(DetectionMethod.COMPANY_ID, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `meta payload is detected without relying on company id only path`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:16",
                companyIds = emptySet(),
                rssi = -57,
                advertisementDataHex = asciiToHex("META_RB_GLASS")
            )
        )

        assertNotNull(detected)
        assertEquals("Meta Platforms", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.PAYLOAD, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `gap eyeglasses appearance is detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:17",
                companyIds = emptySet(),
                rssi = -60,
                appearance = 0x01C0
            )
        )

        assertNotNull(detected)
        assertEquals(Constants.GENERIC_SMART_GLASSES_NAME, detected?.manufacturer?.name)
        assertEquals(DetectionMethod.APPEARANCE, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `generic glasses name is detected by heuristic`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "AI Glasses-9C",
                address = "AA:BB:CC:DD:EE:18",
                companyIds = emptySet(),
                rssi = -64
            )
        )

        assertNotNull(detected)
        assertEquals(Constants.GENERIC_SMART_GLASSES_NAME, detected?.manufacturer?.name)
        assertEquals(DetectionMethod.HEURISTIC, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `headphones are not treated as smart glasses`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "WH-1000XM5",
                address = "AA:BB:CC:DD:EE:19",
                companyIds = setOf(0x012D),
                rssi = -48
            )
        )

        assertNull(detected)
    }

    @Test
    fun `unknown classic rssi still allows name detection`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "XREAL One Pro",
                address = "AA:BB:CC:DD:EE:20",
                companyIds = emptySet(),
                rssi = Constants.UNKNOWN_RSSI_DBM
            )
        )

        assertNotNull(detected)
        assertEquals("XREAL", detected?.manufacturer?.name)
    }

    @Test
    fun `every manufacturer rule can be detected by at least one configured signal`() {
        Constants.SMART_GLASSES_DETECTION_RULES.forEach { rule ->
            val detected = when {
                rule.allowCompanyIdOnly && rule.companyIds.isNotEmpty() -> classifier.classify(
                    DetectionSignal(
                        deviceName = null,
                        address = "AA:BB:CC:DD:EE:21",
                        companyIds = setOf(rule.companyIds.first()),
                        rssi = -50
                    )
                )
                rule.serviceUuids.isNotEmpty() -> classifier.classify(
                    DetectionSignal(
                        deviceName = null,
                        address = "AA:BB:CC:DD:EE:21",
                        companyIds = emptySet(),
                        rssi = -50,
                        serviceUuids = listOf(rule.serviceUuids.first())
                    )
                )
                rule.payloadPatterns.isNotEmpty() -> classifier.classify(
                    DetectionSignal(
                        deviceName = null,
                        address = "AA:BB:CC:DD:EE:21",
                        companyIds = emptySet(),
                        rssi = -50,
                        advertisementDataHex = asciiToHex(rule.payloadPatterns.first())
                    )
                )
                rule.namePatterns.isNotEmpty() -> classifier.classify(
                    DetectionSignal(
                        deviceName = rule.namePatterns.first(),
                        address = "AA:BB:CC:DD:EE:21",
                        companyIds = emptySet(),
                        rssi = -50
                    )
                )
                else -> null
            }

            assertNotNull("${rule.manufacturerName} should be detectable", detected)
            assertEquals(rule.manufacturerName, detected?.manufacturer?.name)
        }
    }

    @Test
    fun `detection rules cover a broad manufacturer catalog`() {
        val manufacturerNames = Constants.SMART_GLASSES_DETECTION_RULES
            .map { rule -> rule.manufacturerName }
            .toSet()

        assertTrue(manufacturerNames.size >= 40)
        listOf(
            "Vuzix",
            "Even Realities",
            "Brilliant Labs",
            "HeyCyan",
            "XREAL",
            "Rokid",
            "VITURE",
            "Halliday"
        ).forEach { name ->
            assertTrue("$name should be in the catalog", name in manufacturerNames)
        }
    }

    private fun asciiToHex(value: String): String {
        return value.encodeToByteArray().joinToString("") { byte ->
            (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
        }
    }
}
