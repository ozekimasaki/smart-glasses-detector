package jp.smartglasses.detector.domain.usecase

import jp.smartglasses.detector.domain.repository.DetectionLogRepository
import jp.smartglasses.detector.domain.repository.DiagnosticLogRepository
import javax.inject.Inject

class ClearStoredDetectionDataUseCase @Inject constructor(
    private val detectionLogRepository: DetectionLogRepository,
    private val diagnosticLogRepository: DiagnosticLogRepository
) {
    suspend operator fun invoke() {
        detectionLogRepository.deleteAllLogs()
        diagnosticLogRepository.deleteAllLogs()
    }
}
