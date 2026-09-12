package jp.smartglasses.detector.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.smartglasses.detector.R
import jp.smartglasses.detector.domain.repository.SettingsRepository
import jp.smartglasses.detector.domain.usecase.ClearStoredDetectionDataUseCase
import jp.smartglasses.detector.domain.usecase.UpdateSettingsUseCase
import jp.smartglasses.detector.util.ScanSensitivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsEvent {
    data class ShowMessage(val messageResId: Int) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val clearStoredDetectionDataUseCase: ClearStoredDetectionDataUseCase,
    settingsRepository: SettingsRepository
) : ViewModel() {
    private val _event = Channel<SettingsEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    val backgroundEnabled = settingsRepository.backgroundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val notificationEnabled = settingsRepository.notificationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val vibrationEnabled = settingsRepository.vibrationEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val soundEnabled = settingsRepository.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val sensitivity = settingsRepository.sensitivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanSensitivity.BALANCED)

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

    fun clearStoredDetectionData() {
        viewModelScope.launch {
            try {
                clearStoredDetectionDataUseCase()
                _event.send(SettingsEvent.ShowMessage(R.string.settings_clear_data_done))
            } catch (_: Exception) {
                _event.send(SettingsEvent.ShowMessage(R.string.settings_clear_data_failed))
            }
        }
    }
}
