package jp.smartglasses.detector

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import jp.smartglasses.detector.data.bluetooth.ScanPermissionAppOpsMonitor
import jp.smartglasses.detector.domain.service.ScanResumePolicy
import jp.smartglasses.detector.domain.usecase.ResumeScanningIfNeededUseCase
import jp.smartglasses.detector.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SmartGlassesDetectorApp : Application() {
    @Inject
    lateinit var resumeScanningIfNeeded: ResumeScanningIfNeededUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var foregroundResumeJob: Job? = null
    private var scanPermissionMonitor: ScanPermissionAppOpsMonitor? = null

    private val resumeReceiver = object : BroadcastReceiver() {
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
            applicationScope.launch {
                try {
                    resumeScanningIfNeeded(appInForeground = isAppInForeground())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to resume scanning after ${intent?.action}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            resumeReceiver,
            resumeIntentFilter(),
            ContextCompat.RECEIVER_EXPORTED
        )
        startScanPermissionWatch()
        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        processLifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    resumeScanningInBackground("app became visible", appInForeground = true)
                    startForegroundResumeLoop()
                }

                override fun onStop(owner: LifecycleOwner) {
                    stopForegroundResumeLoop()
                }
            }
        )
        if (processLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            resumeScanningInBackground("app process started in foreground", appInForeground = true)
            startForegroundResumeLoop()
        }
    }

    private fun startScanPermissionWatch() {
        if (scanPermissionMonitor != null) {
            return
        }

        val monitor = ScanPermissionAppOpsMonitor(this) {
            resumeScanningInBackground(
                reason = "scan permission changed",
                appInForeground = isAppInForeground()
            )
        }
        if (monitor.start()) {
            scanPermissionMonitor = monitor
        }
    }

    private fun resumeIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                addAction(LocationManager.MODE_CHANGED_ACTION)
            }
        }
    }

    private fun resumeScanningInBackground(reason: String, appInForeground: Boolean) {
        applicationScope.launch {
            try {
                resumeScanningIfNeeded(appInForeground = appInForeground)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resume scanning after $reason", e)
            }
        }
    }

    private fun startForegroundResumeLoop() {
        if (foregroundResumeJob?.isActive == true) {
            return
        }

        foregroundResumeJob = applicationScope.launch {
            while (true) {
                delay(Constants.SCAN_HEALTH_CHECK_INTERVAL_MS)
                try {
                    resumeScanningIfNeeded(appInForeground = true)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to refresh scanning while the app is visible", e)
                }
            }
        }
    }

    private fun stopForegroundResumeLoop() {
        foregroundResumeJob?.cancel()
        foregroundResumeJob = null
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    companion object {
        private const val TAG = "SmartGlassesApp"
    }
}
