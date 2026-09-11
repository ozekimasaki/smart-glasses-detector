package jp.smartglasses.detector.receiver

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import jp.smartglasses.detector.domain.service.ScanResumePolicy
import jp.smartglasses.detector.domain.usecase.ResumeScanningIfNeededUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject
    lateinit var resumeScanningIfNeeded: ResumeScanningIfNeededUseCase

    override fun onReceive(context: Context, intent: Intent?) {
        val bluetoothState = if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        } else {
            null
        }
        if (!ScanResumePolicy.shouldHandleAction(intent?.action, bluetoothState)) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                resumeScanningIfNeeded(appInForeground = false)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resume scanning after ${intent?.action}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
