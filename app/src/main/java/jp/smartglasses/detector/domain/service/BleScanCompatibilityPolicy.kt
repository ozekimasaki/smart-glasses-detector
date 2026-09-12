package jp.smartglasses.detector.domain.service

enum class BleScanCompatibilityStep {
    DROP_PENDING_INTENT_SCAN,
    DROP_MATCH_ALL_FILTER,
    DISABLE_EXTENDED_ADVERTISING,
    NONE
}

object BleScanCompatibilityPolicy {
    fun nextStep(
        usingMatchAllFilter: Boolean,
        usingExtendedAdvertising: Boolean,
        usingPendingIntentScan: Boolean = false,
        errorCode: Int = ScanFailurePolicy.SCAN_FAILED_FEATURE_UNSUPPORTED
    ): BleScanCompatibilityStep {
        if (
            usingPendingIntentScan &&
            ScanFailurePolicy.shouldDropPendingIntentScan(errorCode)
        ) {
            return BleScanCompatibilityStep.DROP_PENDING_INTENT_SCAN
        }
        if (!ScanFailurePolicy.shouldTryCompatibilityFallback(errorCode)) {
            return BleScanCompatibilityStep.NONE
        }
        // Android 8+ はバックグラウンドの無フィルタスキャンを止める。
        // 空の match-all フィルタはその回避策なので、先に拡張広告を切る。
        return when {
            usingExtendedAdvertising -> BleScanCompatibilityStep.DISABLE_EXTENDED_ADVERTISING
            usingMatchAllFilter -> BleScanCompatibilityStep.DROP_MATCH_ALL_FILTER
            else -> BleScanCompatibilityStep.NONE
        }
    }

    fun shouldRestoreMatchAllFilterOnRefresh(
        usingMatchAllFilter: Boolean,
        matchAllRejectedThisSession: Boolean = false
    ): Boolean {
        return !usingMatchAllFilter && !matchAllRejectedThisSession
    }
}
