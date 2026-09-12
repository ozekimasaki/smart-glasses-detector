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
import jp.smartglasses.detector.domain.service.ScanResumePolicy
import jp.smartglasses.detector.domain.usecase.ResumeScanningIfNeededUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SmartGlassesDetectorApp : Application() {
    @Inject
    lateinit var resumeScanningIfNeeded: ResumeScanningIfNeededUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        processLifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    resumeScanningInBackground("app became visible", appInForeground = true)
                }
            }
        )
        if (processLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            resumeScanningInBackground("app process started in foreground", appInForeground = true)
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

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    companion object {
        private const val TAG = "SmartGlassesApp"
    }
}
