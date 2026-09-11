package jp.smartglasses.detector.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jp.smartglasses.detector.domain.repository.BluetoothRepository
import jp.smartglasses.detector.domain.repository.SettingsRepository
import jp.smartglasses.detector.domain.service.ScanResumePolicy
import jp.smartglasses.detector.domain.service.ScanServiceController
import jp.smartglasses.detector.util.BackgroundScanSupport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var bluetoothRepository: BluetoothRepository

    @Inject
    lateinit var scanServiceController: ScanServiceController

    override fun onReceive(context: Context, intent: Intent?) {
        if (!ScanResumePolicy.shouldHandleAction(intent?.action)) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                resumeScanningIfNeeded()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resume scanning after ${intent?.action}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun resumeScanningIfNeeded() {
        val shouldResume = ScanResumePolicy.shouldResume(
            wasScanning = settingsRepository.isScanning.first(),
            backgroundEnabled = BackgroundScanSupport.isEnabled(
                settingsRepository.backgroundEnabled.first()
            ),
            hasPermissions = bluetoothRepository.hasPermissions()
        )
        if (!shouldResume) {
            return
        }

        scanServiceController.startScanService()
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
