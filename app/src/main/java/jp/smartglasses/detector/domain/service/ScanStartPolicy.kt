package jp.smartglasses.detector.domain.service

sealed interface ScanStartRequirement {
    data object Ready : ScanStartRequirement
    data object MissingBleHardware : ScanStartRequirement
    data object BluetoothDisabled : ScanStartRequirement
    data object MissingScanPermissions : ScanStartRequirement
    data object LocationDisabled : ScanStartRequirement
    data object NotificationPermissionNeeded : ScanStartRequirement
}

object ScanStartPolicy {
    fun evaluate(
        hasBleHardware: Boolean,
        bluetoothEnabled: Boolean,
        hasScanPermissions: Boolean,
        requiresLocationServices: Boolean,
        locationServicesEnabled: Boolean,
        hasNotificationPermission: Boolean,
        notificationPrompted: Boolean
    ): ScanStartRequirement {
        if (!hasBleHardware) {
            return ScanStartRequirement.MissingBleHardware
        }
        if (!bluetoothEnabled) {
            return ScanStartRequirement.BluetoothDisabled
        }
        if (!hasScanPermissions) {
            return ScanStartRequirement.MissingScanPermissions
        }
        if (requiresLocationServices && !locationServicesEnabled) {
            return ScanStartRequirement.LocationDisabled
        }
        if (!hasNotificationPermission && !notificationPrompted) {
            return ScanStartRequirement.NotificationPermissionNeeded
        }
        return ScanStartRequirement.Ready
    }
}

object ScanUiStatePolicy {
    fun isScanning(persistedIntent: Boolean, hardwareScanning: Boolean): Boolean {
        return persistedIntent || hardwareScanning
    }

    fun restorePrompt(
        persistedIntent: Boolean,
        hasScanPermissions: Boolean,
        requiresLocationServices: Boolean,
        locationServicesEnabled: Boolean,
        bluetoothEnabled: Boolean = true,
        hardwareScanning: Boolean = true
    ): ScanRestorePrompt {
        if (!persistedIntent) {
            return ScanRestorePrompt.None
        }
        if (!hasScanPermissions) {
            return ScanRestorePrompt.ScanPermission
        }
        if (!bluetoothEnabled) {
            return ScanRestorePrompt.Bluetooth
        }
        if (requiresLocationServices && !locationServicesEnabled) {
            return ScanRestorePrompt.Location
        }
        if (!hardwareScanning) {
            return ScanRestorePrompt.Hardware
        }
        return ScanRestorePrompt.None
    }

    const val HARDWARE_RESTORE_PROMPT_DELAY_MS = 8_000L
}

enum class ScanRestorePrompt {
    None,
    ScanPermission,
    Bluetooth,
    Location,
    Hardware
}

object BackgroundScanRuntimePolicy {
    fun shouldStopService(backgroundEnabled: Boolean, appInForeground: Boolean): Boolean {
        return !backgroundEnabled && !appInForeground
    }
}
