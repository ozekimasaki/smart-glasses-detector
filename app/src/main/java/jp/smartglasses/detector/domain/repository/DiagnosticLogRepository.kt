package jp.smartglasses.detector.domain.repository

import jp.smartglasses.detector.domain.model.DiagnosticLog

interface DiagnosticLogRepository {
    suspend fun insertLog(log: DiagnosticLog)
    suspend fun getLatestLogs(limit: Int): List<DiagnosticLog>
}
