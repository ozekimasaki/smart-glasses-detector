package jp.smartglasses.detector.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_logs")
data class DiagnosticLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val advertisedName: String,
    val deviceAddress: String,
    val companyIds: String,
    val serviceUuids: String,
    val advertisementDataHex: String,
    val rssi: Int,
    val detectedAt: Long
)
