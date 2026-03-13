package jp.smartglasses.detector.domain.usecase

import jp.smartglasses.detector.domain.repository.SettingsRepository
import jp.smartglasses.detector.util.ScanSensitivity
import javax.inject.Inject

class UpdateSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend fun setBackgroundEnabled(enabled: Boolean) {
        settingsRepository.setBackgroundEnabled(enabled)
    }
    
    suspend fun setNotificationEnabled(enabled: Boolean) {
        settingsRepository.setNotificationEnabled(enabled)
    }
    
    suspend fun setVibrationEnabled(enabled: Boolean) {
        settingsRepository.setVibrationEnabled(enabled)
    }
    
    suspend fun setSoundEnabled(enabled: Boolean) {
        settingsRepository.setSoundEnabled(enabled)
    }
    
    suspend fun setSensitivity(sensitivity: ScanSensitivity) {
        settingsRepository.setSensitivity(sensitivity)
    }
}
