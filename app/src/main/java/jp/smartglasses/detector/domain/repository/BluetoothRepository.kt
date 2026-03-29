package jp.smartglasses.detector.domain.repository

import jp.smartglasses.detector.domain.model.BluetoothScanFailure
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import kotlinx.coroutines.flow.Flow

interface BluetoothRepository {
    val scannedDevices: Flow<SmartGlassesDevice>
    val scanFailures: Flow<BluetoothScanFailure>
    val isScanning: Flow<Boolean>
    
    suspend fun startScanning()
    suspend fun stopScanning()
    fun hasPermissions(): Boolean
    fun hasBleHardwareSupport(): Boolean
    fun isBluetoothEnabled(): Boolean
    fun isLocationServicesEnabled(): Boolean
}
