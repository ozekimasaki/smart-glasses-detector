package jp.smartglasses.detector.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DetectionLogEntity::class, DiagnosticLogEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun detectionLogDao(): DetectionLogDao
    abstract fun diagnosticLogDao(): DiagnosticLogDao
}
