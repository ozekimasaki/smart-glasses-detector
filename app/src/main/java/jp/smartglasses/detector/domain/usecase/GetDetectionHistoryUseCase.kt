package jp.smartglasses.detector.domain.usecase

import jp.smartglasses.detector.domain.model.DetectionLog
import jp.smartglasses.detector.domain.repository.DetectionLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDetectionHistoryUseCase @Inject constructor(
    private val repository: DetectionLogRepository
) {
    operator fun invoke(): Flow<List<DetectionLog>> {
        return repository.getAllLogs()
    }
}
