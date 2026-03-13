package jp.smartglasses.detector.domain.repository

import jp.smartglasses.detector.domain.model.DiagnosticLog
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import kotlinx.coroutines.flow.Flow

interface BluetoothRepository {
    val scannedDevices: Flow<SmartGlassesDevice>
    val diagnosticLogs: Flow<DiagnosticLog>
    val isScanning: Flow<Boolean>
    
    suspend fun startScanning()
    suspend fun stopScanning()
    fun hasPermissions(): Boolean
}
