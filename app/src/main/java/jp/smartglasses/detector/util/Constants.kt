package jp.smartglasses.detector.util

internal data class DetectionRule(
    val manufacturerName: String,
    val companyIds: Set<Int> = emptySet(),
    val namePatterns: List<String> = emptyList(),
    val serviceUuids: Set<String> = emptySet(),
    val payloadPatterns: List<String> = emptyList(),
    val manufacturerDataSuffixes: Set<Int> = emptySet(),
    val allowCompanyIdOnly: Boolean = true
)

object Constants {
    const val NOTIFICATION_CHANNEL_ID_SCANNING = "scanning_channel"
    const val NOTIFICATION_CHANNEL_ID_DETECTION = "detection_channel"
    const val NOTIFICATION_ID_SCANNING = 1001
    const val NOTIFICATION_ID_DETECTION = 1002

    internal const val UNKNOWN_RSSI_DBM = -127
    internal const val COOLDOWN_SAME_DEVICE_MS = 30_000L
    internal const val COOLDOWN_SAME_MANUFACTURER_MS = 15_000L
    internal const val BLE_SCAN_REFRESH_INTERVAL_MS = 15 * 60 * 1000L
    internal const val SCAN_HEALTH_CHECK_INTERVAL_MS = 15_000L
    internal const val NEARBY_DEVICE_TTL_MS = 20_000L
    internal const val NEARBY_DEVICE_PRUNE_INTERVAL_MS = 5_000L

    internal const val GENERIC_SMART_GLASSES_NAME = "スマートグラス"

    internal val GENERIC_GLASSES_NAME_REGEXES = listOf(
        Regex("""(?i)smart[\s-]?glass"""),
        Regex("""(?i)\b(?:ai|ar|xr|mr)[\s-]?glass"""),
        Regex("""(?i)eye-?wear"""),
        Regex("""(?i)\beyewear\b"""),
        Regex("""(?i)spectacles"""),
        Regex("""(?i)eye[\s-]?glasses"""),
        Regex("""(?i)\bglasses\b"""),
        Regex("""(?i)\bhud\b"""),
        Regex("""(?i)ai[\s-]?eyewear"""),
        Regex("""(?i)ar[\s-]?eyewear"""),
        Regex("""スマートグラス"""),
        Regex("""アイウェア"""),
        Regex("""ARグラス"""),
        Regex("""XRグラス"""),
        Regex("""AIグラス""")
    )

    internal val GENERIC_NON_GLASSES_NAME_REGEXES = listOf(
        Regex("""(?i)airpods"""),
        Regex("""(?i)pixel\s*buds"""),
        Regex("""(?i)galaxy\s*buds"""),
        Regex("""(?i)headphones?"""),
        Regex("""(?i)earbuds?"""),
        Regex("""(?i)\bheadset\b"""),
        Regex("""(?i)\bspeaker\b""")
    )

    internal val SMART_GLASSES_DETECTION_RULES = listOf(
        DetectionRule(
            manufacturerName = "Seiko Epson",
            companyIds = setOf(0x0040),
            namePatterns = listOf(
                "Moverio",
                "Epson BT",
                "BT-40",
                "BT-35",
                "BT-30"
            )
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
            companyIds = setOf(0x00E0, 0x018E),
            namePatterns = listOf(
                "Google Glass",
                "Glass EE",
                "Glass Enterprise",
                "Android XR",
                "Warby Parker",
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
            manufacturerName = "Meta Platforms",
            companyIds = setOf(0x01AB, 0x058E),
            namePatterns = listOf(
                "Ray-Ban",
                "RayBan",
                "Ray Ban",
                "META_RB",
                "Oakley Meta",
                "Oakley HSTN",
                "Ray-Ban Display",
                "RayBan Display",
                "Ray-Ban Stories",
                "RayBan Stories"
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
            namePatterns = listOf("Spectacles", "Snap Glass", "Snap Spectacles"),
            serviceUuids = setOf("0000FE45-0000-1000-8000-00805F9B34FB")
        ),
        DetectionRule(
            manufacturerName = "TCL",
            companyIds = setOf(0x0BC6),
            namePatterns = listOf(
                "RayNeo",
                "NXTWEAR",
                "TCL Glass",
                "RayNeo X2",
                "RayNeo X3",
                "RayNeo Air"
            )
        ),
        DetectionRule(
            manufacturerName = "Luxottica",
            companyIds = setOf(0x0D53),
            namePatterns = listOf(
                "Ray-Ban",
                "RayBan",
                "Oakley Meta",
                "Oakley HSTN",
                "Ray-Ban Stories",
                "Essilor"
            )
        ),
        DetectionRule(
            manufacturerName = "Vuzix",
            companyIds = setOf(0x060C),
            namePatterns = listOf(
                "Vuzix",
                "Vuzix Blade",
                "Vuzix Shield",
                "Vuzix M400",
                "Ultralite",
                "Vuzix Z100"
            )
        ),
        DetectionRule(
            manufacturerName = "Kopin",
            companyIds = setOf(0x041F),
            namePatterns = listOf("Solos", "AirGo", "Solos AirGo", "AirGo Vision")
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
                "Galaxy Glasses",
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
            namePatterns = listOf(
                "XREAL",
                "Nreal",
                "XREAL One",
                "XREAL One Pro",
                "XREAL Air",
                "Nreal Air",
                "Nreal Light"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Rokid",
            namePatterns = listOf(
                "Rokid",
                "Rokid Max",
                "Rokid Max 2",
                "Rokid Max Ultra",
                "Rokid Glasses",
                "Rokid Glass",
                "Rokid AR Lite"
            ),
            serviceUuids = setOf("00009100-0000-1000-8000-00805F9B34FB"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "INMO",
            namePatterns = listOf(
                "INMO",
                "INMO Air",
                "INMO Air2",
                "INMO Air 3",
                "INMO GO",
                "INMO GO2"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Looktech",
            namePatterns = listOf("Looktech"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "LAWAKEN",
            namePatterns = listOf("LAWAKEN", "LAWKAN"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Halliday",
            namePatterns = listOf("Halliday", "Halliday Glass", "Halliday AI"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "VITURE",
            namePatterns = listOf(
                "VITURE",
                "VITURE One",
                "VITURE Beast",
                "VITURE Luma",
                "VITURE Pro"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Even Realities",
            companyIds = setOf(0x10F9),
            namePatterns = listOf(
                "Even Realities",
                "Even G1",
                "Even-G1",
                "EvenG1",
                "Even G2",
                "Even-G2",
                "EvenG2",
                "G1_",
                "G1-"
            )
        ),
        DetectionRule(
            manufacturerName = "Brilliant Labs",
            namePatterns = listOf(
                "Brilliant Labs",
                "Brilliant Frame",
                "Brilliant Halo",
                "Monocle",
                "Frame-"
            ),
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
            namePatterns = listOf(
                "Mentra",
                "Mentra Live",
                "Mentra Mach",
                "MENTRA_LIVE_BLE",
                "MENTRA_LIVE_BT",
                "XyBLE_",
                "NIMO"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Engo",
            namePatterns = listOf("Engo", "ActiveLook", "A.Look"),
            companyIds = setOf(0x08F2),
            serviceUuids = setOf("0783B03E-8535-B5A0-7140-A304D2495CB7"),
            manufacturerDataSuffixes = setOf(0x08F2)
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
            companyIds = setOf(0x08CA),
            namePatterns = listOf("Nubia Glass", "Neovision"),
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
        ),
        DetectionRule(
            manufacturerName = "Magic Leap",
            namePatterns = listOf("Magic Leap", "MagicLeap"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "DigiLens",
            namePatterns = listOf("DigiLens"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Shiftall",
            namePatterns = listOf("MeganeX", "Shiftall"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Soundcore",
            companyIds = setOf(0x0CC2),
            namePatterns = listOf("Soundcore Frame", "Soundcore Frames"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Vue",
            namePatterns = listOf("Vue Smart", "Vue Glass"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Honor",
            namePatterns = listOf("Honor Glass", "Honor Glasses"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Vivo",
            namePatterns = listOf("Vivo Glass", "Vivo Glasses"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Nothing",
            namePatterns = listOf("Nothing Glass", "Nothing Glasses"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Pico",
            namePatterns = listOf("Pico Glass", "Pico Glasses"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Ampere",
            namePatterns = listOf("Ampere"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Lucyd",
            namePatterns = listOf("Lucyd", "Lucyd Lyte"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Tooz",
            namePatterns = listOf("Tooz"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Mojo Vision",
            namePatterns = listOf("Mojo Vision", "Mojo Lens"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "ThirdEye",
            namePatterns = listOf("ThirdEye"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Zungle",
            namePatterns = listOf("Zungle"),
            allowCompanyIdOnly = false
        )
    )
}

enum class ScanSensitivity {
    LOW_POWER,
    BALANCED,
    HIGH_ACCURACY
}
