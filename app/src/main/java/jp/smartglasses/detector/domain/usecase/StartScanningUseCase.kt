package jp.smartglasses.detector.domain.usecase

import jp.smartglasses.detector.domain.service.ScanServiceController
import javax.inject.Inject

class StartScanningUseCase @Inject constructor(
    private val scanServiceController: ScanServiceController
) {
    operator fun invoke() {
        scanServiceController.startScanService()
    }
}
