package jp.smartglasses.detector.util

internal data class DetectionRule(
    val manufacturerName: String,
    val companyIds: Set<Int> = emptySet(),
    val namePatterns: List<String> = emptyList(),
    val serviceUuids: Set<String> = emptySet(),
    val payloadPatterns: List<String> = emptyList(),
    val allowCompanyIdOnly: Boolean = true
)

object Constants {
    const val NOTIFICATION_CHANNEL_ID_SCANNING = "scanning_channel"
    const val NOTIFICATION_CHANNEL_ID_DETECTION = "detection_channel"
    const val NOTIFICATION_ID_SCANNING = 1001
    const val NOTIFICATION_ID_DETECTION = 1002

    internal const val MIN_DETECTION_RSSI_DBM = -75
    internal const val UNKNOWN_RSSI_DBM = -127
    internal const val COOLDOWN_SAME_DEVICE_MS = 30_000L
    internal const val COOLDOWN_SAME_MANUFACTURER_MS = 15_000L

    internal const val GENERIC_SMART_GLASSES_NAME = "スマートグラス"

    internal val GENERIC_GLASSES_NAME_REGEXES = listOf(
        Regex("""(?i)smart[\s-]?glass"""),
        Regex("""(?i)\b(?:ai|ar|xr|mr)[\s-]?glass"""),
        Regex("""(?i)eye-?wear"""),
        Regex("""(?i)\beyewear\b"""),
        Regex("""(?i)spectacles"""),
        Regex("""(?i)eye[\s-]?glasses"""),
        Regex("""(?i)\bglasses\b"""),
        Regex("""スマートグラス"""),
        Regex("""アイウェア""")
    )

    internal val SMART_GLASSES_DETECTION_RULES = listOf(
        DetectionRule(
            manufacturerName = "Seiko Epson",
            companyIds = setOf(0x0040),
            namePatterns = listOf("Moverio", "Epson BT")
        ),
        DetectionRule(
            manufacturerName = "Apple",
            companyIds = setOf(0x004C),
            namePatterns = listOf(
                "Vision Pro",
                "Apple Vision",
                "Apple Glass",
                "Apple Glasses"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Google",
            companyIds = setOf(0x00E0),
            namePatterns = listOf(
                "Google Glass",
                "Glass EE",
                "Glass Enterprise",
                "Android XR",
                "Warby"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Amazon",
            companyIds = setOf(0x0171),
            namePatterns = listOf("Echo Frame", "Echo Frames", "Amazon Frame")
        ),
        DetectionRule(
            manufacturerName = "Google LLC",
            companyIds = setOf(0x018E),
            namePatterns = listOf(
                "Google Glass",
                "Glass EE",
                "Glass Enterprise",
                "Android XR"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Meta Platforms",
            companyIds = setOf(0x01AB),
            namePatterns = listOf(
                "Ray-Ban",
                "RayBan",
                "Ray Ban",
                "META_RB",
                "Oakley Meta"
            ),
            serviceUuids = setOf("0000FD5F-0000-1000-8000-00805F9B34FB"),
            payloadPatterns = listOf("META_RB_GLASS")
        ),
        DetectionRule(
            manufacturerName = "Huawei",
            companyIds = setOf(0x027D),
            namePatterns = listOf(
                "HUAWEI Eyewear 2",
                "HUAWEI Eyewear",
                "OWNDAYS",
                "HW1001",
                "HW1002",
                "HWF2003N",
                "HWF2004N",
                "HWF2005N",
                "HWF2006N"
            )
        ),
        DetectionRule(
            manufacturerName = "Lenovo",
            companyIds = setOf(0x02C5),
            namePatterns = listOf(
                "Legion Glass",
                "ThinkReality",
                "Lenovo Glass"
            )
        ),
        DetectionRule(
            manufacturerName = "Meizu",
            companyIds = setOf(0x03AB),
            namePatterns = listOf("MYVU", "StarV", "Meizu Glass")
        ),
        DetectionRule(
            manufacturerName = "Snapchat",
            companyIds = setOf(0x03C2),
            namePatterns = listOf("Spectacles", "Snap Glass")
        ),
        DetectionRule(
            manufacturerName = "Meta Tech",
            companyIds = setOf(0x058E),
            namePatterns = listOf(
                "Ray-Ban",
                "RayBan",
                "Ray Ban",
                "META_RB",
                "Oakley Meta"
            ),
            serviceUuids = setOf("0000FD5F-0000-1000-8000-00805F9B34FB"),
            payloadPatterns = listOf("META_RB_GLASS")
        ),
        DetectionRule(
            manufacturerName = "TCL",
            companyIds = setOf(0x0BC6),
            namePatterns = listOf("RayNeo", "NXTWEAR", "TCL Glass")
        ),
        DetectionRule(
            manufacturerName = "Luxottica",
            companyIds = setOf(0x0D53),
            namePatterns = listOf("Ray-Ban", "RayBan", "Oakley Meta", "Essilor")
        ),
        DetectionRule(
            manufacturerName = "Vuzix",
            companyIds = setOf(0x060C),
            namePatterns = listOf("Vuzix", "Vuzix Blade", "Vuzix Shield", "Vuzix M400")
        ),
        DetectionRule(
            manufacturerName = "Kopin",
            companyIds = setOf(0x041F),
            namePatterns = listOf("Solos", "AirGo")
        ),
        DetectionRule(
            manufacturerName = "North",
            companyIds = setOf(0x0562),
            namePatterns = listOf("Focals", "North Focals")
        ),
        DetectionRule(
            manufacturerName = "Sony",
            companyIds = setOf(0x012D),
            namePatterns = listOf("SmartEyeglass", "Sony Glass", "Xperia View"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Fauna",
            companyIds = setOf(0x0976),
            namePatterns = listOf("Fauna")
        ),
        DetectionRule(
            manufacturerName = "Xiaomi",
            companyIds = setOf(0x038F),
            namePatterns = listOf(
                "Xiaomi Glass",
                "Mi Glass",
                "Wireless AR",
                "Xiaomi Smart Glass"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Oppo",
            companyIds = setOf(0x079A),
            namePatterns = listOf("Air Glass", "Oppo Glass"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Samsung",
            companyIds = setOf(0x0075),
            namePatterns = listOf(
                "Galaxy Glass",
                "Samsung Glass",
                "Samsung XR"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Bose",
            companyIds = setOf(0x009E),
            namePatterns = listOf("Bose Frame", "Bose Frames", "Frames Tempo"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Razer",
            companyIds = setOf(0x068E),
            namePatterns = listOf("Anzu", "Razer Anzu"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "XREAL",
            namePatterns = listOf("XREAL", "Nreal"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Rokid",
            namePatterns = listOf("Rokid"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "INMO",
            namePatterns = listOf("INMO"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Looktech",
            namePatterns = listOf("Looktech"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "LAWAKEN",
            namePatterns = listOf("LAWAKEN"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Halliday",
            namePatterns = listOf("Halliday"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "VITURE",
            namePatterns = listOf("VITURE"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Even Realities",
            namePatterns = listOf(
                "Even Realities",
                "Even G1",
                "Even-G1",
                "EvenG1"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Brilliant Labs",
            namePatterns = listOf("Brilliant Labs", "Brilliant Frame", "Monocle"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "HeyCyan",
            namePatterns = listOf("HeyCyan", "Nilox"),
            serviceUuids = setOf("7905FFF0-B5CE-4E99-A40F-4B1E122D00D0"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Rogbird",
            namePatterns = listOf("Rogbird", "VistaView", "Rollme"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Mentra",
            namePatterns = listOf("Mentra"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Engo",
            namePatterns = listOf("Engo"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "JINS",
            namePatterns = listOf("JINS MEME", "JINS-MEME"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Thunderbird",
            namePatterns = listOf("Thunderbird"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "LLVision",
            namePatterns = listOf("LLVision", "Leion"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Mad Gaze",
            namePatterns = listOf("Mad Gaze", "MADGaze"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Nubia",
            namePatterns = listOf("Nubia Glass", "Neovision"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Vuzix Companion",
            namePatterns = listOf("Ultralite", "Vuzix Z100"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "RealWear",
            namePatterns = listOf("RealWear", "Navigator-"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Iristick",
            namePatterns = listOf("Iristick"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "OrCam",
            namePatterns = listOf("OrCam", "MyEye"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Envision",
            namePatterns = listOf("Envision Glass"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "eSight",
            namePatterns = listOf("eSight"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "NuEyes",
            namePatterns = listOf("NuEyes"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Microsoft",
            namePatterns = listOf("HoloLens"),
            allowCompanyIdOnly = false
        )
    )
}

enum class ScanSensitivity {
    LOW_POWER,
    BALANCED,
    HIGH_ACCURACY
}
