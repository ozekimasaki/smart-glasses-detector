package jp.smartglasses.detector.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detection_logs")
data class DetectionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceName: String,
    val deviceAddress: String,
    val manufacturerName: String,
    val rssi: Int,
    val distance: String,
    val detectedAt: Long
)
