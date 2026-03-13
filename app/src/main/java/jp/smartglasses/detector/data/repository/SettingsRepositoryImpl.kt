package jp.smartglasses.detector.data.repository

import jp.smartglasses.detector.data.preferences.AppPreferences
import jp.smartglasses.detector.domain.repository.SettingsRepository
import jp.smartglasses.detector.util.ScanSensitivity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferences: AppPreferences
) : SettingsRepository {

    override val backgroundEnabled: Flow<Boolean> = preferences.backgroundEnabled
    override val notificationEnabled: Flow<Boolean> = preferences.notificationEnabled
    override val vibrationEnabled: Flow<Boolean> = preferences.vibrationEnabled
    override val soundEnabled: Flow<Boolean> = preferences.soundEnabled
    override val sensitivity: Flow<ScanSensitivity> = preferences.sensitivity
    override val onboardingCompleted: Flow<Boolean> = preferences.onboardingCompleted
    override val isScanning: Flow<Boolean> = preferences.isScanning

    override suspend fun setBackgroundEnabled(enabled: Boolean) =
        preferences.setBackgroundEnabled(enabled)

    override suspend fun setNotificationEnabled(enabled: Boolean) =
        preferences.setNotificationEnabled(enabled)

    override suspend fun setVibrationEnabled(enabled: Boolean) =
        preferences.setVibrationEnabled(enabled)

    override suspend fun setSoundEnabled(enabled: Boolean) =
        preferences.setSoundEnabled(enabled)

    override suspend fun setSensitivity(sensitivity: ScanSensitivity) =
        preferences.setSensitivity(sensitivity)

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        preferences.setOnboardingCompleted(completed)

    override suspend fun setIsScanning(scanning: Boolean) =
        preferences.setIsScanning(scanning)
}
