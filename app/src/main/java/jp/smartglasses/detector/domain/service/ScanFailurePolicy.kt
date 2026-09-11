package jp.smartglasses.detector.domain.service

object ScanFailurePolicy {
    const val SCAN_FAILED_ALREADY_STARTED = 1
    const val SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 2
    const val SCAN_FAILED_INTERNAL_ERROR = 3
    const val SCAN_FAILED_FEATURE_UNSUPPORTED = 4
    const val SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES = 5
    const val SCAN_FAILED_SCANNING_TOO_FREQUENTLY = 6

    fun shouldIgnore(errorCode: Int): Boolean {
        return errorCode == SCAN_FAILED_ALREADY_STARTED
    }

    fun isRecoverable(errorCode: Int): Boolean {
        return errorCode == SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ||
            errorCode == SCAN_FAILED_INTERNAL_ERROR ||
            errorCode == SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES ||
            errorCode == SCAN_FAILED_SCANNING_TOO_FREQUENTLY
    }

    fun shouldFallbackToLegacy(errorCode: Int): Boolean {
        return errorCode == SCAN_FAILED_FEATURE_UNSUPPORTED
    }

    fun shouldKeepScanning(errorCode: Int): Boolean {
        return shouldIgnore(errorCode) ||
            isRecoverable(errorCode) ||
            shouldFallbackToLegacy(errorCode)
    }

    fun retryDelayMs(attempt: Int): Long {
        val boundedAttempt = attempt.coerceIn(1, 6)
        return (1_000L * (1 shl (boundedAttempt - 1))).coerceAtMost(30_000L)
    }
}
