package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.DetectionMethod
import jp.smartglasses.detector.domain.service.BluetoothAdvertisedNamePolicy
import jp.smartglasses.detector.domain.service.BluetoothDeviceClassPolicy
import jp.smartglasses.detector.domain.service.DetectionMatchClass
import jp.smartglasses.detector.domain.service.DetectionNotificationPolicy
import jp.smartglasses.detector.domain.service.DetectionRssiPolicy
import jp.smartglasses.detector.util.Constants
import jp.smartglasses.detector.util.ScanSensitivity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedDetectionTest {
    private val classifier = SmartGlassesClassifier()

    @Test
    fun parsesEyeglassesAppearanceAndGlassesClassOfDevice() {
        val appearance = AdvertisementParser.parse(
            byteArrayOf(0x02, 0x01, 0x06, 0x03, 0x19, 0xC0.toByte(), 0x01)
        )
        assertEquals(0x01C0, appearance.appearance)
        assertTrue(AdvertisementParser.isEyeglassesAppearance(appearance.appearance))

        val deviceClass = AdvertisementParser.parse(byteArrayOf(0x04, 0x0D, 0x14, 0x07, 0x00))
        assertEquals(0x0714, deviceClass.deviceClass)
        assertTrue(BluetoothDeviceClassPolicy.isGlassesDeviceClass(deviceClass.deviceClass))
    }

    @Test
    fun parsesCompleteNameAndMetaCompanyId() {
        val nameBytes = "XREAL One".encodeToByteArray()
        val nameRecord = byteArrayOf((nameBytes.size + 1).toByte(), 0x09) + nameBytes
        assertEquals("XREAL One", AdvertisementParser.parse(nameRecord).completeName)

        val parsed = AdvertisementParser.parse(
            byteArrayOf(0x05, 0xFF.toByte(), 0xAB.toByte(), 0x01, 0x00, 0x00)
        )
        assertEquals(setOf(0x01AB), parsed.companyIds)
        assertEquals("05FFAB010000", AdvertisementParser.encodeHex(byteArrayOf(0x05, 0xFF.toByte(), 0xAB.toByte(), 0x01, 0x00, 0x00)))
    }

    @Test
    fun appleCompanyIdAloneIsIgnoredAndMetaIsDetected() {
        assertNull(
            classifier.classify(
                DetectionSignal(
                    deviceName = "iPhone",
                    address = "AA:BB:CC:DD:EE:01",
                    companyIds = setOf(0x004C),
                    rssi = -60
                )
            )
        )

        val detected = classifier.classify(
            DetectionSignal(
                deviceName = "Ray-Ban Meta",
                address = "AA:BB:CC:DD:EE:04",
                companyIds = setOf(0x01AB),
                rssi = -60
            )
        )
        assertNotNull(detected)
        assertEquals("Meta Platforms", detected.manufacturer.name)
        assertEquals(DetectionMethod.COMPANY_ID, detected.manufacturer.detectionMethod)
    }

    @Test
    fun classifyAdvertisementAcceptsHexAndCsvFromIos() {
        val detected = SmartGlassesDetection.classifyAdvertisement(
            address = "E621E1F8-C36C-495A-93FC-0C247A3E6E5F",
            rssi = -58,
            deviceName = "Ray-Ban Meta",
            advertisementHex = "05FFAB010000",
            extraCompanyIdsCsv = "",
            extraServiceUuidsCsv = "",
            appearance = SmartGlassesDetection.UNSET_OPTIONAL_INT,
            deviceClass = SmartGlassesDetection.UNSET_OPTIONAL_INT,
            sensitivityName = "BALANCED"
        )
        assertNotNull(detected)
        assertEquals("Meta Platforms", detected.manufacturer.name)
        assertEquals("E621E1F8-C36C-495A-93FC-0C247A3E6E5F", detected.address)
    }

    @Test
    fun catalogServiceUuidsIncludeConfirmedGlassesServices() {
        val uuids = SmartGlassesDetection.catalogServiceUuids()
        assertTrue(uuids.contains("0000FD5F-0000-1000-8000-00805F9B34FB"))
        assertTrue(uuids.contains("0000FE45-0000-1000-8000-00805F9B34FB"))
        assertTrue(uuids.contains("7905FFF0-B5CE-4E99-A40F-4B1E122D00D0"))
        assertTrue(SmartGlassesDetection.catalogServiceUuidsCsv().contains("0000FE45"))
    }

    @Test
    fun rssiPolicyKeepsUnknownAndBalancedCatalogFloors() {
        assertTrue(
            DetectionRssiPolicy.isEligible(
                rssi = Constants.UNKNOWN_RSSI_DBM,
                sensitivity = ScanSensitivity.LOW_POWER,
                matchClass = DetectionMatchClass.WEAK
            )
        )
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
        assertEquals(
            -100,
            DetectionRssiPolicy.minimumRssiDbm(ScanSensitivity.BALANCED, DetectionMatchClass.CATALOG)
        )
    }

    @Test
    fun deviceClassPolicyTreatsWearableAndAudioVideoGlasses() {
        assertTrue(
            BluetoothDeviceClassPolicy.isGlassesDeviceClass(
                BluetoothDeviceClassPolicy.WEARABLE_GLASSES
            )
        )
        assertTrue(
            BluetoothDeviceClassPolicy.isGlassesDeviceClass(
                BluetoothDeviceClassPolicy.AUDIO_VIDEO_GLASSES
            )
        )
        assertFalse(BluetoothDeviceClassPolicy.isGlassesDeviceClass(0x0418))
        assertEquals(
            BluetoothDeviceClassPolicy.WEARABLE_GLASSES,
            BluetoothDeviceClassPolicy.preferGlassesDeviceClass(
                primary = 0x0418,
                fallback = BluetoothDeviceClassPolicy.WEARABLE_GLASSES
            )
        )
    }

    @Test
    fun advertisedNameSkipsPlaceholders() {
        assertEquals(
            "G2_12_L",
            BluetoothAdvertisedNamePolicy.resolve("G2_12_L", "Unknown", "My Glasses")
        )
        assertNull(BluetoothAdvertisedNamePolicy.resolve(" ", "", null))
    }

    @Test
    fun notificationIdsStayStablePerDevice() {
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
        assertNotEquals(
            first,
            DetectionNotificationPolicy.notificationId(
                address = "AA:BB:CC:DD:EE:02",
                name = "Even G2_12_L"
            )
        )
    }

    @Test
    fun bleUuidNormalizesAssignedNumbers() {
        assertEquals(
            "0000FD5F-0000-1000-8000-00805F9B34FB",
            BleUuid.normalize("0xfd5f")
        )
        assertTrue(BleUuid.matches("0000fd5f-0000-1000-8000-00805f9b34fb", "FD5F"))
    }
}
