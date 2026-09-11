package jp.smartglasses.detector.domain.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicDiscoveryPolicyTest {
    @Test
    fun `classic discovery runs once per scanning session`() {
        assertTrue(ClassicDiscoveryPolicy.shouldStartClassicDiscovery(alreadyStartedThisSession = false))
        assertFalse(ClassicDiscoveryPolicy.shouldStartClassicDiscovery(alreadyStartedThisSession = true))
    }
}
