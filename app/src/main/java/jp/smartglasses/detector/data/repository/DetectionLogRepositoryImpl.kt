package jp.smartglasses.detector.data.repository

import jp.smartglasses.detector.data.database.DetectionLogDao
import jp.smartglasses.detector.data.database.DetectionLogEntity
import jp.smartglasses.detector.domain.model.DetectionLog
import jp.smartglasses.detector.domain.repository.DetectionLogRepository
import jp.smartglasses.detector.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetectionLogRepositoryImpl @Inject constructor(
    private val dao: DetectionLogDao
) : DetectionLogRepository {
    
    override fun getAllLogs(): Flow<List<DetectionLog>> {
        return dao.getAllLogs().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getLogsForDate(startTime: Long, endTime: Long): Flow<List<DetectionLog>> {
        return dao.getLogsForDate(startTime, endTime).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeLatestLogs(limit: Int): Flow<List<DetectionLog>> {
        return dao.observeLatestLogs(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeTodayCount(startOfDay: Long): Flow<Int> {
        return dao.observeTodayCount(startOfDay)
    }

    override suspend fun getLatestLogs(limit: Int): List<DetectionLog> {
        return dao.getLatestLogs(limit).map { it.toDomain() }
    }
    
    override suspend fun insertLog(log: DetectionLog) {
        dao.insertAndTrim(log.toEntity(), Constants.DETECTION_LOG_KEEP_COUNT)
    }
    
    override suspend fun deleteOldLogs(before: Long) {
        dao.deleteOldLogs(before)
    }
    
    override suspend fun deleteAllLogs() {
        dao.deleteAllLogs()
    }
    
    override suspend fun getTodayCount(): Int {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dao.getTodayCount(calendar.timeInMillis)
    }
    
    private fun DetectionLogEntity.toDomain() = DetectionLog(
        id = id,
        deviceName = deviceName,
        deviceAddress = deviceAddress,
        manufacturerName = manufacturerName,
        rssi = rssi,
        distance = distance,
        detectedAt = detectedAt
    )
    
    private fun DetectionLog.toEntity() = DetectionLogEntity(
        id = id,
        deviceName = deviceName,
        deviceAddress = deviceAddress,
        manufacturerName = manufacturerName,
        rssi = rssi,
        distance = distance,
        detectedAt = detectedAt
    )
}
