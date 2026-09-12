package jp.smartglasses.detector.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test

class BleScanCompatibilityPolicyTest {
    @Test
    fun `drops the match-all filter before disabling extended advertising`() {
        assertEquals(
            BleScanCompatibilityStep.DROP_MATCH_ALL_FILTER,
            BleScanCompatibilityPolicy.nextStep(
                usingMatchAllFilter = true,
                usingExtendedAdvertising = true
            )
        )
        assertEquals(
            BleScanCompatibilityStep.DISABLE_EXTENDED_ADVERTISING,
            BleScanCompatibilityPolicy.nextStep(
                usingMatchAllFilter = false,
                usingExtendedAdvertising = true
            )
        )
        assertEquals(
            BleScanCompatibilityStep.NONE,
            BleScanCompatibilityPolicy.nextStep(
                usingMatchAllFilter = false,
                usingExtendedAdvertising = false
            )
        )
    }
}
