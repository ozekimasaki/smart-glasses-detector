package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.DetectionMethod
import jp.smartglasses.detector.util.Constants
import jp.smartglasses.detector.util.ScanSensitivity
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
    fun `distant catalog matches remain detectable at balanced sensitivity`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Ray-Ban Meta",
                address = "AA:BB:CC:DD:EE:06",
                companyIds = setOf(0x01AB),
                rssi = -90
            )
        )

        assertNotNull(detected)
        assertEquals("Meta Platforms", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.COMPANY_ID, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `weak heuristic names stay ignored at the old balanced floor`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Pocket HUD-01",
                address = "AA:BB:CC:DD:EE:06B",
                companyIds = emptySet(),
                rssi = -80
            )
        )

        assertNull(detected)
    }

    @Test
    fun `low power sensitivity ignores distant catalog matches`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Ray-Ban Meta",
                address = "AA:BB:CC:DD:EE:06C",
                companyIds = setOf(0x01AB),
                rssi = -90
            ),
            sensitivity = ScanSensitivity.LOW_POWER
        )

        assertNull(detected)
    }

    @Test
    fun `high accuracy sensitivity detects farther catalog matches`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:06D",
                companyIds = emptySet(),
                rssi = -108,
                serviceUuids = listOf("00009100-0000-1000-8000-00805F9B34FB")
            ),
            sensitivity = ScanSensitivity.HIGH_ACCURACY
        )

        assertNotNull(detected)
        assertEquals("Rokid", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.SERVICE_UUID, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `even g1 hyphenated names are detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "G1-2048",
                address = "AA:BB:CC:DD:EE:06F",
                companyIds = emptySet(),
                rssi = -62
            )
        )

        assertNotNull(detected)
        assertEquals("Even Realities", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.DEVICE_NAME, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `rokid unnamed advertisement is detected by service uuid`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:06E",
                companyIds = emptySet(),
                rssi = -88,
                serviceUuids = listOf("00009100-0000-1000-8000-00805F9B34FB")
            )
        )

        assertNotNull(detected)
        assertEquals("Rokid", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.SERVICE_UUID, detected?.manufacturer?.detectionMethod)
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
    fun `meta service uuid in raw advertisement is detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:22",
                companyIds = emptySet(),
                rssi = -58,
                advertisementDataHex = "03035FFD"
            )
        )

        assertNotNull(detected)
        assertEquals("Meta Platforms", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.SERVICE_UUID, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `company id encoded in advertisement bytes is detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:23",
                companyIds = emptySet(),
                rssi = -52,
                advertisementDataHex = "05FFAB010000"
            )
        )

        assertNotNull(detected)
        assertEquals("Meta Platforms", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.COMPANY_ID, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `even realities g1 coded names are detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "G1_8A21",
                address = "AA:BB:CC:DD:EE:24",
                companyIds = emptySet(),
                rssi = -60
            )
        )

        assertNotNull(detected)
        assertEquals("Even Realities", detected?.manufacturer?.name)
    }

    @Test
    fun `japanese generic glasses names are detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "ARグラス-01",
                address = "AA:BB:CC:DD:EE:25",
                companyIds = emptySet(),
                rssi = -61
            )
        )

        assertNotNull(detected)
        assertEquals(DetectionMethod.HEURISTIC, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `even realities company id is detected without a device name`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:26",
                companyIds = setOf(0x10F9),
                rssi = -58
            )
        )

        assertNotNull(detected)
        assertEquals("Even Realities", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.COMPANY_ID, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `snap assigned uuid is detected from advertisement bytes`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:27",
                companyIds = emptySet(),
                rssi = -60,
                advertisementDataHex = "030345FE"
            )
        )

        assertNotNull(detected)
        assertEquals("Snapchat", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.SERVICE_UUID, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `snap service data uuid is detected from advertisement bytes`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:30",
                companyIds = emptySet(),
                rssi = -60,
                advertisementDataHex = "051645FE0000"
            )
        )

        assertNotNull(detected)
        assertEquals("Snapchat", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.SERVICE_UUID, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `activelook manufacturer suffix is detected without company id only`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:31",
                companyIds = emptySet(),
                rssi = -60,
                advertisementDataHex = "05FFFADA08F2"
            )
        )

        assertNotNull(detected)
        assertEquals("Engo", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.PAYLOAD, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `activelook complete name is detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "A.Look 000128",
                address = "AA:BB:CC:DD:EE:32",
                companyIds = emptySet(),
                rssi = -58
            )
        )

        assertNotNull(detected)
        assertEquals("Engo", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.DEVICE_NAME, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `mentra live legacy names are detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "XyBLE_A1B2",
                address = "AA:BB:CC:DD:EE:33",
                companyIds = emptySet(),
                rssi = -59
            )
        )

        assertNotNull(detected)
        assertEquals("Mentra", detected?.manufacturer?.name)
    }

    @Test
    fun `brilliant frame coded names are detected`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Frame-1A2B",
                address = "AA:BB:CC:DD:EE:34",
                companyIds = emptySet(),
                rssi = -61
            )
        )

        assertNotNull(detected)
        assertEquals("Brilliant Labs", detected?.manufacturer?.name)
    }

    @Test
    fun `headphone names are excluded from generic glasses heuristic`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Smart Glasses Headphones",
                address = "AA:BB:CC:DD:EE:28",
                companyIds = emptySet(),
                rssi = -55
            )
        )

        assertNull(detected)
    }

    @Test
    fun `unknown hud glasses name is detected by heuristic`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Pocket HUD-01",
                address = "AA:BB:CC:DD:EE:29",
                companyIds = emptySet(),
                rssi = -62
            )
        )

        assertNotNull(detected)
        assertEquals(DetectionMethod.HEURISTIC, detected?.manufacturer?.detectionMethod)
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
                rule.manufacturerDataSuffixes.isNotEmpty() -> classifier.classify(
                    DetectionSignal(
                        deviceName = null,
                        address = "AA:BB:CC:DD:EE:21",
                        companyIds = emptySet(),
                        rssi = -50,
                        advertisementDataHex = manufacturerSuffixHex(rule.manufacturerDataSuffixes.first())
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

        assertTrue(manufacturerNames.size >= 45)
        listOf(
            "Vuzix",
            "Even Realities",
            "Brilliant Labs",
            "HeyCyan",
            "XREAL",
            "Rokid",
            "VITURE",
            "Halliday",
            "Lucyd",
            "Tooz",
            "Engo",
            "Mentra"
        ).forEach { name ->
            assertTrue("$name should be in the catalog", name in manufacturerNames)
        }

        val rokid = Constants.SMART_GLASSES_DETECTION_RULES.first { rule ->
            rule.manufacturerName == "Rokid"
        }
        assertTrue(
            "Rokid Glasses official BLE UUID should be configured",
            rokid.serviceUuids.any { uuid ->
                uuid.equals("00009100-0000-1000-8000-00805F9B34FB", ignoreCase = true)
            }
        )

        val brilliant = Constants.SMART_GLASSES_DETECTION_RULES.first { rule ->
            rule.manufacturerName == "Brilliant Labs"
        }
        assertTrue(
            "Brilliant Labs Frame official BLE UUID should be configured",
            brilliant.serviceUuids.any { uuid ->
                uuid.equals("7A230001-5475-A6A4-654C-8431F6AD49C4", ignoreCase = true)
            }
        )
    }

    @Test
    fun `compact recent product names stay detectable`() {
        val inmo = classifier.classify(
            DetectionSignal(
                deviceName = "INMOAIR3_A1B2",
                address = "AA:BB:CC:DD:EE:30",
                companyIds = emptySet(),
                rssi = -60
            )
        )
        val galaxyXr = classifier.classify(
            DetectionSignal(
                deviceName = "Galaxy XR-01",
                address = "AA:BB:CC:DD:EE:31",
                companyIds = emptySet(),
                rssi = -60
            )
        )
        val evenG3 = classifier.classify(
            DetectionSignal(
                deviceName = "Even G3_12_L",
                address = "AA:BB:CC:DD:EE:32",
                companyIds = emptySet(),
                rssi = -60
            )
        )
        val halliday = classifier.classify(
            DetectionSignal(
                deviceName = "HALLIDAYGP101",
                address = "AA:BB:CC:DD:EE:35",
                companyIds = emptySet(),
                rssi = -60
            )
        )
        val frameName = classifier.classify(
            DetectionSignal(
                deviceName = "Frame 4F",
                address = "AA:BB:CC:DD:EE:36",
                companyIds = emptySet(),
                rssi = -60
            )
        )

        assertEquals("INMO", inmo?.manufacturer?.name)
        assertEquals("Samsung", galaxyXr?.manufacturer?.name)
        assertEquals("Even Realities", evenG3?.manufacturer?.name)
        assertEquals("Halliday", halliday?.manufacturer?.name)
        assertEquals("Brilliant Labs", frameName?.manufacturer?.name)
        assertEquals(DetectionMethod.DEVICE_NAME, frameName?.manufacturer?.detectionMethod)
    }

    @Test
    fun `brilliant frame service uuid is detected without a name`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = null,
                address = "AA:BB:CC:DD:EE:37",
                companyIds = emptySet(),
                rssi = -58,
                serviceUuids = listOf("7A230001-5475-A6A4-654C-8431F6AD49C4")
            )
        )

        assertNotNull(detected)
        assertEquals("Brilliant Labs", detected?.manufacturer?.name)
        assertEquals(DetectionMethod.SERVICE_UUID, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `unknown chinese glasses name is detected by heuristic`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "智能眼镜-A1",
                address = "AA:BB:CC:DD:EE:38",
                companyIds = emptySet(),
                rssi = -60
            )
        )

        assertNotNull(detected)
        assertEquals(DetectionMethod.HEURISTIC, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `camera glasses name is detected by heuristic`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Pocket Camera Glass",
                address = "AA:BB:CC:DD:EE:39",
                companyIds = emptySet(),
                rssi = -61
            )
        )

        assertNotNull(detected)
        assertEquals(DetectionMethod.HEURISTIC, detected?.manufacturer?.detectionMethod)
    }

    @Test
    fun `smart watch names are excluded from generic glasses heuristic`() {
        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Smart Watch Glasses",
                address = "AA:BB:CC:DD:EE:40",
                companyIds = emptySet(),
                rssi = -50
            )
        )

        assertNull(detected)
    }

    private fun asciiToHex(value: String): String {
        return value.encodeToByteArray().joinToString("") { byte ->
            (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
        }
    }

    private fun manufacturerSuffixHex(suffix: Int): String {
        val high = (suffix shr 8) and 0xFF
        val low = suffix and 0xFF
        return "05FF0000%02X%02X".format(high, low)
    }
}
