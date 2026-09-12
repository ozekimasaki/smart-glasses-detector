package jp.smartglasses.detector.domain.service

/**
 * プロセスが殺されても BLE 広告を拾うための PendingIntent スキャン方針。
 *
 * Android の [android.bluetooth.le.BluetoothLeScanner] extra 名と一致させる。
 */
object BleScanPendingIntentPolicy {
    const val ACTION_SCAN_RESULTS = "jp.smartglasses.detector.action.BLE_SCAN_RESULTS"
    const val RECEIVER_CLASS_NAME = "jp.smartglasses.detector.receiver.BleScanResultReceiver"
    const val REQUEST_CODE = 2001
    const val EXTRA_LIST_SCAN_RESULT = "android.bluetooth.le.extra.LIST_SCAN_RESULT"
    const val EXTRA_ERROR_CODE = "android.bluetooth.le.extra.ERROR_CODE"
    const val DEFAULT_ERROR_CODE = 0

    fun shouldStart(scanningRequested: Boolean): Boolean {
        return scanningRequested
    }

    fun shouldHandleDelivery(action: String?): Boolean {
        return action == ACTION_SCAN_RESULTS
    }

    fun shouldIngestResults(
        persistedScanning: Boolean,
        errorCode: Int,
        resultCount: Int
    ): Boolean {
        return persistedScanning &&
            errorCode <= DEFAULT_ERROR_CODE &&
            resultCount > 0
    }

    fun shouldReportFailure(persistedScanning: Boolean, errorCode: Int): Boolean {
        return persistedScanning && errorCode > DEFAULT_ERROR_CODE
    }

    fun shouldResumeServiceAfterDelivery(
        persistedScanning: Boolean,
        hardwareScanRunning: Boolean
    ): Boolean {
        return persistedScanning && !hardwareScanRunning
    }
}
