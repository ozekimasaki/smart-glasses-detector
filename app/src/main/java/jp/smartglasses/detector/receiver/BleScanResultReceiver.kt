package jp.smartglasses.detector.receiver

import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jp.smartglasses.detector.data.bluetooth.SmartGlassesDetector
import jp.smartglasses.detector.domain.repository.BluetoothRepository
import jp.smartglasses.detector.domain.repository.SettingsRepository
import jp.smartglasses.detector.domain.service.BleScanPendingIntentPolicy
import jp.smartglasses.detector.domain.usecase.ResumeScanningIfNeededUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BleScanResultReceiver : BroadcastReceiver() {
    @Inject
    lateinit var smartGlassesDetector: SmartGlassesDetector

    @Inject
    lateinit var resumeScanningIfNeeded: ResumeScanningIfNeededUseCase

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var bluetoothRepository: BluetoothRepository

    override fun onReceive(context: Context, intent: Intent?) {
        if (!BleScanPendingIntentPolicy.shouldHandleDelivery(intent?.action)) {
            return
        }

        val errorCode = intent?.getIntExtra(
            BluetoothLeScanner.EXTRA_ERROR_CODE,
            BleScanPendingIntentPolicy.DEFAULT_ERROR_CODE
        ) ?: BleScanPendingIntentPolicy.DEFAULT_ERROR_CODE
        val results = extractScanResults(intent)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val persistedScanning = settingsRepository.isScanning.first()
                if (
                    BleScanPendingIntentPolicy.shouldReportFailure(
                        persistedScanning = persistedScanning,
                        errorCode = errorCode
                    )
                ) {
                    smartGlassesDetector.reportScanFailure(errorCode)
                }
                if (
                    BleScanPendingIntentPolicy.shouldIngestResults(
                        persistedScanning = persistedScanning,
                        errorCode = errorCode,
                        resultCount = results.size
                    )
                ) {
                    smartGlassesDetector.ingestScanResults(results)
                }
                if (
                    BleScanPendingIntentPolicy.shouldResumeServiceAfterDelivery(
                        persistedScanning = persistedScanning,
                        hardwareScanRunning = bluetoothRepository.isHardwareScanRunning.first()
                    )
                ) {
                    resumeScanningIfNeeded(appInForeground = false)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to ingest surviving BLE scan results", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun extractScanResults(intent: Intent?): List<ScanResult> {
        if (intent == null) {
            return emptyList()
        }
        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(
                BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT,
                ScanResult::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT)
        }
        return results.orEmpty()
    }

    companion object {
        private const val TAG = "BleScanResultReceiver"
    }
}
