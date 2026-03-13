package jp.smartglasses.detector.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DetectionLogDao {
    @Query("SELECT * FROM detection_logs ORDER BY detectedAt DESC")
    fun getAllLogs(): Flow<List<DetectionLogEntity>>
    
    @Query("SELECT * FROM detection_logs WHERE detectedAt >= :startTime AND detectedAt < :endTime ORDER BY detectedAt DESC")
    fun getLogsForDate(startTime: Long, endTime: Long): Flow<List<DetectionLogEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DetectionLogEntity)
    
    @Query("DELETE FROM detection_logs WHERE detectedAt < :before")
    suspend fun deleteOldLogs(before: Long)
    
    @Query("DELETE FROM detection_logs")
    suspend fun deleteAllLogs()
    
    @Query("SELECT COUNT(*) FROM detection_logs WHERE detectedAt >= :startOfDay")
    suspend fun getTodayCount(startOfDay: Long): Int
}
