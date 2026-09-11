package jp.smartglasses.detector.domain.service

object ClassicDiscoveryPolicy {
    fun shouldStartClassicDiscovery(alreadyStartedThisSession: Boolean): Boolean {
        return !alreadyStartedThisSession
    }
}
