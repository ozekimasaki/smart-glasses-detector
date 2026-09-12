package jp.smartglasses.detector.data.service

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.smartglasses.detector.domain.service.ScanResumePolicy
import jp.smartglasses.detector.domain.service.ScanServiceController
import jp.smartglasses.detector.service.ScanningForegroundService
import javax.inject.Inject

class ScanServiceControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ScanServiceController {

    override fun startScanService(fromBackground: Boolean) {
        val intent = Intent(context, ScanningForegroundService::class.java).apply {
            action = ScanningForegroundService.ACTION_START
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            if (
                ScanResumePolicy.shouldSwallowBackgroundForegroundStartFailure(fromBackground) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException
            ) {
                Log.w(TAG, "Cannot start foreground service from background; will retry when visible", e)
                return
            }
            throw e
        }
    }

    override fun stopScanService() {
        val intent = Intent(context, ScanningForegroundService::class.java).apply {
            action = ScanningForegroundService.ACTION_STOP
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot deliver stop action, falling back to stopService", e)
            context.stopService(Intent(context, ScanningForegroundService::class.java))
        }
    }

    companion object {
        private const val TAG = "ScanServiceController"
    }
}
