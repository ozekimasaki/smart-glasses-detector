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
}
