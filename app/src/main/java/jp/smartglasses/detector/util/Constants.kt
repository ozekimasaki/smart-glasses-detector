package jp.smartglasses.detector.util

internal data class DetectionRule(
    val manufacturerName: String,
    val companyIds: Set<Int> = emptySet(),
    val namePatterns: List<String> = emptyList(),
    val allowCompanyIdOnly: Boolean = true
)

object Constants {
    const val NOTIFICATION_CHANNEL_ID_SCANNING = "scanning_channel"
    const val NOTIFICATION_CHANNEL_ID_DETECTION = "detection_channel"
    const val NOTIFICATION_ID_SCANNING = 1001
    const val NOTIFICATION_ID_DETECTION = 1002

    internal const val MIN_DETECTION_RSSI_DBM = -75
    internal const val COOLDOWN_SAME_DEVICE_MS = 30_000L
    internal const val COOLDOWN_SAME_MANUFACTURER_MS = 15_000L

    internal val SMART_GLASSES_DETECTION_RULES = listOf(
        DetectionRule(
            manufacturerName = "Seiko Epson",
            companyIds = setOf(0x0040)
        ),
        DetectionRule(
            manufacturerName = "Apple",
            companyIds = setOf(0x004C),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Google",
            companyIds = setOf(0x00E0),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Amazon",
            companyIds = setOf(0x0171)
        ),
        DetectionRule(
            manufacturerName = "Google LLC",
            companyIds = setOf(0x018E),
            allowCompanyIdOnly = false
        ),
        DetectionRule(
            manufacturerName = "Meta Platforms",
            companyIds = setOf(0x01AB)
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
            companyIds = setOf(0x02C5)
        ),
        DetectionRule(
            manufacturerName = "Meizu",
            companyIds = setOf(0x03AB)
        ),
        DetectionRule(
            manufacturerName = "Snapchat",
            companyIds = setOf(0x03C2)
        ),
        DetectionRule(
            manufacturerName = "Meta Tech",
            companyIds = setOf(0x058E)
        ),
        DetectionRule(
            manufacturerName = "TCL",
            companyIds = setOf(0x0BC6)
        ),
        DetectionRule(
            manufacturerName = "Luxottica",
            companyIds = setOf(0x0D53)
        ),
        DetectionRule(
            manufacturerName = "XREAL",
            namePatterns = listOf("XREAL"),
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
        )
    )
}

enum class ScanSensitivity {
    LOW_POWER,
    BALANCED,
    HIGH_ACCURACY
}
