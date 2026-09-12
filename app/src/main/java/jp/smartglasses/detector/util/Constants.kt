package jp.smartglasses.detector.util

internal data class DetectionRule(
    val manufacturerName: String,
    val companyIds: Set<Int> = emptySet(),
    val namePatterns: List<String> = emptyList(),
    val nameRegexes: List<Regex> = emptyList(),
    val excludedNamePatterns: List<String> = emptyList(),
    val excludedNameRegexes: List<Regex> = emptyList(),
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
    internal const val BLE_SCAN_REFRESH_INTERVAL_MS = 4 * 60 * 1000L
    internal const val BLE_SCAN_FOREGROUND_REFRESH_INTERVAL_MS = 45_000L
    internal const val SCAN_HEALTH_CHECK_INTERVAL_MS = 15_000L
    internal const val CLASSIC_DISCOVERY_DELAY_MS = 15_000L
    internal const val CLASSIC_DISCOVERY_REFRESH_INTERVAL_MS = 2 * 60 * 1000L
    // Classic inquiry は前面でも 2 分間隔のため、最後の信号から次の inquiry まで近くに残す
    internal const val NEARBY_DEVICE_TTL_MS = CLASSIC_DISCOVERY_REFRESH_INTERVAL_MS
    internal const val NEARBY_DEVICE_PRUNE_INTERVAL_MS = 5_000L
    internal const val DIAGNOSTIC_LOG_KEEP_COUNT = 500
    internal const val DIAGNOSTIC_LOG_WRITE_COOLDOWN_MS = 30_000L
    internal const val DETECTION_LOG_KEEP_COUNT = 1000

    internal const val GENERIC_SMART_GLASSES_NAME = "スマートグラス"

    // 未知メーカーでも Smart Glasses / スマートグラスと名乗る広告はカタログ距離で拾う
    internal val GENERIC_STRONG_GLASSES_NAME_REGEXES = listOf(
        Regex("""(?i)smart[\s-]?eye[\s-]?glass"""),
        Regex("""(?i)smart[\s-]?glass"""),
        Regex("""(?i)\b(?:ai|ar|xr|mr)[\s-]?eye[\s-]?glass"""),
        Regex("""(?i)\b(?:ai|ar|xr|mr)[\s-]?glass"""),
        Regex("""(?i)camera[\s-]?glass"""),
        Regex("""(?i)(?:ai|smart)[\s-]?sunglass"""),
        Regex("""(?i)(?:ai|ar|xr|mr|smart)[\s-]?eyewear"""),
        Regex("""スマートグラス"""),
        Regex("""スマートメガネ"""),
        Regex("""スマート眼鏡"""),
        Regex("""スマートアイウェア"""),
        Regex("""AIメガネ"""),
        Regex("""AI眼鏡"""),
        Regex("""ARメガネ"""),
        Regex("""AR眼鏡"""),
        Regex("""撮影グラス"""),
        Regex("""翻訳グラス"""),
        Regex("""翻译眼镜"""),
        Regex("""智能墨镜"""),
        Regex("""ARグラス"""),
        Regex("""XRグラス"""),
        Regex("""MRグラス"""),
        Regex("""AIグラス"""),
        Regex("""カメラグラス"""),
        Regex("""智能眼镜"""),
        Regex("""智能眼鏡"""),
        Regex("""스마트글래스"""),
        Regex("""스마트글라스"""),
        Regex("""스마트안경""")
    )

    // glasses / HUD / アイウェアはファッション眼鏡や汎用HUDに当たるため近い距離だけ
    internal val GENERIC_WEAK_GLASSES_NAME_REGEXES = listOf(
        Regex("""(?i)eye-?wear"""),
        Regex("""(?i)\beyewear\b"""),
        Regex("""(?i)spectacles"""),
        Regex("""(?i)eye[\s-]?glasses"""),
        Regex("""(?i)\bglasses\b"""),
        Regex("""(?i)\bhud\b"""),
        Regex("""アイウェア""")
    )

    internal val GENERIC_GLASSES_NAME_REGEXES =
        GENERIC_STRONG_GLASSES_NAME_REGEXES + GENERIC_WEAK_GLASSES_NAME_REGEXES

    internal val GENERIC_NON_GLASSES_NAME_REGEXES = listOf(
        Regex("""(?i)airpods"""),
        Regex("""(?i)pixel\s*buds"""),
        Regex("""(?i)galaxy\s*buds"""),
        Regex("""(?i)headphones?"""),
        Regex("""(?i)earbuds?"""),
        Regex("""(?i)\bheadset\b"""),
        Regex("""(?i)\bspeaker\b"""),
        Regex("""(?i)smart[\s-]?watch"""),
        Regex("""(?i)galaxy\s*watch"""),
        Regex("""(?i)pixel\s*watch"""),
        Regex("""(?i)fitbit"""),
        Regex("""(?i)airtag"""),
        Regex("""(?i)smart[\s-]?tag"""),
        Regex("""(?i)smart[\s-]?band"""),
        Regex("""(?i)smart[\s-]?ring"""),
        Regex("""(?i)\bquest\b"""),
        Regex("""(?i)oculus""")
    )

    // メーカーデータの ASCII に GLASS / EYEWEAR が載っている無名広告（未知メーカー）
    // HOURGLASS / SUNGLASS は単語境界や区切りが無いので当てない
    internal val GENERIC_STRONG_PAYLOAD_REGEXES = listOf(
        Regex("""(?i)(?:^|[\s_\-./])glass(?:es)?(?:$|[\s_\-./0-9])"""),
        Regex("""(?i)(?:^|[\s_\-./])eyewear(?:$|[\s_\-./0-9])"""),
        Regex("""(?i)smart[\s-_]?glass""")
    )

    internal val SMART_GLASSES_DETECTION_RULES = listOf(
        DetectionRule(
            manufacturerName = "Seiko Epson",
            companyIds = setOf(0x0040),
            namePatterns = listOf(
                "Moverio",
                "Epson BT",
                "BT-45",
                "BT-45C",
                "BT-45CS",
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
            namePatterns = listOf(
                "Echo Frame",
                "Echo Frames",
                "Echo Frames 2",
                "Amazon Frame"
            ),
            excludedNamePatterns = listOf(
                "Echo Dot",
                "Echo Show",
                "Echo Spot",
                "Echo Studio",
                "Fire TV",
                "Fire Stick",
                "Kindle"
            )
        ),
        DetectionRule(
            manufacturerName = "Meta Platforms",
            companyIds = setOf(0x01AB, 0x058E),
            namePatterns = listOf(
                "Ray-Ban",
                "RayBan",
                "Ray Ban",
                "META_RB",
                "Meta Glasses",
                "Meta Ray Ban",
                "Oakley Meta",
                "Oakley HSTN",
                "Oakley Vanguard",
                "Ray-Ban Display",
                "RayBan Display",
                "Ray-Ban Stories",
                "RayBan Stories",
                "Blayzer",
                "Scriber"
            ),
            excludedNamePatterns = listOf(
                "Quest",
                "Oculus",
                "Neural Band",
                "Meta Band"
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
                "HUAWEI Vision Glass",
                "Huawei Vision Glass",
                "OWNDAYS",
                "华为眼镜",
                "华为智能眼镜",
                "华为AI眼镜",
                "HW1001",
                "HW1002",
                "HWF2003N",
                "HWF2004N",
                "HWF2005N",
                "HWF2006N"
            ),
            excludedNamePatterns = listOf(
                "FreeBuds",
                "FreeLace",
                "Watch GT",
                "Watch FIT",
                "Watch Fit",
                "Watch",
                "手表",
                "Band"
            )
        ),
        DetectionRule(
            manufacturerName = "Lenovo",
            companyIds = setOf(0x02C5),
            namePatterns = listOf(
                "Legion Glass",
                "ThinkReality",
                "ThinkReality A3",
                "ThinkReality VRX",
                "Lenovo Glass",
                "Vision AI Glass",
                "Legion Glasses"
            )
        ),
        DetectionRule(
            manufacturerName = "Meizu",
            companyIds = setOf(0x03AB),
            namePatterns = listOf(
                "MYVU",
                "MYVU Explorer",
                "StarV",
                "StarV View",
                "Meizu Glass",
                "星纪眼镜"
            )
        ),
        DetectionRule(
            manufacturerName = "Snapchat",
            companyIds = setOf(0x03C2),
            namePatterns = listOf(
                "Spectacles",
                "Snap Glass",
                "Snap Spectacles",
                "Spectacles 5",
                "Snap Specs",
                "Snap SPECS"
            ),
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
                "RayNeo X3 Pro",
                "RayNeo V3",
                "RayNeo Air",
                "RayNeo Air 2",
                "RayNeo Air 3",
                "RayNeo Air 3s",
                "RayNeo Air 4",
                "RayNeo Air 4 Pro",
                "NXTWEAR S",
                "NXTWEAR AIR",
                // 中国向けブランド名。裸の「雷鸟」はテレビ等に当たるため使わない。
                "雷鸟Air",
                "雷鸟X2",
                "雷鸟X3",
                "雷鸟V3",
                "雷鸟眼镜",
                "雷鸟AR"
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
                "Oakley Vanguard",
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
                "Vuzix Blade 2",
                "Vuzix Shield",
                "Vuzix M400",
                "Vuzix M4000",
                "Ultralite",
                "Vuzix Z100"
            )
        ),
        DetectionRule(
            manufacturerName = "Kopin",
            companyIds = setOf(0x041F),
            namePatterns = listOf(
                "Solos",
                "AirGo",
                "Solos AirGo",
                "AirGo Vision",
                "AirGo 3",
                "AirGo3",
                "AirGo A5",
                "AirGo V2"
            )
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
                "Xiaomi Smart Glass",
                "Xiaomi AI Glass",
                "小米眼镜",
                "小米智能眼镜",
                "小米AI眼镜"
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
                "Galaxy XR",
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
                "XREAL One S",
                "XREAL 1S",
                "XREAL Eye",
                "XREAL Air",
                "XREAL Air 2",
                "XREAL Air 2 Pro",
                "XREAL Air 2 Ultra",
                "Nreal Air",
                "Nreal Light",
                "XREAL Aura",
                "XREAL R1",
                "ROG R1",
                "ROG XREAL"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Rokid",
            namePatterns = listOf(
                "Rokid",
                "Rokid Max",
                "Rokid Max 2",
                "Rokid Max2",
                "Rokid Max Ultra",
                "Rokid Glasses",
                "Rokid Glass",
                "Rokid AR Lite",
                "Rokid AR Spatial",
                "若琪"
            ),
            // 実機広告名: Glasses_XXXX（UUID 0x9100 が広告に乗らない場合の保険）
            nameRegexes = listOf(
                Regex("""(?i)^Glasses_[0-9A-Fa-z]+$""")
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
                "INMO Air3",
                "INMOAIR",
                "INMO GO",
                "INMO GO2",
                "INMO GO3",
                "INMO GO 3",
                "INMOGO",
                "映莫"
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
            namePatterns = listOf("LAWAKEN", "LAWKAN", "李未可"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Halliday",
            namePatterns = listOf(
                "Halliday",
                "Halliday Glass",
                "Halliday AI",
                "Halliday G2",
                "DigiWindow",
                "GP101",
                "HALLIDAYGP101"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "VITURE",
            namePatterns = listOf(
                "VITURE",
                "VITURE One",
                "VITURE One Lite",
                "VITURE Beast",
                "VITURE Luma",
                "VITURE Luma Pro",
                "VITURE Luma Ultra",
                "VITURE Pro",
                "VITURE Neckband"
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
                "Even G3",
                "Even-G3",
                "EvenG3",
                "Even G",
                "G1_",
                "G1-",
                "G2_",
                "G2-",
                "G3_",
                "G3-"
            ),
            // Even G1/G2 公式広告: G1_12_L / G2_XX_L / G2_XX_R
            nameRegexes = listOf(
                Regex("""(?i)\bG[123]_[0-9A-Za-z]+_[LR]\b""")
            ),
            // Even R1 はグラスではなくコントローラリング。CID 0x10F9 でも除外する。
            // contains("R1") は AIR1 / G1_R1_L 等に当たるため使わない。
            excludedNameRegexes = listOf(
                Regex("""(?i)^R1$"""),
                Regex("""(?i)\bEven(\s+Realities)?\s+R1\b""")
            )
        ),
        DetectionRule(
            manufacturerName = "Brilliant Labs",
            namePatterns = listOf(
                "Brilliant Labs",
                "Brilliant Frame",
                "Brilliant Halo",
                "Monocle",
                "Frame-",
                "Frame Update",
                "Halo-"
            ),
            // Halo / Frame 公式 BLE 仕様: "Halo XX" / "Frame XX"（XX は EUI-48 の第4バイト）
            nameRegexes = listOf(
                Regex("""(?i)\bHalo\s+[0-9A-Fa-f]{2}\b"""),
                Regex("""(?i)\bFrame\s+[0-9A-Fa-f]{2}\b""")
            ),
            serviceUuids = setOf("7A230001-5475-A6A4-654C-8431F6AD49C4"),
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
                "Mentra Mach1",
                "Mentra Display",
                "Mentra Nex",
                "Nex1-",
                "NexSim",
                "MENTRA_DISPLAY_",
                "mentra_live",
                "MENTRA_LIVE_BLE",
                "MENTRA_LIVE_BT",
                "XyBLE_",
                "XyBLE",
                "Xy_A",
                "NIMO"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Xingyi",
            namePatterns = listOf("AR99", "Xingyi"),
            payloadPatterns = listOf("AR99"),
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
            namePatterns = listOf("JINS MEME", "JINS-MEME", "MEME ES"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Thunderbird",
            namePatterns = listOf(
                "Thunderbird",
                "Thunderbird Glass",
                "Thunderbird Air"
            ),
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
            namePatterns = listOf(
                "RealWear",
                "Navigator-",
                "Navigator 500",
                "Navigator 520"
            ),
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
            namePatterns = listOf(
                "Honor Glass",
                "Honor Glasses",
                "荣耀眼镜",
                "荣耀智能眼镜",
                "荣耀AI眼镜"
            ),
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
            namePatterns = listOf(
                "Lucyd",
                "Lucyd Lyte",
                "Lucyd Glasses",
                // FCC 2BBYK-LCD008 / LCD011 のモデル番号。名前に Lucyd が無い場合の保険。
                "LCD008",
                "LCD010",
                "LCD011"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "MemoMind",
            namePatterns = listOf(
                "MemoMind",
                "Memo Mind",
                "XGIMI Memo"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Dymesty",
            namePatterns = listOf("Dymesty"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Chamelo",
            namePatterns = listOf("Chamelo"),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "OhO Sunshine",
            namePatterns = listOf(
                "OhO Sunshine",
                "OHO Sunshine",
                "OhO Edge",
                "OHO Edge",
                "OhO Brave",
                "OhO Globe"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "L'Atitude",
            namePatterns = listOf(
                "L'Atitude",
                "Latitude 52",
                "52°N"
            ),
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
        ),
        DetectionRule(
            manufacturerName = "Xiaodu",
            namePatterns = listOf(
                "Xiaodu Glass",
                "Xiaodu Glasses",
                "Xiaodu AI Glass",
                "小度眼镜",
                "小度AI眼镜",
                "小度智能眼镜",
                "XD-SSL0101"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Monako",
            namePatterns = listOf(
                "Monako Glass",
                "Monako Glasses",
                "Monako"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "iFLYTEK",
            // 録音ペンや翻訳機も同じ CID を使うため CID-only にはしない
            companyIds = setOf(0x0DF1),
            namePatterns = listOf(
                "iFLYTEK Glass",
                "iFLYTEK Glasses",
                "iFLYTEK AI Glass",
                "iFlytek Glass",
                "讯飞眼镜",
                "讯飞AI眼镜",
                "讯飞智能眼镜",
                "科大讯飞眼镜"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Quark",
            namePatterns = listOf(
                "Quark Glass",
                "Quark Glasses",
                "Quark AI Glass",
                "夸克眼镜",
                "夸克AI眼镜",
                "夸克智能眼镜"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Doubao",
            namePatterns = listOf(
                "Doubao Glass",
                "Doubao Glasses",
                "Doubao AI Glass",
                "豆包眼镜",
                "豆包AI眼镜",
                "豆包智能眼镜"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Everysight",
            namePatterns = listOf(
                "Everysight",
                "Everysight Raptor",
                "Everysight Maverick",
                "Maverick AI Pro"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "OpenGlass",
            // Friend/Omi ペンダントと同じ 19B10000 UUID は載せず、広告名だけ使う
            namePatterns = listOf(
                "OpenGlass",
                "Open Glass"
            ),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "XRAI",
            namePatterns = listOf(
                "XRAI Glass",
                "XRAI Glasses",
                "XRAI"
            ),
            allowCompanyIdOnly = false
        )
    )
}

enum class ScanSensitivity {
    LOW_POWER,
    BALANCED,
    HIGH_ACCURACY
}
