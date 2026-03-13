package jp.smartglasses.detector.data.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.smartglasses.detector.domain.model.DiagnosticLog
import jp.smartglasses.detector.data.preferences.AppPreferences
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.domain.repository.BluetoothRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smartGlassesDetector: SmartGlassesDetector,
    private val preferences: AppPreferences
) : BluetoothRepository {

    override val scannedDevices: Flow<SmartGlassesDevice>
        get() = smartGlassesDetector.scannedDevices

    override val diagnosticLogs: Flow<DiagnosticLog>
        get() = smartGlassesDetector.diagnosticLogs
    
    override val isScanning: Flow<Boolean>
        get() = smartGlassesDetector.isScanning
    
    override suspend fun startScanning() {
        val sensitivity = preferences.sensitivity.first()
        smartGlassesDetector.startScanning(sensitivity)
    }
    
    override suspend fun stopScanning() {
        smartGlassesDetector.stopScanning()
    }
    
    override fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
