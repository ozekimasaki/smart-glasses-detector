package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.domain.model.DetectionMethod
import jp.smartglasses.detector.util.Constants
import jp.smartglasses.detector.util.DetectionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EmulatedAdvertisementCoverageTest {
    private val classifier = SmartGlassesClassifier()
    private val catalog = EmulatorCatalog.load()

    @Test
    fun `emulator company id catalog matches classifier policy`() {
        assertTrue(catalog.companyIdDevices.size >= 18)

        catalog.companyIdDevices.forEach { device ->
            val deviceName = device.label.substringBefore('(').trim()
            val matchingRules = Constants.SMART_GLASSES_DETECTION_RULES.filter { rule ->
                device.companyId in rule.companyIds
            }
            assertTrue(
                "${device.label} company id 0x${device.companyId.toString(16).uppercase()} should exist in the catalog",
                matchingRules.isNotEmpty()
            )

            val detected = classifier.classify(
                DetectionSignal(
                    deviceName = deviceName,
                    address = "AA:BB:CC:DD:EE:60",
                    companyIds = setOf(device.companyId),
                    rssi = -55
                )
            )

            if (shouldDetectCompanyId(deviceName, matchingRules)) {
                assertNotNull("${device.label} should be detected", detected)
                assertEquals(matchingRules.first().manufacturerName, detected?.manufacturer?.name)
            } else {
                assertNull(
                    "${device.label} is documented as company-id-only ignored",
                    detected
                )
            }
        }
    }

    @Test
    fun `emulator name catalog is detected`() {
        assertTrue(catalog.nameDevices.size >= 28)
        assertTrue(
            "Halo official coded name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "Halo 4F" }
        )
        assertTrue(
            "Brilliant Frame official coded name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "Frame 4F" }
        )
        assertTrue(
            "Mentra Live lowercase prefix should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "mentra_live_abc" }
        )
        assertTrue(
            "Mentra Nex advertised prefix should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "Nex1-77" }
        )
        assertTrue(
            "NIMO classic name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "Nimo-A1B2" }
        )
        assertTrue(
            "Lucyd FCC model name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "LCD008-10" }
        )
        assertTrue(
            "Halliday G2 name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "Halliday G2" }
        )
        assertTrue(
            "Rokid Glasses coded name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "Glasses_A1B2" }
        )
        assertTrue(
            "Solos AirGo3 pairing name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "Solos AirGo3 1234" }
        )
        assertTrue(
            "Solos AirGo 3 spaced pairing name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "Solos AirGo 3 1234" }
        )
        assertTrue(
            "RayNeo Air 4 Pro name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "RayNeo Air 4 Pro" }
        )
        assertTrue(
            "Mentra Display legacy NexSim name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "NexSim A1B2C3" }
        )
        assertTrue(
            "RayNeo Chinese Air name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "雷鸟Air 2" }
        )
        assertTrue(
            "Rokid Chinese brand name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "若琪眼镜" }
        )
        assertTrue(
            "INMO Chinese brand name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "映莫GO2" }
        )
        assertTrue(
            "Huawei Chinese eyewear name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "华为眼镜" }
        )
        assertTrue(
            "Even G1 official coded name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "G1_12_L" } &&
                catalog.nameDevices.any { device -> device.name == "G1_12_R" }
        )
        assertTrue(
            "Recent catalog product names should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "XREAL One Pro" } &&
                catalog.nameDevices.any { device -> device.name == "Oakley Meta" } &&
                catalog.nameDevices.any { device -> device.name == "VITURE Beast" } &&
                catalog.nameDevices.any { device -> device.name == "Rokid Max 2" } &&
                catalog.nameDevices.any { device -> device.name == "Echo Frames 2" }
        )
        assertTrue(
            "Japanese smart megane heuristic name should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "スマートメガネ" }
        )
        assertTrue(
            "Recent USB-C era product names should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "RayNeo Air 3s" } &&
                catalog.nameDevices.any { device -> device.name == "XREAL One S" } &&
                catalog.nameDevices.any { device -> device.name == "VITURE Luma Pro" }
        )
        assertTrue(
            "Strong heuristic and 2026 brand names should be in the emulator",
            catalog.nameDevices.any { device -> device.name == "Smart Glasses" } &&
                catalog.nameDevices.any { device -> device.name == "スマートグラス" } &&
                catalog.nameDevices.any { device -> device.name == "MemoMind One" } &&
                catalog.nameDevices.any { device -> device.name == "Dymesty Cook Edge" } &&
                catalog.nameDevices.any { device -> device.name == "小度AI眼镜" } &&
                catalog.nameDevices.any { device -> device.name == "Monako Glass" } &&
                catalog.nameDevices.any { device -> device.name == "讯飞AI眼镜" } &&
                catalog.nameDevices.any { device -> device.name == "夸克AI眼镜" } &&
                catalog.nameDevices.any { device -> device.name == "豆包AI眼镜" } &&
                catalog.nameDevices.any { device -> device.name == "Everysight Maverick" } &&
                catalog.nameDevices.any { device -> device.name == "OpenGlass" } &&
                catalog.nameDevices.any { device -> device.name == "XRAI Glass" } &&
                catalog.nameDevices.any { device -> device.name == "HEY2_A1B2" } &&
                catalog.nameDevices.any { device -> device.name == "AirScouter WD-200B" } &&
                catalog.nameDevices.any { device -> device.name == "RETISSA ON" } &&
                catalog.nameDevices.any { device -> device.name == "字幕眼镜-01" } &&
                catalog.nameDevices.any { device -> device.name == "OnePlus Glasses" } &&
                catalog.nameDevices.any { device -> device.name == "Memo Air Display" } &&
                catalog.nameDevices.any { device -> device.name == ".lumen-A1B2" } &&
                catalog.nameDevices.any { device -> device.name == "Nitrous Shift" } &&
                catalog.nameDevices.any { device -> device.name == "Rokid Style" } &&
                catalog.nameDevices.any { device -> device.name == "Lunettes intelligentes-01" } &&
                catalog.nameDevices.any { device -> device.name == "AceSight VR" } &&
                catalog.nameDevices.any { device -> device.name == "SightPlus" } &&
                catalog.nameDevices.any { device -> device.name == "Recon Jet" } &&
                catalog.nameDevices.any { device -> device.name == "Golden-i 5" }
        )

        catalog.nameDevices.forEach { device ->
            val detected = classifier.classify(
                DetectionSignal(
                    deviceName = device.name,
                    address = "AA:BB:CC:DD:EE:61",
                    companyIds = emptySet(),
                    rssi = -55
                )
            )
            assertNotNull("${device.name} should be detected", detected)
        }
    }

    @Test
    fun `emulator 16-bit uuid catalog is detected`() {
        assertTrue(catalog.uuid16Devices.size >= 2)

        catalog.uuid16Devices.forEach { device ->
            val uuid = BleUuid.normalize("%04X".format(device.uuid16))
            val detected = classifier.classify(
                DetectionSignal(
                    deviceName = null,
                    address = "AA:BB:CC:DD:EE:62",
                    companyIds = emptySet(),
                    rssi = -55,
                    serviceUuids = listOf(uuid)
                )
            )
            assertNotNull("${device.label} uuid 0x${device.uuid16.toString(16)} should be detected", detected)
            assertEquals(DetectionMethod.SERVICE_UUID, detected?.manufacturer?.detectionMethod)
        }
    }

    @Test
    fun `emulator 128-bit uuid catalog is detected`() {
        assertTrue(catalog.uuid128Devices.isNotEmpty())

        catalog.uuid128Devices.forEach { device ->
            val detected = classifier.classify(
                DetectionSignal(
                    deviceName = null,
                    address = "AA:BB:CC:DD:EE:63",
                    companyIds = emptySet(),
                    rssi = -55,
                    serviceUuids = listOf(device.uuid)
                )
            )
            assertNotNull("${device.label} uuid ${device.uuid} should be detected", detected)
            assertEquals(DetectionMethod.SERVICE_UUID, detected?.manufacturer?.detectionMethod)
        }
    }

    @Test
    fun `emulator payload catalog is detected`() {
        assertTrue(catalog.payloadDevices.isNotEmpty())
        assertTrue(
            "Meta Ray-Ban payload should be in the emulator",
            catalog.payloadDevices.any { device -> device.ascii == "META_RB_GLASS" }
        )

        catalog.payloadDevices.forEach { device ->
            val payload = device.ascii.encodeToByteArray()
            val advertisement = buildString {
                append("020106")
                append("%02X".format(1 + payload.size))
                append("FF")
                append(payload.joinToString("") { byte ->
                    (byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
                })
            }
            val detected = classifier.classify(
                DetectionSignal(
                    deviceName = null,
                    address = "AA:BB:CC:DD:EE:64",
                    companyIds = emptySet(),
                    rssi = -55,
                    advertisementDataHex = advertisement
                )
            )
            assertNotNull("${device.label} payload ${device.ascii} should be detected", detected)
            assertEquals(DetectionMethod.PAYLOAD, detected?.manufacturer?.detectionMethod)
        }
    }

    @Test
    fun `emulator eyeglasses appearance catalog is detected`() {
        assertTrue(catalog.appearanceDevices.isNotEmpty())

        catalog.appearanceDevices.forEach { device ->
            val low = device.appearance and 0xFF
            val high = (device.appearance shr 8) and 0xFF
            val advertisement = "0201060319%02X%02X".format(low, high)
            val detected = classifier.classify(
                DetectionSignal(
                    deviceName = null,
                    address = "AA:BB:CC:DD:EE:65",
                    companyIds = emptySet(),
                    rssi = -55,
                    advertisementDataHex = advertisement
                )
            )
            assertNotNull("${device.label} appearance 0x${device.appearance.toString(16)} should be detected", detected)
            assertEquals(DetectionMethod.APPEARANCE, detected?.manufacturer?.detectionMethod)
        }
    }

    @Test
    fun `emulator non-glasses controls stay undetected`() {
        assertTrue(catalog.nonGlassesNames.size >= 4)
        assertTrue(
            "Even R1 controller ring should stay in the non-glasses catalog",
            catalog.nonGlassesNames.contains("R1") &&
                catalog.nonGlassesNames.contains("Even R1") &&
                catalog.nonGlassesNames.contains("Even Realities R1") &&
                catalog.nonGlassesNames.contains("Frame TV") &&
                catalog.nonGlassesNames.contains("雷鸟TV") &&
                catalog.nonGlassesNames.contains("华为手表")
        )

        catalog.nonGlassesNames.forEach { name ->
            val detected = classifier.classify(
                DetectionSignal(
                    deviceName = name,
                    address = "AA:BB:CC:DD:EE:66",
                    companyIds = emptySet(),
                    rssi = -50
                )
            )
            assertNull("$name should not be classified as smart glasses", detected)
        }
    }

    private fun shouldDetectCompanyId(
        deviceName: String,
        matchingRules: List<DetectionRule>
    ): Boolean {
        return matchingRules.any { rule ->
            val excluded = rule.excludedNamePatterns.any { pattern ->
                deviceName.contains(pattern, ignoreCase = true)
            } || rule.excludedNameRegexes.any { regex ->
                regex.containsMatchIn(deviceName)
            }
            when {
                excluded -> false
                rule.allowCompanyIdOnly -> true
                else -> rule.namePatterns.any { pattern ->
                    deviceName.contains(pattern, ignoreCase = true)
                } || rule.nameRegexes.any { regex ->
                    regex.containsMatchIn(deviceName)
                }
            }
        }
    }
}

private data class EmulatorCompanyIdDevice(val label: String, val companyId: Int)
private data class EmulatorNameDevice(val name: String)
private data class EmulatorUuid16Device(val label: String, val uuid16: Int)
private data class EmulatorUuid128Device(val label: String, val uuid: String)
private data class EmulatorPayloadDevice(val label: String, val ascii: String)
private data class EmulatorAppearanceDevice(val label: String, val appearance: Int)

private data class ParsedEmulatorCatalog(
    val companyIdDevices: List<EmulatorCompanyIdDevice>,
    val nameDevices: List<EmulatorNameDevice>,
    val uuid16Devices: List<EmulatorUuid16Device>,
    val uuid128Devices: List<EmulatorUuid128Device>,
    val payloadDevices: List<EmulatorPayloadDevice>,
    val appearanceDevices: List<EmulatorAppearanceDevice>,
    val nonGlassesNames: List<String>
)

private object EmulatorCatalog {
    fun load(): ParsedEmulatorCatalog {
        val text = locateScript().readText()
        return ParsedEmulatorCatalog(
            companyIdDevices = parseLabeledHexEntries(section(text, "COMPANY_ID_DEVICES"))
                .map { (label, value) -> EmulatorCompanyIdDevice(label, value) },
            nameDevices = parseQuotedValues(section(text, "NAME_PATTERN_DEVICES"))
                .map(::EmulatorNameDevice),
            uuid16Devices = parseLabeledHexEntries(section(text, "UUID_DEVICES"))
                .map { (label, value) -> EmulatorUuid16Device(label, value) },
            uuid128Devices = parseLabeledQuotedEntries(section(text, "UUID128_DEVICES"))
                .map { (label, uuid) -> EmulatorUuid128Device(label, uuid) },
            payloadDevices = parseLabeledQuotedEntries(section(text, "PAYLOAD_DEVICES"))
                .map { (label, ascii) -> EmulatorPayloadDevice(label, ascii) },
            appearanceDevices = parseLabeledHexEntries(section(text, "APPEARANCE_DEVICES"))
                .map { (label, value) -> EmulatorAppearanceDevice(label, value) },
            nonGlassesNames = parseQuotedValues(section(text, "NON_GLASSES_NAME_DEVICES"))
        )
    }

    private fun locateScript(): File {
        val userDir = System.getProperty("user.dir")
            ?: error("user.dir is missing")
        var dir: File? = File(userDir).canonicalFile
        repeat(6) {
            val current = dir ?: return@repeat
            val candidate = File(current, "tools/ble_smartglasses_emulator.py")
            if (candidate.isFile) {
                return candidate
            }
            dir = current.parentFile
        }
        error("tools/ble_smartglasses_emulator.py was not found from ${System.getProperty("user.dir")}")
    }

    private fun section(text: String, name: String): String {
        val regex = Regex("""$name\s*=\s*\{(.*?)\n\}""", RegexOption.DOT_MATCHES_ALL)
        return regex.find(text)?.groupValues?.get(1)
            ?: error("emulator section $name was not found")
    }

    private fun parseLabeledHexEntries(body: String): List<Pair<String, Int>> {
        return Regex("""\(\s*"([^"]+)"\s*,\s*0x([0-9A-Fa-f]+)\s*\)""")
            .findAll(body)
            .map { match -> match.groupValues[1] to match.groupValues[2].toInt(16) }
            .toList()
    }

    private fun parseLabeledQuotedEntries(body: String): List<Pair<String, String>> {
        return Regex("""\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)""")
            .findAll(body)
            .map { match -> match.groupValues[1] to match.groupValues[2] }
            .toList()
    }

    private fun parseQuotedValues(body: String): List<String> {
        return Regex(""""([^"]+)"""")
            .findAll(body)
            .map { match -> match.groupValues[1] }
            .toList()
    }
}
