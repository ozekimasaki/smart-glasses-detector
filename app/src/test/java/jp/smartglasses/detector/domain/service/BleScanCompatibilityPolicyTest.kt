package jp.smartglasses.detector.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BleScanCompatibilityPolicyTest {
    @Test
    fun `disables extended advertising before dropping the match-all filter`() {
        assertEquals(
            BleScanCompatibilityStep.DISABLE_EXTENDED_ADVERTISING,
            BleScanCompatibilityPolicy.nextStep(
                usingMatchAllFilter = true,
                usingExtendedAdvertising = true
            )
        )
        assertEquals(
            BleScanCompatibilityStep.DROP_MATCH_ALL_FILTER,
            BleScanCompatibilityPolicy.nextStep(
                usingMatchAllFilter = true,
                usingExtendedAdvertising = false
            )
        )
        assertEquals(
            BleScanCompatibilityStep.NONE,
            BleScanCompatibilityPolicy.nextStep(
                usingMatchAllFilter = false,
                usingExtendedAdvertising = false
            )
        )
        assertEquals(
            BleScanCompatibilityStep.DISABLE_EXTENDED_ADVERTISING,
            BleScanCompatibilityPolicy.nextStep(
                usingMatchAllFilter = false,
                usingExtendedAdvertising = true
            )
        )
    }

    @Test
    fun `drops the surviving scan before disabling extended advertising`() {
        assertEquals(
            BleScanCompatibilityStep.DROP_PENDING_INTENT_SCAN,
            BleScanCompatibilityPolicy.nextStep(
                usingMatchAllFilter = true,
                usingExtendedAdvertising = true,
                usingPendingIntentScan = true,
                errorCode = ScanFailurePolicy.SCAN_FAILED_SCANNING_TOO_FREQUENTLY
            )
        )
        assertEquals(
            BleScanCompatibilityStep.DROP_PENDING_INTENT_SCAN,
            BleScanCompatibilityPolicy.nextStep(
                usingMatchAllFilter = true,
                usingExtendedAdvertising = true,
                usingPendingIntentScan = true,
                errorCode = ScanFailurePolicy.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES
            )
        )
        assertEquals(
            BleScanCompatibilityStep.DROP_PENDING_INTENT_SCAN,
            BleScanCompatibilityPolicy.nextStep(
                usingMatchAllFilter = true,
                usingExtendedAdvertising = true,
                usingPendingIntentScan = true,
                errorCode = ScanFailurePolicy.SCAN_FAILED_INTERNAL_ERROR
            )
        )
        assertEquals(
            BleScanCompatibilityStep.DISABLE_EXTENDED_ADVERTISING,
            BleScanCompatibilityPolicy.nextStep(
                usingMatchAllFilter = true,
                usingExtendedAdvertising = true,
                usingPendingIntentScan = true,
                errorCode = ScanFailurePolicy.SCAN_FAILED_FEATURE_UNSUPPORTED
            )
        )
        assertEquals(
            BleScanCompatibilityStep.NONE,
            BleScanCompatibilityPolicy.nextStep(
                usingMatchAllFilter = true,
                usingExtendedAdvertising = true,
                usingPendingIntentScan = false,
                errorCode = ScanFailurePolicy.SCAN_FAILED_SCANNING_TOO_FREQUENTLY
            )
        )
    }

    @Test
    fun `restores the match-all filter on the next scheduled refresh`() {
        assertTrue(
            BleScanCompatibilityPolicy.shouldRestoreMatchAllFilterOnRefresh(
                usingMatchAllFilter = false
            )
        )
        assertFalse(
            BleScanCompatibilityPolicy.shouldRestoreMatchAllFilterOnRefresh(
                usingMatchAllFilter = true
            )
        )
    }

    @Test
    fun `does not restore a match-all filter that this session already rejected`() {
        assertFalse(
            BleScanCompatibilityPolicy.shouldRestoreMatchAllFilterOnRefresh(
                usingMatchAllFilter = false,
                matchAllRejectedThisSession = true
            )
        )
        assertTrue(
            BleScanCompatibilityPolicy.shouldRestoreMatchAllFilterOnRefresh(
                usingMatchAllFilter = false,
                matchAllRejectedThisSession = false
            )
        )
    }
}
