package jp.smartglasses.detector.domain.service

object HardwareScanStatePolicy {
    fun isActive(
        userRequestedScanning: Boolean,
        bluetoothEnabled: Boolean,
        scanPermissionGranted: Boolean = true
    ): Boolean {
        return userRequestedScanning && bluetoothEnabled && scanPermissionGranted
    }
}
