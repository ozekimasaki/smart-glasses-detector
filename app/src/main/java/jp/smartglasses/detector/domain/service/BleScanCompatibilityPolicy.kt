package jp.smartglasses.detector.domain.service

enum class BleScanCompatibilityStep {
    DROP_MATCH_ALL_FILTER,
    DISABLE_EXTENDED_ADVERTISING,
    NONE
}

object BleScanCompatibilityPolicy {
    fun nextStep(
        usingMatchAllFilter: Boolean,
        usingExtendedAdvertising: Boolean
    ): BleScanCompatibilityStep {
        return when {
            usingMatchAllFilter -> BleScanCompatibilityStep.DROP_MATCH_ALL_FILTER
            usingExtendedAdvertising -> BleScanCompatibilityStep.DISABLE_EXTENDED_ADVERTISING
            else -> BleScanCompatibilityStep.NONE
        }
    }
}
