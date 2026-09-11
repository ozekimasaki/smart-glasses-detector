package jp.smartglasses.detector.domain.service

object ScanResumePolicy {
    fun shouldResume(
        wasScanning: Boolean,
        backgroundEnabled: Boolean,
        hasPermissions: Boolean
    ): Boolean {
        return wasScanning && backgroundEnabled && hasPermissions
    }

    fun shouldHandleAction(action: String?): Boolean {
        return action in HANDLED_ACTIONS
    }

    val HANDLED_ACTIONS = setOf(
        "android.intent.action.BOOT_COMPLETED",
        "android.intent.action.LOCKED_BOOT_COMPLETED",
        "android.intent.action.MY_PACKAGE_REPLACED",
        "android.intent.action.QUICKBOOT_POWERON",
        "com.htc.intent.action.QUICKBOOT_POWERON"
    )
}
