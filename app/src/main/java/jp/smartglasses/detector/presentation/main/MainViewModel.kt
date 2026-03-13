package jp.smartglasses.detector.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.smartglasses.detector.domain.repository.BluetoothRepository
import jp.smartglasses.detector.domain.repository.DetectionLogRepository
import jp.smartglasses.detector.domain.repository.SettingsRepository
import jp.smartglasses.detector.domain.service.ScanServiceController
import jp.smartglasses.detector.util.BackgroundScanSupport
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MainUiState {
    object Idle : MainUiState()
    object Scanning : MainUiState()
}

sealed interface MainEvent {
    data class ShowMessage(val message: String) : MainEvent
    data object OpenAppSettings : MainEvent
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val scanServiceController: ScanServiceController,
    private val detectionLogRepository: DetectionLogRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {
    private val _event = Channel<MainEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    val isScanning = bluetoothRepository.isScanning
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val uiState = bluetoothRepository.isScanning
        .map { scanning -> if (scanning) MainUiState.Scanning else MainUiState.Idle }
        .stateIn(viewModelScope, SharingStarted.Lazily, MainUiState.Idle)

    val todayCount = detectionLogRepository.getAllLogs()
        .map { logs ->
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            logs.count { log -> log.detectedAt >= startOfDay }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val backgroundScanningEnabled = settingsRepository.backgroundEnabled
        .map { BackgroundScanSupport.isEnabled(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun toggleScanning() {
        if (isScanning.value) {
            stopScanning()
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        viewModelScope.launch {
            if (!bluetoothRepository.hasPermissions()) {
                _event.send(MainEvent.OpenAppSettings)
                return@launch
            }

            try {
                scanServiceController.startScanService()
            } catch (_: Exception) {
                _event.send(MainEvent.ShowMessage("探索を開始できませんでした。もう一度お試しください。"))
            }
        }
    }

    private fun stopScanning() {
        viewModelScope.launch {
            try {
                scanServiceController.stopScanService()
            } catch (_: Exception) {
                _event.send(MainEvent.ShowMessage("探索の停止に失敗しました。"))
            }
        }
    }
}
