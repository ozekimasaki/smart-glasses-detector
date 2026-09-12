package jp.smartglasses.detector.presentation.main

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.smartglasses.detector.R
import jp.smartglasses.detector.domain.repository.BluetoothRepository
import jp.smartglasses.detector.domain.repository.DetectionLogRepository
import jp.smartglasses.detector.domain.repository.SettingsRepository
import jp.smartglasses.detector.domain.service.ScanRestorePrompt
import jp.smartglasses.detector.domain.service.ScanStartPolicy
import jp.smartglasses.detector.domain.service.ScanStartRequirement
import jp.smartglasses.detector.domain.service.ScanUiStatePolicy
import jp.smartglasses.detector.domain.usecase.StartScanningUseCase
import jp.smartglasses.detector.domain.usecase.StopScanningUseCase
import jp.smartglasses.detector.util.BackgroundScanSupport
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
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
    data class ShowMessage(val messageResId: Int) : MainEvent
    data object OpenAppSettings : MainEvent
    data object OpenLocationSettings : MainEvent
    data object RequestEnableBluetooth : MainEvent
    data object RequestScanPermissions : MainEvent
    data object RequestNotificationPermission : MainEvent
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository,
    private val startScanningUseCase: StartScanningUseCase,
    private val stopScanningUseCase: StopScanningUseCase,
    private val detectionLogRepository: DetectionLogRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _event = Channel<MainEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    val isScanning = combine(
        settingsRepository.isScanning,
        bluetoothRepository.isScanning,
        ScanUiStatePolicy::isScanning
    ).stateIn(viewModelScope, SharingStarted.Lazily, false)

    val uiState = isScanning
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

    val recentDetections = detectionLogRepository.getAllLogs()
        .map { logs -> logs.take(5) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val nearbyDevices = bluetoothRepository.nearbyDevices
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val backgroundScanningEnabled = settingsRepository.backgroundEnabled
        .map { BackgroundScanSupport.isEnabled(it) }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val scanBlockerRefresh = MutableStateFlow(0)
    val restorePrompt = combine(
        settingsRepository.isScanning,
        bluetoothRepository.isScanning,
        scanBlockerRefresh
    ) { persistedIntent, _, _ ->
        ScanUiStatePolicy.restorePrompt(
            persistedIntent = persistedIntent,
            hasScanPermissions = bluetoothRepository.hasPermissions(),
            requiresLocationServices = Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
            locationServicesEnabled = bluetoothRepository.isLocationServicesEnabled(),
            bluetoothEnabled = bluetoothRepository.isBluetoothEnabled()
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, ScanRestorePrompt.None)

    private var notificationPrompted = false

    fun refreshScanBlockers() {
        scanBlockerRefresh.value += 1
    }

    fun restoreScanningEnvironment() {
        when (restorePrompt.value) {
            ScanRestorePrompt.ScanPermission -> viewModelScope.launch {
                _event.send(MainEvent.RequestScanPermissions)
            }
            ScanRestorePrompt.Bluetooth -> viewModelScope.launch {
                _event.send(MainEvent.RequestEnableBluetooth)
            }
            ScanRestorePrompt.Location -> viewModelScope.launch {
                _event.send(MainEvent.ShowMessage(R.string.error_location_pre_s))
                _event.send(MainEvent.OpenLocationSettings)
            }
            ScanRestorePrompt.None -> refreshScanBlockers()
        }
    }

    fun toggleScanning() {
        if (isScanning.value) {
            stopScanning()
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        viewModelScope.launch {
            when (
                ScanStartPolicy.evaluate(
                    hasBleHardware = bluetoothRepository.hasBleHardwareSupport(),
                    bluetoothEnabled = bluetoothRepository.isBluetoothEnabled(),
                    hasScanPermissions = bluetoothRepository.hasPermissions(),
                    requiresLocationServices = Build.VERSION.SDK_INT < Build.VERSION_CODES.S,
                    locationServicesEnabled = bluetoothRepository.isLocationServicesEnabled(),
                    hasNotificationPermission = bluetoothRepository.hasNotificationPermission(),
                    notificationPrompted = notificationPrompted
                )
            ) {
                ScanStartRequirement.MissingBleHardware -> {
                    _event.send(MainEvent.ShowMessage(R.string.error_ble_unsupported))
                    return@launch
                }
                ScanStartRequirement.BluetoothDisabled -> {
                    _event.send(MainEvent.RequestEnableBluetooth)
                    return@launch
                }
                ScanStartRequirement.MissingScanPermissions -> {
                    _event.send(MainEvent.RequestScanPermissions)
                    return@launch
                }
                ScanStartRequirement.LocationDisabled -> {
                    _event.send(MainEvent.ShowMessage(R.string.error_location_pre_s))
                    _event.send(MainEvent.OpenLocationSettings)
                    return@launch
                }
                ScanStartRequirement.NotificationPermissionNeeded -> {
                    notificationPrompted = true
                    _event.send(MainEvent.RequestNotificationPermission)
                    return@launch
                }
                ScanStartRequirement.Ready -> Unit
            }

            try {
                startScanningUseCase()
            } catch (_: Exception) {
                _event.send(MainEvent.ShowMessage(R.string.error_scan_start))
            }
        }
    }

    fun onBluetoothEnabled() {
        startScanning()
    }

    fun onScanPermissionsResolved(granted: Boolean) {
        if (granted) {
            refreshScanBlockers()
            startScanning()
            return
        }
        viewModelScope.launch {
            _event.send(MainEvent.OpenAppSettings)
        }
    }

    fun onNotificationPermissionResolved() {
        startScanning()
    }

    private fun stopScanning() {
        viewModelScope.launch {
            try {
                stopScanningUseCase()
            } catch (_: Exception) {
                _event.send(MainEvent.ShowMessage(R.string.error_scan_stop))
            }
        }
    }
}
