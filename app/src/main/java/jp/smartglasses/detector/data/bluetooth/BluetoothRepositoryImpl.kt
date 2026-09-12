package jp.smartglasses.detector.data.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.smartglasses.detector.domain.model.BluetoothScanFailure
import jp.smartglasses.detector.data.preferences.AppPreferences
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.domain.repository.BluetoothRepository
import jp.smartglasses.detector.domain.service.ForegroundScanBoostPolicy
import jp.smartglasses.detector.util.ScanSensitivity
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

    override val scanFailures: Flow<BluetoothScanFailure>
        get() = smartGlassesDetector.scanFailures
    
    override val isScanning: Flow<Boolean>
        get() = smartGlassesDetector.isScanning

    override val isHardwareScanRunning: Flow<Boolean>
        get() = smartGlassesDetector.isHardwareScanRunning

    override val nearbyDevices: Flow<List<SmartGlassesDevice>>
        get() = smartGlassesDetector.nearbyDevices
    
    override suspend fun startScanning() {
        val sensitivity = ForegroundScanBoostPolicy.effectiveSensitivity(
            userSensitivity = preferences.sensitivity.first(),
            appInForeground = isAppInForeground()
        )
        smartGlassesDetector.startScanning(sensitivity)
    }

    override fun ensureHardwareScanning() {
        smartGlassesDetector.ensureHardwareScanning()
    }
    
    override suspend fun stopScanning() {
        smartGlassesDetector.stopScanning()
    }

    override fun updateScanSensitivity(sensitivity: ScanSensitivity) {
        smartGlassesDetector.updateSensitivity(
            ForegroundScanBoostPolicy.effectiveSensitivity(
                userSensitivity = sensitivity,
                appInForeground = isAppInForeground()
            )
        )
    }
    
    override fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun hasBleHardwareSupport(): Boolean {
        return smartGlassesDetector.hasBleHardwareSupport()
    }

    override fun isBluetoothEnabled(): Boolean {
        return smartGlassesDetector.isBluetoothEnabled()
    }

    override fun isLocationServicesEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return true
        }

        val locationManager = context.getSystemService(LocationManager::class.java) ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }
}
