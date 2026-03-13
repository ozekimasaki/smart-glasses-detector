package jp.smartglasses.detector.domain.usecase

import jp.smartglasses.detector.domain.repository.BluetoothRepository
import javax.inject.Inject

class StartScanningUseCase @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) {
    suspend operator fun invoke() {
        bluetoothRepository.startScanning()
    }
}
