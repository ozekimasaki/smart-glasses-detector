package jp.smartglasses.detector.domain.service

interface ScanServiceController {
    fun startScanService(fromBackground: Boolean = false)
    fun stopScanService()
}
