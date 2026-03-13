package jp.smartglasses.detector.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.smartglasses.detector.domain.repository.SettingsRepository
import jp.smartglasses.detector.domain.usecase.UpdateSettingsUseCase
import jp.smartglasses.detector.util.ScanSensitivity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    settingsRepository: SettingsRepository
) : ViewModel() {
    
    val backgroundEnabled = settingsRepository.backgroundEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)
    
    val notificationEnabled = settingsRepository.notificationEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)
    
    val vibrationEnabled = settingsRepository.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)
    
    val soundEnabled = settingsRepository.soundEnabled
        .stateIn(viewModelScope, SharingStarted.Lazily, true)
    
    val sensitivity = settingsRepository.sensitivity
        .stateIn(viewModelScope, SharingStarted.Lazily, ScanSensitivity.BALANCED)
    
    fun setBackgroundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingsUseCase.setBackgroundEnabled(enabled)
        }
    }
    
    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingsUseCase.setNotificationEnabled(enabled)
        }
    }
    
    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingsUseCase.setVibrationEnabled(enabled)
        }
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingsUseCase.setSoundEnabled(enabled)
        }
    }
    
    fun setSensitivity(sensitivity: ScanSensitivity) {
        viewModelScope.launch {
            updateSettingsUseCase.setSensitivity(sensitivity)
        }
    }
}
