package jp.smartglasses.detector.domain.usecase

import jp.smartglasses.detector.domain.model.DetectionLog
import jp.smartglasses.detector.domain.model.DiagnosticLog
import jp.smartglasses.detector.domain.repository.DetectionLogRepository
import jp.smartglasses.detector.domain.repository.DiagnosticLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ClearStoredDetectionDataUseCaseTest {
    @Test
    fun `clears detection history and diagnostic logs`() = runBlocking {
        val detectionLogs = RecordingDetectionLogRepository()
        val diagnosticLogs = RecordingDiagnosticLogRepository()
        val useCase = ClearStoredDetectionDataUseCase(
            detectionLogRepository = detectionLogs,
            diagnosticLogRepository = diagnosticLogs
        )

        useCase()

        assertEquals(1, detectionLogs.deleteAllCount)
        assertEquals(1, diagnosticLogs.deleteAllCount)
    }

    private class RecordingDetectionLogRepository : DetectionLogRepository {
        var deleteAllCount = 0

        override fun getAllLogs(): Flow<List<DetectionLog>> = emptyFlow()
        override fun getLogsForDate(startTime: Long, endTime: Long): Flow<List<DetectionLog>> = emptyFlow()
        override fun observeLatestLogs(limit: Int): Flow<List<DetectionLog>> = emptyFlow()
        override fun observeTodayCount(startOfDay: Long): Flow<Int> = emptyFlow()
        override suspend fun getLatestLogs(limit: Int): List<DetectionLog> = emptyList()
        override suspend fun insertLog(log: DetectionLog) = Unit
        override suspend fun deleteOldLogs(before: Long) = Unit
        override suspend fun deleteAllLogs() {
            deleteAllCount += 1
        }
        override suspend fun getTodayCount(): Int = 0
    }

    private class RecordingDiagnosticLogRepository : DiagnosticLogRepository {
        var deleteAllCount = 0

        override suspend fun insertLog(log: DiagnosticLog) = Unit
        override suspend fun getLatestLogs(limit: Int): List<DiagnosticLog> = emptyList()
        override suspend fun deleteAllLogs() {
            deleteAllCount += 1
        }
    }
}
