package jp.smartglasses.detector.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import jp.smartglasses.detector.MainActivity
import jp.smartglasses.detector.R
import jp.smartglasses.detector.domain.model.DiagnosticLog
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.domain.repository.BluetoothRepository
import jp.smartglasses.detector.domain.repository.DiagnosticLogRepository
import jp.smartglasses.detector.domain.repository.DetectionLogRepository
import jp.smartglasses.detector.domain.repository.SettingsRepository
import jp.smartglasses.detector.util.BackgroundScanSupport
import jp.smartglasses.detector.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ScanningForegroundService : Service() {
    
    @Inject
    lateinit var bluetoothRepository: BluetoothRepository
    
    @Inject
    lateinit var detectionLogRepository: DetectionLogRepository

    @Inject
    lateinit var diagnosticLogRepository: DiagnosticLogRepository
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    private val binder = LocalBinder()
    private var scanJob: Job? = null
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + supervisorJob)
    @Volatile
    private var isStopping = false
    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            if (!shouldKeepScanningInBackground()) {
                stopScanningAndStopSelf()
            }
        }
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): ScanningForegroundService = this@ScanningForegroundService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundWithNotification()
            ACTION_STOP -> stopScanningAndStopSelf()
            null -> {
                if (shouldResumeScanningAfterRestart()) {
                    startForegroundWithNotification()
                } else {
                    stopSelf(startId)
                }
            }
        }
        return resolveRestartMode()
    }

    private fun startForegroundWithNotification() {
        if (scanJob?.isActive == true) {
            return
        }

        val notification = createScanningNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                Constants.NOTIFICATION_ID_SCANNING,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(Constants.NOTIFICATION_ID_SCANNING, notification)
        }
        
        startScanning()
    }

    private fun startScanning() {
        scanJob = scope.launch {
            try {
                if (!bluetoothRepository.hasPermissions()) {
                    Log.w(TAG, "Missing Bluetooth permission. Stop foreground service.")
                    stopForegroundAndSelf()
                    return@launch
                }

                bluetoothRepository.startScanning()
                settingsRepository.setIsScanning(true)

                val deviceCollectionJob = launch {
                    bluetoothRepository.scannedDevices.collect { device ->
                        onDeviceDetected(device)
                    }
                }

                val diagnosticCollectionJob = launch {
                    bluetoothRepository.diagnosticLogs.collect { log ->
                        onDiagnosticLogDetected(log)
                    }
                }

                try {
                    awaitCancellation()
                } finally {
                    deviceCollectionJob.cancel()
                    diagnosticCollectionJob.cancel()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Bluetooth scanning", e)
                stopForegroundAndSelf()
            } finally {
                stopBluetoothScanSafely()
                persistScanningState(false)
            }
        }
    }

    private fun stopScanningAndStopSelf() {
        if (isStopping) return
        isStopping = true

        scope.launch {
            val activeJob = scanJob
            scanJob = null
            if (activeJob != null) {
                activeJob.cancelAndJoin()
            } else {
                stopBluetoothScanSafely()
                persistScanningState(false)
            }

            stopForegroundAndSelf()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        if (!shouldKeepScanningInBackground()) {
            stopScanningAndStopSelf()
        }
    }

    private suspend fun onDeviceDetected(device: SmartGlassesDevice) {
        val notificationEnabled = settingsRepository.notificationEnabled.first()
        val vibrationEnabled = settingsRepository.vibrationEnabled.first()
        val soundEnabled = settingsRepository.soundEnabled.first()
        
        detectionLogRepository.insertLog(
            jp.smartglasses.detector.domain.model.DetectionLog(
                deviceName = device.name,
                deviceAddress = device.address,
                manufacturerName = device.manufacturer.name,
                rssi = device.rssi,
                distance = device.distance.label,
                detectedAt = device.detectedAt
            )
        )
        
        if (notificationEnabled) {
            showDetectionNotification(device, soundEnabled)
        }
        
        if (vibrationEnabled) {
            vibrate()
        }
    }

    private suspend fun onDiagnosticLogDetected(log: DiagnosticLog) {
        diagnosticLogRepository.insertLog(log)
    }
    
    private fun createScanningNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID_SCANNING)
            .setContentTitle(getString(R.string.notification_scanning_title))
            .setContentText(
                getString(
                    if (shouldKeepScanningInBackground()) {
                        R.string.notification_scanning_text_background
                    } else {
                        R.string.notification_scanning_text_foreground
                    }
                )
            )
            .setSmallIcon(R.drawable.ic_notification_scan)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun showDetectionNotification(device: SmartGlassesDevice, playSound: Boolean) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID_DETECTION)
            .setContentTitle(getString(R.string.notification_detection_title))
            .setContentText("${device.name} - ${device.distance.label}")
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply {
                if (playSound) {
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setSound(soundUri)
                }
            }
            .build()
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(Constants.NOTIFICATION_ID_DETECTION, notification)
    }

    private fun vibrate() {
        val vibrator = getSystemService(Vibrator::class.java)
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
    }

    private fun createNotificationChannels() {
        val scanningChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID_SCANNING,
            getString(R.string.notification_channel_scanning),
            NotificationManager.IMPORTANCE_LOW
        )

        val detectionChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID_DETECTION,
            getString(R.string.notification_channel_detection),
            NotificationManager.IMPORTANCE_HIGH
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(scanningChannel, detectionChannel))
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)

        if (!isStopping) {
            runBlocking {
                scanJob?.cancelAndJoin()
                stopBluetoothScanSafely()
                persistScanningState(false)
            }
        }

        supervisorJob.cancel()
        super.onDestroy()
    }
    
    companion object {
        private const val TAG = "ScanningFgService"
        const val ACTION_START = "jp.smartglasses.detector.action.START"
        const val ACTION_STOP = "jp.smartglasses.detector.action.STOP"
    }

    private suspend fun stopForegroundAndSelf() {
        withContext(Dispatchers.Main.immediate) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun shouldResumeScanningAfterRestart(): Boolean {
        return runBlocking(Dispatchers.IO) {
            settingsRepository.isScanning.first() &&
                BackgroundScanSupport.isEnabled(settingsRepository.backgroundEnabled.first())
        }
    }

    private fun resolveRestartMode(): Int {
        return if (shouldKeepScanningInBackground()) START_STICKY else START_NOT_STICKY
    }

    private fun shouldKeepScanningInBackground(): Boolean {
        return runBlocking(Dispatchers.IO) {
            BackgroundScanSupport.isEnabled(settingsRepository.backgroundEnabled.first())
        }
    }

    private suspend fun stopBluetoothScanSafely() {
        try {
            bluetoothRepository.stopScanning()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop Bluetooth scanning safely", e)
        }
    }

    private suspend fun persistScanningState(scanning: Boolean) {
        try {
            settingsRepository.setIsScanning(scanning)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist scanning state", e)
        }
    }
}
