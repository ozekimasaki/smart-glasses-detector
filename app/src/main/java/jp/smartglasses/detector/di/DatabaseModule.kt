package jp.smartglasses.detector.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jp.smartglasses.detector.data.database.AppDatabase
import jp.smartglasses.detector.data.database.DetectionLogDao
import jp.smartglasses.detector.data.database.DiagnosticLogDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "smart_glasses_detector_db"
        ).addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideDetectionLogDao(database: AppDatabase): DetectionLogDao {
        return database.detectionLogDao()
    }

    @Provides
    fun provideDiagnosticLogDao(database: AppDatabase): DiagnosticLogDao {
        return database.diagnosticLogDao()
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS diagnostic_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    advertisedName TEXT NOT NULL,
                    deviceAddress TEXT NOT NULL,
                    companyIds TEXT NOT NULL,
                    serviceUuids TEXT NOT NULL,
                    advertisementDataHex TEXT NOT NULL,
                    rssi INTEGER NOT NULL,
                    detectedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }
}
