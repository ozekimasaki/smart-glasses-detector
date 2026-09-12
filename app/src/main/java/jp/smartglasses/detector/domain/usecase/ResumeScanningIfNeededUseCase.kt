package jp.smartglasses.detector.domain.usecase

import jp.smartglasses.detector.domain.repository.BluetoothRepository
import jp.smartglasses.detector.domain.repository.SettingsRepository
import jp.smartglasses.detector.domain.service.ScanResumePolicy
import jp.smartglasses.detector.domain.service.ScanServiceController
import jp.smartglasses.detector.util.BackgroundScanSupport
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ResumeScanningIfNeededUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val bluetoothRepository: BluetoothRepository,
    private val scanServiceController: ScanServiceController
) {
    suspend operator fun invoke(
        appInForeground: Boolean = false,
        backgroundScanSupported: Boolean = BackgroundScanSupport.isSupported()
    ) {
        val shouldResume = ScanResumePolicy.shouldResume(
            wasScanning = settingsRepository.isScanning.first(),
            backgroundEnabled = backgroundScanSupported && settingsRepository.backgroundEnabled.first(),
            hasPermissions = bluetoothRepository.hasPermissions(),
            appInForeground = appInForeground
        )
        if (!shouldResume) {
            return
        }

        scanServiceController.startScanService(fromBackground = !appInForeground)
    }
}
