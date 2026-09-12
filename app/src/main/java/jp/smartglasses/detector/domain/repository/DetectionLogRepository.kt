package jp.smartglasses.detector.domain.repository

import jp.smartglasses.detector.domain.model.DetectionLog
import kotlinx.coroutines.flow.Flow

interface DetectionLogRepository {
    fun getAllLogs(): Flow<List<DetectionLog>>
    fun getLogsForDate(startTime: Long, endTime: Long): Flow<List<DetectionLog>>
    fun observeLatestLogs(limit: Int): Flow<List<DetectionLog>>
    fun observeTodayCount(startOfDay: Long): Flow<Int>
    suspend fun getLatestLogs(limit: Int): List<DetectionLog>
    suspend fun insertLog(log: DetectionLog)
    suspend fun deleteOldLogs(before: Long)
    suspend fun deleteAllLogs()
    suspend fun getTodayCount(): Int
}
