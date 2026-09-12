package jp.smartglasses.detector.domain.service

/**
 * OEM が ScanCallback をバッチしているときに、待たずに結果を出す。
 */
object BleScanFlushPolicy {
    fun shouldFlushPendingResults(
        scanningRequested: Boolean,
        hardwareScanRunning: Boolean,
        bluetoothEnabled: Boolean,
        scanPermissionGranted: Boolean
    ): Boolean {
        return scanningRequested &&
            hardwareScanRunning &&
            bluetoothEnabled &&
            scanPermissionGranted
    }
}
