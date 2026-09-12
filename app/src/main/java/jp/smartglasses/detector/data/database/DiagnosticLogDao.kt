package jp.smartglasses.detector.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DiagnosticLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DiagnosticLogEntity)

    @Query("SELECT * FROM diagnostic_logs ORDER BY detectedAt DESC LIMIT :limit")
    suspend fun getLatestLogs(limit: Int): List<DiagnosticLogEntity>

    @Query("SELECT COUNT(*) FROM diagnostic_logs")
    suspend fun count(): Int

    @Query(
        "DELETE FROM diagnostic_logs WHERE id IN " +
            "(SELECT id FROM diagnostic_logs ORDER BY detectedAt ASC LIMIT :count)"
    )
    suspend fun deleteOldest(count: Int)
}
