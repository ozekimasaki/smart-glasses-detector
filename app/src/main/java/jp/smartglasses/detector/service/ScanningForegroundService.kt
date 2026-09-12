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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import jp.smartglasses.detector.MainActivity
import jp.smartglasses.detector.R
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.domain.model.Distance
import jp.smartglasses.detector.domain.repository.BluetoothRepository
import jp.smartglasses.detector.domain.repository.DetectionLogRepository
import jp.smartglasses.detector.domain.repository.SettingsRepository
import jp.smartglasses.detector.domain.service.BackgroundScanRuntimePolicy
import jp.smartglasses.detector.domain.service.DetectionNotificationPolicy
import jp.smartglasses.detector.domain.service.ScanEnvironmentSignals
import jp.smartglasses.detector.domain.service.ScanFailurePolicy
import jp.smartglasses.detector.domain.service.ScanResumePolicy
import jp.smartglasses.detector.util.BackgroundScanSupport
import jp.smartglasses.detector.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean

@AndroidEntryPoint
class ScanningForegroundService : Service() {
    
    @Inject
    lateinit var bluetoothRepository: BluetoothRepository
    
    @Inject
    lateinit var detectionLogRepository: DetectionLogRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var scanEnvironmentSignals: ScanEnvironmentSignals
    
    private val binder = LocalBinder()
    private var scanJob: Job? = null
    private var healthCheckJob: Job? = null
    private var environmentWatchJob: Job? = null
    private var backgroundSettingsJob: Job? = null
    private var scanningStateJob: Job? = null
    private var sensitivityJob: Job? = null
    private var alertSettingsJob: Job? = null
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + supervisorJob)
    private val isStopping = AtomicBoolean(false)
    private val pendingRecreationStartIds = ArrayList<Int>()
    @Volatile private var settingsReady = false
    @Volatile private var backgroundScanningEnabled = false
    @Volatile private var persistedScanningState = false
    @Volatile private var notificationEnabled = true
    @Volatile private var vibrationEnabled = true
    @Volatile private var soundEnabled = true
    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            applyEffectiveScanSensitivity()
        }

        override fun onStop(owner: LifecycleOwner) {
            if (!shouldKeepScanningInBackground()) {
                pauseScanningKeepingIntent()
            } else {
                applyEffectiveScanSensitivity()
            }
        }
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): ScanningForegroundService = this@ScanningForegroundService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        observeSettings()
        createNotificationChannels()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
        scope.launch {
            loadPersistedSettings()
            withContext(Dispatchers.Main.immediate) {
                settingsReady = true
                val startIds = pendingRecreationStartIds.toList()
                pendingRecreationStartIds.clear()
                startIds.forEach { startId ->
                    handleRecreation(startId)
                }
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val explicitStart = intent?.action == ACTION_START
        val explicitStop = intent?.action == ACTION_STOP
        when {
            explicitStart -> startForegroundWithNotification()
            explicitStop -> {
                pendingRecreationStartIds.clear()
                stopScanningAndStopSelf()
            }
            ScanResumePolicy.shouldDeferRecreationUntilSettingsReady(settingsReady) -> {
                if (
                    ScanResumePolicy.shouldPromoteForegroundWhileSettingsLoad(
                        settingsReady = false,
                        explicitStart = false,
                        explicitStop = false
                    )
                ) {
                    promoteToForeground()
                }
                pendingRecreationStartIds += startId
            }
            else -> handleRecreation(startId)
        }
        return if (
            ScanResumePolicy.shouldRestartSticky(
                explicitStart = explicitStart,
                explicitStop = explicitStop,
                settingsReady = settingsReady,
                persistedScanningState = persistedScanningState
            )
        ) {
            START_STICKY
        } else {
            START_NOT_STICKY
        }
    }

    private fun handleRecreation(startId: Int) {
        if (isStopping.get()) {
            return
        }
        if (shouldResumeScanningAfterRestart()) {
            startForegroundWithNotification()
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }
    }

    private fun startForegroundWithNotification() {
        if (isStopping.get()) {
            return
        }

        if (scanJob?.isActive == true) {
            if (ScanResumePolicy.shouldRefreshHardwareOnDuplicateStart(scanAlreadyActive = true)) {
                bluetoothRepository.ensureHardwareScanning()
            }
            return
        }

        promoteToForeground()
        startScanning()
    }

    private fun promoteToForeground() {
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
    }

    private fun startScanning() {
        scanJob = scope.launch {
            val deviceCollectionJob = launch(start = CoroutineStart.UNDISPATCHED) {
                bluetoothRepository.scannedDevices.collect { device ->
                    onDeviceDetected(device)
                }
            }

            val scanFailureCollectionJob = launch(start = CoroutineStart.UNDISPATCHED) {
                bluetoothRepository.scanFailures.collect { failure ->
                    Log.e(TAG, "Bluetooth scan failed with error code ${failure.errorCode}")
                    if (
                        ScanFailurePolicy.shouldPauseScanning(
                            errorCode = failure.errorCode,
                            bluetoothEnabled = bluetoothRepository.isBluetoothEnabled(),
                            locationServicesEnabled = bluetoothRepository.isLocationServicesEnabled(),
                            scanPermissionGranted = bluetoothRepository.hasPermissions()
                        )
                    ) {
                        pauseHardwareKeepingSession()
                    }
                }
            }

            try {
                persistScanningState(true)
                startOrPauseHardwareForCurrentEnvironment()
                startHealthCheck()
                startEnvironmentWatch()

                try {
                    awaitCancellation()
                } finally {
                    deviceCollectionJob.cancel()
                    scanFailureCollectionJob.cancel()
                    healthCheckJob?.cancel()
                    healthCheckJob = null
                    environmentWatchJob?.cancel()
                    environmentWatchJob = null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Bluetooth scanning", e)
                pauseHardwareKeepingSession()
            } finally {
                scanJob = null
                stopBluetoothScanSafely()
                if (
                    !ScanResumePolicy.shouldKeepScanningIntent(
                        userOrPolicyStop = isStopping.get()
                    )
                ) {
                    persistScanningState(false)
                }
            }
        }
    }

    private suspend fun startOrPauseHardwareForCurrentEnvironment() {
        try {
            bluetoothRepository.startScanning()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Bluetooth scanning", e)
            bluetoothRepository.pauseHardwareKeepingSession()
        }
        refreshScanningNotification()
    }

    private fun pauseHardwareKeepingSession() {
        if (isStopping.get()) {
            return
        }
        bluetoothRepository.pauseHardwareKeepingSession()
        refreshScanningNotification()
    }

    private fun stopScanningAndStopSelf() {
        if (!isStopping.compareAndSet(false, true)) return

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

    private fun pauseScanningKeepingIntent() {
        if (isStopping.get()) {
            return
        }

        scope.launch {
            val activeJob = scanJob
            scanJob = null
            if (activeJob != null) {
                activeJob.cancelAndJoin()
            } else {
                stopBluetoothScanSafely()
            }

            stopForegroundAndSelf()
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        if (!shouldKeepScanningInBackground()) {
            pauseScanningKeepingIntent()
        }
    }

    private fun applyEffectiveScanSensitivity() {
        if (isStopping.get()) {
            return
        }

        scope.launch {
            bluetoothRepository.updateScanSensitivity(settingsRepository.sensitivity.first())
        }
    }

    private suspend fun onDeviceDetected(device: SmartGlassesDevice) {
        detectionLogRepository.insertLog(
            jp.smartglasses.detector.domain.model.DetectionLog(
                deviceName = device.name,
                deviceAddress = device.address,
                manufacturerName = device.manufacturer.name,
                rssi = device.rssi,
                distance = device.distance.name,
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

    private val openAppPendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createScanningNotification(): Notification {
        val waitingForEnvironment = ScanResumePolicy.shouldPauseForLostEnvironment(
            hasPermissions = bluetoothRepository.hasPermissions(),
            bluetoothEnabled = bluetoothRepository.isBluetoothEnabled(),
            locationServicesEnabled = bluetoothRepository.isLocationServicesEnabled()
        )
        val contentText = when {
            waitingForEnvironment -> getString(R.string.notification_scanning_text_waiting)
            shouldKeepScanningInBackground() -> getString(R.string.notification_scanning_text_background)
            else -> getString(R.string.notification_scanning_text_foreground)
        }
        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID_SCANNING)
            .setContentTitle(getString(R.string.notification_scanning_title))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification_scan)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showDetectionNotification(device: SmartGlassesDevice, playSound: Boolean) {
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID_DETECTION)
            .setContentTitle(getString(R.string.notification_detection_title))
            .setContentText("${device.name} - ${getString(distanceLabelRes(device.distance))}")
            .setSmallIcon(R.drawable.ic_notification_alert)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(DetectionNotificationPolicy.GROUP_KEY)
            .setAutoCancel(true)
            .apply {
                if (playSound) {
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    setSound(soundUri)
                }
            }
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(
            DetectionNotificationPolicy.notificationId(
                address = device.address,
                name = device.name
            ),
            notification
        )
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
        backgroundSettingsJob?.cancel()
        scanningStateJob?.cancel()
        sensitivityJob?.cancel()
        alertSettingsJob?.cancel()
        healthCheckJob?.cancel()
        environmentWatchJob?.cancel()
        scanJob?.cancel()
        scanJob = null
        stopBluetoothScanNow()
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
        return ScanResumePolicy.shouldRestartAfterRecreation(
            persistedIntent = persistedScanningState,
            backgroundEnabled = backgroundScanningEnabled,
            appInForeground = isAppInForeground()
        )
    }

    private fun shouldKeepScanningInBackground(): Boolean {
        return backgroundScanningEnabled
    }

    private fun stopBluetoothScanNow() {
        try {
            bluetoothRepository.stopScanningNow()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop Bluetooth scanning safely", e)
        }
    }

    private suspend fun stopBluetoothScanSafely() {
        stopBluetoothScanNow()
    }

    private suspend fun persistScanningState(scanning: Boolean) {
        persistedScanningState = scanning
        try {
            settingsRepository.setIsScanning(scanning)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist scanning state", e)
        }
    }

    private suspend fun loadPersistedSettings() {
        try {
            backgroundScanningEnabled = BackgroundScanSupport.isEnabled(
                settingsRepository.backgroundEnabled.first()
            )
            persistedScanningState = settingsRepository.isScanning.first()
            notificationEnabled = settingsRepository.notificationEnabled.first()
            vibrationEnabled = settingsRepository.vibrationEnabled.first()
            soundEnabled = settingsRepository.soundEnabled.first()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize cached scanning settings", e)
            backgroundScanningEnabled = false
            persistedScanningState = false
        }
    }

    private fun observeSettings() {
        backgroundSettingsJob = scope.launch {
            settingsRepository.backgroundEnabled.collect { enabled ->
                backgroundScanningEnabled = BackgroundScanSupport.isEnabled(enabled)
                refreshScanningNotification()
                if (
                    BackgroundScanRuntimePolicy.shouldStopService(
                        backgroundEnabled = backgroundScanningEnabled,
                        appInForeground = isAppInForeground()
                    )
                ) {
                    pauseScanningKeepingIntent()
                }
            }
        }

        scanningStateJob = scope.launch {
            settingsRepository.isScanning.collect { scanning ->
                persistedScanningState = scanning
            }
        }

        sensitivityJob = scope.launch {
            settingsRepository.sensitivity.collect { sensitivity ->
                bluetoothRepository.updateScanSensitivity(sensitivity)
            }
        }

        alertSettingsJob = scope.launch {
            combine(
                settingsRepository.notificationEnabled,
                settingsRepository.vibrationEnabled,
                settingsRepository.soundEnabled
            ) { notifications, vibration, sound ->
                Triple(notifications, vibration, sound)
            }.collect { (notifications, vibration, sound) ->
                notificationEnabled = notifications
                vibrationEnabled = vibration
                soundEnabled = sound
            }
        }
    }

    private fun startHealthCheck() {
        healthCheckJob?.cancel()
        healthCheckJob = scope.launch {
            while (true) {
                delay(Constants.SCAN_HEALTH_CHECK_INTERVAL_MS)
                applyEnvironmentState()
            }
        }
    }

    private fun startEnvironmentWatch() {
        environmentWatchJob?.cancel()
        environmentWatchJob = scope.launch {
            scanEnvironmentSignals.revision.collect {
                applyEnvironmentState()
            }
        }
    }

    private suspend fun applyEnvironmentState() {
        if (isStopping.get() || scanJob?.isActive != true) {
            return
        }

        val hasPermissions = bluetoothRepository.hasPermissions()
        val bluetoothEnabled = bluetoothRepository.isBluetoothEnabled()
        val locationServicesEnabled = bluetoothRepository.isLocationServicesEnabled()
        if (
            ScanResumePolicy.shouldPauseForLostEnvironment(
                hasPermissions = hasPermissions,
                bluetoothEnabled = bluetoothEnabled,
                locationServicesEnabled = locationServicesEnabled
            )
        ) {
            Log.w(TAG, "Required scan permission, Bluetooth, or location services are no longer available.")
            if (
                !ScanResumePolicy.shouldKeepForegroundServiceDuringEnvironmentPause(
                    backgroundEnabled = backgroundScanningEnabled,
                    appInForeground = isAppInForeground()
                )
            ) {
                pauseScanningKeepingIntent()
                return
            }
            if (bluetoothRepository.isHardwareScanRunning.first()) {
                pauseHardwareKeepingSession()
            } else {
                refreshScanningNotification()
            }
            return
        }
        if (
            ScanResumePolicy.shouldRestartHardwareScan(
                hasPermissions = hasPermissions,
                bluetoothEnabled = bluetoothEnabled,
                locationServicesEnabled = locationServicesEnabled,
                hardwareScanning = bluetoothRepository.isHardwareScanRunning.first()
            )
        ) {
            Log.w(TAG, "Hardware scan stopped while the environment is healthy; restarting")
            try {
                bluetoothRepository.ensureHardwareScanning()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to restart Bluetooth scanning", e)
            }
            refreshScanningNotification()
        }
    }

    private fun refreshScanningNotification() {
        if (isStopping.get() || scanJob?.isActive != true) {
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
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    private fun distanceLabelRes(distance: Distance): Int {
        return when (distance) {
            Distance.VERY_CLOSE -> R.string.distance_very_close
            Distance.CLOSE -> R.string.distance_close
            Distance.MODERATE -> R.string.distance_medium
            Distance.FAR -> R.string.distance_far
        }
    }
}
