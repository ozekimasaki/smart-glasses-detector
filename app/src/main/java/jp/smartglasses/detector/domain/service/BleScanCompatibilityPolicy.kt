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
        // Android 8+ はバックグラウンドの無フィルタスキャンを止める。
        // 空の match-all フィルタはその回避策なので、先に拡張広告を切る。
        return when {
            usingExtendedAdvertising -> BleScanCompatibilityStep.DISABLE_EXTENDED_ADVERTISING
            usingMatchAllFilter -> BleScanCompatibilityStep.DROP_MATCH_ALL_FILTER
            else -> BleScanCompatibilityStep.NONE
        }
    }

    fun shouldRestoreMatchAllFilterOnRefresh(usingMatchAllFilter: Boolean): Boolean {
        return !usingMatchAllFilter
    }
}
