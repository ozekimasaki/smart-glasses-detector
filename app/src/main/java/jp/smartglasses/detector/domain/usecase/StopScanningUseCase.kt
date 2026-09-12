package jp.smartglasses.detector.domain.usecase

import jp.smartglasses.detector.domain.service.ScanServiceController
import javax.inject.Inject

class StopScanningUseCase @Inject constructor(
    private val scanServiceController: ScanServiceController
) {
    operator fun invoke() {
        scanServiceController.stopScanService()
    }
}
