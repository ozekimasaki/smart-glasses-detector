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

    fun shouldFlushAfterClassicDiscoveryFinished(
        action: String?,
        scanningRequested: Boolean
    ): Boolean {
        return scanningRequested &&
            action == ClassicDiscoveryPolicy.ACTION_DISCOVERY_FINISHED
    }
}
