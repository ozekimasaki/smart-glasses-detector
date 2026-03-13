package jp.smartglasses.detector.domain.repository

import jp.smartglasses.detector.util.ScanSensitivity
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val backgroundEnabled: Flow<Boolean>
    val notificationEnabled: Flow<Boolean>
    val vibrationEnabled: Flow<Boolean>
    val soundEnabled: Flow<Boolean>
    val sensitivity: Flow<ScanSensitivity>
    val onboardingCompleted: Flow<Boolean>
    val isScanning: Flow<Boolean>

    suspend fun setBackgroundEnabled(enabled: Boolean)
    suspend fun setNotificationEnabled(enabled: Boolean)
    suspend fun setVibrationEnabled(enabled: Boolean)
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun setSensitivity(sensitivity: ScanSensitivity)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setIsScanning(scanning: Boolean)
}
