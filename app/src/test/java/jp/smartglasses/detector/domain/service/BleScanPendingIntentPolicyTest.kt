package jp.smartglasses.detector.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleScanPendingIntentPolicyTest {
    @Test
    fun `starts the surviving scan only while the user wants to scan`() {
        assertTrue(BleScanPendingIntentPolicy.shouldStart(scanningRequested = true))
        assertFalse(BleScanPendingIntentPolicy.shouldStart(scanningRequested = false))
        assertFalse(
            BleScanPendingIntentPolicy.shouldStart(
                scanningRequested = true,
                pendingIntentScanEnabled = false
            )
        )
    }

    @Test
    fun `restores the surviving scan on refresh unless this session already rejected it`() {
        assertTrue(
            BleScanPendingIntentPolicy.shouldRestoreOnRefresh(
                usingPendingIntentScan = false,
                pendingIntentScanRejectedThisSession = false
            )
        )
        assertFalse(
            BleScanPendingIntentPolicy.shouldRestoreOnRefresh(
                usingPendingIntentScan = true,
                pendingIntentScanRejectedThisSession = false
            )
        )
        assertFalse(
            BleScanPendingIntentPolicy.shouldRestoreOnRefresh(
                usingPendingIntentScan = false,
                pendingIntentScanRejectedThisSession = true
            )
        )
    }

    @Test
    fun `handles only the app-owned scan result action`() {
        assertTrue(
            BleScanPendingIntentPolicy.shouldHandleDelivery(
                BleScanPendingIntentPolicy.ACTION_SCAN_RESULTS
            )
        )
        assertFalse(BleScanPendingIntentPolicy.shouldHandleDelivery(null))
        assertFalse(
            BleScanPendingIntentPolicy.shouldHandleDelivery(
                ScanResumePolicy.ACTION_BLUETOOTH_STATE_CHANGED
            )
        )
        assertEquals(
            "jp.smartglasses.detector.action.BLE_SCAN_RESULTS",
            BleScanPendingIntentPolicy.ACTION_SCAN_RESULTS
        )
        assertEquals(
            "jp.smartglasses.detector.receiver.BleScanResultReceiver",
            BleScanPendingIntentPolicy.RECEIVER_CLASS_NAME
        )
        assertEquals(2001, BleScanPendingIntentPolicy.REQUEST_CODE)
        assertEquals(
            "android.bluetooth.le.extra.LIST_SCAN_RESULT",
            BleScanPendingIntentPolicy.EXTRA_LIST_SCAN_RESULT
        )
        assertEquals(
            "android.bluetooth.le.extra.ERROR_CODE",
            BleScanPendingIntentPolicy.EXTRA_ERROR_CODE
        )
    }

    @Test
    fun `ingests delivered advertisements when scanning was persisted`() {
        assertTrue(
            BleScanPendingIntentPolicy.shouldIngestResults(
                persistedScanning = true,
                errorCode = 0,
                resultCount = 1
            )
        )
        assertFalse(
            BleScanPendingIntentPolicy.shouldIngestResults(
                persistedScanning = false,
                errorCode = 0,
                resultCount = 1
            )
        )
        assertFalse(
            BleScanPendingIntentPolicy.shouldIngestResults(
                persistedScanning = true,
                errorCode = 0,
                resultCount = 0
            )
        )
        assertFalse(
            BleScanPendingIntentPolicy.shouldIngestResults(
                persistedScanning = true,
                errorCode = ScanFailurePolicy.SCAN_FAILED_INTERNAL_ERROR,
                resultCount = 1
            )
        )
    }

    @Test
    fun `reports pending-intent scan failures so hardware can be restarted`() {
        assertTrue(
            BleScanPendingIntentPolicy.shouldReportFailure(
                persistedScanning = true,
                errorCode = ScanFailurePolicy.SCAN_FAILED_INTERNAL_ERROR
            )
        )
        assertFalse(
            BleScanPendingIntentPolicy.shouldReportFailure(
                persistedScanning = false,
                errorCode = ScanFailurePolicy.SCAN_FAILED_INTERNAL_ERROR
            )
        )
        assertFalse(
            BleScanPendingIntentPolicy.shouldReportFailure(
                persistedScanning = true,
                errorCode = 0
            )
        )
    }

    @Test
    fun `resumes the foreground service when a surviving scan wakes a dead process`() {
        assertTrue(
            BleScanPendingIntentPolicy.shouldResumeServiceAfterDelivery(
                persistedScanning = true,
                hardwareScanRunning = false
            )
        )
        assertFalse(
            BleScanPendingIntentPolicy.shouldResumeServiceAfterDelivery(
                persistedScanning = true,
                hardwareScanRunning = true
            )
        )
        assertFalse(
            BleScanPendingIntentPolicy.shouldResumeServiceAfterDelivery(
                persistedScanning = false,
                hardwareScanRunning = false
            )
        )
    }
}
