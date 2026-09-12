package jp.smartglasses.detector.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.util.size
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.smartglasses.detector.domain.model.BluetoothScanFailure
import jp.smartglasses.detector.domain.model.DiagnosticLog
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.domain.model.deduplicationKey
import jp.smartglasses.detector.domain.model.hasPayload
import jp.smartglasses.detector.domain.repository.DiagnosticLogRepository
import jp.smartglasses.detector.domain.service.BleScanCompatibilityPolicy
import jp.smartglasses.detector.domain.service.BleScanCompatibilityStep
import jp.smartglasses.detector.domain.service.BleScanRefreshPolicy
import jp.smartglasses.detector.domain.service.BluetoothAdvertisedNamePolicy
import jp.smartglasses.detector.domain.service.ClassicDiscoveryPolicy
import jp.smartglasses.detector.domain.service.HardwareScanStatePolicy
import jp.smartglasses.detector.domain.service.ScanEnvironmentSignals
import jp.smartglasses.detector.domain.service.ScanFailurePolicy
import jp.smartglasses.detector.domain.service.ScanResumePolicy
import jp.smartglasses.detector.domain.service.SeenAdvertiserPolicy
import jp.smartglasses.detector.domain.service.SeenAdvertiserSnapshot
import jp.smartglasses.detector.util.Constants
import jp.smartglasses.detector.util.ScanSensitivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartGlassesDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val diagnosticLogRepository: DiagnosticLogRepository,
    private val scanEnvironmentSignals: ScanEnvironmentSignals
) {
    private val _scannedDevices = Channel<SmartGlassesDevice>(capacity = Channel.BUFFERED)
    val scannedDevices: Flow<SmartGlassesDevice> = _scannedDevices.receiveAsFlow()
    private val _scanFailures = Channel<BluetoothScanFailure>(capacity = Channel.BUFFERED)
    val scanFailures: Flow<BluetoothScanFailure> = _scanFailures.receiveAsFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    private val _hardwareScanRunning = MutableStateFlow(false)
    val isHardwareScanRunning: StateFlow<Boolean> = _hardwareScanRunning.asStateFlow()
    private val _nearbyDevices = MutableStateFlow<List<SmartGlassesDevice>>(emptyList())
    val nearbyDevices: StateFlow<List<SmartGlassesDevice>> = _nearbyDevices.asStateFlow()
    private val detectionCooldownGate = DetectionCooldownGate()
    private val nearbyDeviceTracker = NearbyDeviceTracker()
    private val classicInquiryAddresses = ConcurrentHashMap.newKeySet<String>()
    private val classicInquiryRssi = ConcurrentHashMap<String, Int>()
    private val seenAdvertisers = ConcurrentHashMap<String, SeenAdvertiserSnapshot>()
    private val scanSignalProcessor = ScanSignalProcessor()
    private val diagnosticWriteGate = DiagnosticLogWriteGate()
    private val isClassicDiscoveryReceiverRegistered = AtomicBoolean(false)
    private val classicDiscoveryStarted = AtomicBoolean(false)
    private val lastClassicDiscoveryStartedAt = AtomicLong(0L)
    private val isBluetoothStateReceiverRegistered = AtomicBoolean(false)
    private val isLocationModeReceiverRegistered = AtomicBoolean(false)
    private val userRequestedScanning = AtomicBoolean(false)
    private var scanPermissionMonitor: ScanPermissionAppOpsMonitor? = null
    private var lastSensitivity: ScanSensitivity = ScanSensitivity.BALANCED
    private var usingExtendedAdvertising = true
    private var usingMatchAllFilter = true
    private var matchAllFilterRejectedThisSession = false
    private var retryAttempt = 0
    private var scanWatchdogJob: Job? = null
    private var nearbyPruneJob: Job? = null
    private var retryJob: Job? = null
    private var classicDiscoveryJob: Job? = null
    private var classicDiscoveryRetryJob: Job? = null
    private val diagnosticPersistenceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Failed to persist diagnostic log", throwable)
        }
    )

    private val classicDiscoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val bluetoothDevice = intent.extractBluetoothDevice() ?: return
            val address = resolveDeviceAddress(bluetoothDevice)
            val scanningRequested = userRequestedScanning.get()
            if (action == BluetoothDevice.ACTION_FOUND && scanningRequested) {
                rememberSeenAdvertiser(
                    address = address,
                    rssi = intent.getShortExtra(
                        BluetoothDevice.EXTRA_RSSI,
                        Constants.UNKNOWN_RSSI_DBM.toShort()
                    ).toInt()
                )
            }
            if (
                !ClassicDiscoveryPolicy.shouldApplyInquiryUpdate(
                    action = action,
                    scanningRequested = scanningRequested,
                    alreadySeenAddress = wasSeenInClassicInquiry(address)
                )
            ) {
                return
            }
            handleClassicDiscoveryResult(intent)
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) {
                return
            }

            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> {
                    scanEnvironmentSignals.notifyChanged()
                    if (userRequestedScanning.get()) {
                        classicDiscoveryStarted.set(false)
                        startLeAndClassicScanning()
                    }
                }
                BluetoothAdapter.STATE_OFF,
                BluetoothAdapter.STATE_TURNING_OFF -> {
                    pauseHardwareScan(bluetoothEnabled = false)
                    _scanFailures.trySend(
                        BluetoothScanFailure(ScanFailurePolicy.SCAN_ENVIRONMENT_BLUETOOTH_DISABLED)
                    )
                }
            }
        }
    }

    private val locationModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != LocationManager.MODE_CHANGED_ACTION) {
                return
            }
            if (isLocationServicesSatisfied()) {
                scanEnvironmentSignals.notifyChanged()
                if (userRequestedScanning.get()) {
                    startLeAndClassicScanning()
                }
            } else {
                pauseHardwareScan(locationServicesSatisfied = false)
                _scanFailures.trySend(
                    BluetoothScanFailure(ScanFailurePolicy.SCAN_ENVIRONMENT_LOCATION_DISABLED)
                )
            }
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleDetectionSignal(extractSignal(result))
        }
        
        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { result ->
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            if (ScanFailurePolicy.shouldIgnore(errorCode)) {
                return
            }

            _hardwareScanRunning.value = false

            if (
                ScanFailurePolicy.shouldTryCompatibilityFallback(errorCode) &&
                userRequestedScanning.get()
            ) {
                when (
                    BleScanCompatibilityPolicy.nextStep(
                        usingMatchAllFilter = usingMatchAllFilter,
                        usingExtendedAdvertising = usingExtendedAdvertising
                    )
                ) {
                    BleScanCompatibilityStep.DROP_MATCH_ALL_FILTER -> {
                        Log.w(TAG, "Match-all BLE scan filter is unsupported, falling back to an unfiltered scan")
                        rejectMatchAllFilter()
                        startLeAndClassicScanning()
                        return
                    }
                    BleScanCompatibilityStep.DISABLE_EXTENDED_ADVERTISING -> {
                        Log.w(TAG, "Extended BLE scan is unsupported, falling back to legacy advertisements")
                        usingExtendedAdvertising = false
                        startLeAndClassicScanning()
                        return
                    }
                    BleScanCompatibilityStep.NONE -> Unit
                }
            }

            if (ScanFailurePolicy.isRecoverable(errorCode) && userRequestedScanning.get()) {
                Log.w(TAG, "Recoverable BLE scan failure $errorCode, retrying")
                scheduleScanRetry()
                return
            }

            _isScanning.value = false
            _scanFailures.trySend(BluetoothScanFailure(errorCode))
        }
    }
    
    fun detectSmartGlasses(result: ScanResult): SmartGlassesDevice? {
        return scanSignalProcessor.detectDevice(extractSignal(result), lastSensitivity)
    }

    private fun extractSignal(result: ScanResult): DetectionSignal {
        val scanRecord = result.scanRecord
        val parsedAdvertisement = parseAdvertisement(scanRecord)
        return DetectionSignal(
            deviceName = BluetoothAdvertisedNamePolicy.resolve(
                parsedAdvertisement.completeName,
                scanRecord?.deviceName,
                parsedAdvertisement.shortName,
                resolveCachedDeviceName(result.device),
                resolveDeviceAlias(result.device)
            ),
            address = resolveDeviceAddress(result),
            companyIds = (scanRecord?.let(::extractCompanyIds).orEmpty()) + parsedAdvertisement.companyIds,
            rssi = result.rssi,
            serviceUuids = BleUuid.merge(
                scanRecord?.serviceUuids?.map { uuid -> uuid.toString() }.orEmpty(),
                scanRecord?.serviceData?.keys?.map { uuid -> uuid.toString() }.orEmpty(),
                parsedAdvertisement.serviceUuids
            ),
            advertisementDataHex = scanRecord?.bytes?.toHexString().orEmpty(),
            extraPayloadHex = scanRecord?.let(::extractManufacturerPayloadHex).orEmpty(),
            appearance = parsedAdvertisement.appearance,
            deviceClass = resolveDeviceClass(result.device)
        )
    }

    private fun parseAdvertisement(scanRecord: ScanRecord?): ParsedAdvertisement {
        val fromBytes = AdvertisementParser.parse(scanRecord?.bytes)
        if (scanRecord == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return fromBytes
        }

        return fromBytes.merge(
            AdvertisementParser.parseAdvertisingDataMap(scanRecord.advertisingDataMap)
        )
    }

    private fun extractCompanyIds(scanRecord: ScanRecord): Set<Int> {
        val companyIds = mutableSetOf<Int>()
        val manufacturerSpecificData = scanRecord.manufacturerSpecificData
        for (index in 0 until manufacturerSpecificData.size) {
            companyIds += manufacturerSpecificData.keyAt(index)
        }
        return companyIds
    }

    private fun extractManufacturerPayloadHex(scanRecord: ScanRecord): String {
        val manufacturerSpecificData = scanRecord.manufacturerSpecificData
        if (manufacturerSpecificData.size == 0) {
            return ""
        }

        return buildString {
            for (index in 0 until manufacturerSpecificData.size) {
                append(
                    AdvertisementParser.encodeManufacturerSpecificTlv(
                        companyId = manufacturerSpecificData.keyAt(index),
                        payload = manufacturerSpecificData.valueAt(index) ?: byteArrayOf()
                    )
                )
            }
        }
    }

    private fun shouldEmitDetection(device: SmartGlassesDevice): Boolean {
        return detectionCooldownGate.shouldEmitDetection(
            deviceKey = buildDeviceKey(device),
            manufacturerKey = buildManufacturerKey(device)
        )
    }

    private fun buildDeviceKey(device: SmartGlassesDevice): String {
        val normalizedAddress = device.address.trim().uppercase()
        if (normalizedAddress.isNotEmpty()) {
            return "address:$normalizedAddress"
        }

        val normalizedManufacturer = device.manufacturer.name.trim().lowercase()
        val normalizedName = device.name.trim().lowercase()
        return "fallback:$normalizedManufacturer:$normalizedName"
    }

    private fun buildManufacturerKey(device: SmartGlassesDevice): String {
        return device.manufacturer.name.trim().lowercase()
    }

    private fun rememberSeenAdvertiser(
        address: String,
        rssi: Int,
        companyIds: Set<Int> = emptySet(),
        serviceUuids: List<String> = emptyList(),
        appearance: Int? = null,
        deviceClass: Int? = null,
        deviceName: String? = null
    ) {
        if (!ClassicDiscoveryPolicy.shouldRememberSeenAdvertiser(address)) {
            return
        }
        classicInquiryAddresses.add(address)
        if (rssi != Constants.UNKNOWN_RSSI_DBM) {
            classicInquiryRssi[address] = rssi
        }
        seenAdvertisers[address] = SeenAdvertiserPolicy.merge(
            existing = seenAdvertisers[address],
            rssi = rssi,
            companyIds = companyIds,
            serviceUuids = serviceUuids,
            appearance = appearance,
            deviceClass = deviceClass,
            deviceName = deviceName,
            nowMs = System.currentTimeMillis()
        )
    }

    private fun rememberSeenAdvertiser(signal: DetectionSignal) {
        rememberSeenAdvertiser(
            address = signal.address,
            rssi = signal.rssi,
            companyIds = signal.companyIds,
            serviceUuids = signal.serviceUuids,
            appearance = signal.appearance,
            deviceClass = signal.deviceClass,
            deviceName = signal.deviceName
        )
    }

    private fun enrichWithSeenAdvertiser(signal: DetectionSignal): DetectionSignal {
        rememberSeenAdvertiser(signal)
        val snapshot = seenAdvertisers[signal.address] ?: return signal
        return signal.copy(
            deviceName = snapshot.deviceName,
            companyIds = snapshot.companyIds,
            serviceUuids = snapshot.serviceUuids,
            appearance = snapshot.appearance,
            deviceClass = snapshot.deviceClass,
            rssi = snapshot.rssi
        )
    }

    private fun handleDetectionSignal(signal: DetectionSignal, action: String? = null) {
        val enriched = enrichWithSeenAdvertiser(signal)
        val processed = scanSignalProcessor.process(enriched, lastSensitivity)
        persistDiagnosticLog(processed.diagnosticLog)

        val detectedDevice = processed.detectedDevice
        if (detectedDevice != null) {
            rememberNearbyDevice(detectedDevice)
            if (shouldEmitDetection(detectedDevice)) {
                _scannedDevices.trySend(detectedDevice)
            }
            return
        }
        if (
            SeenAdvertiserPolicy.shouldForgetNearbyAfterIdentityUpdate(
                detected = false,
                action = action,
                hasUsableName = enriched.deviceName != null
            )
        ) {
            _nearbyDevices.value = nearbyDeviceTracker.forget(enriched.address)
        }
    }

    private fun wasSeenInClassicInquiry(address: String): Boolean {
        return address.isNotBlank() && classicInquiryAddresses.contains(address)
    }

    private fun clearClassicInquiryMemory() {
        classicInquiryAddresses.clear()
        classicInquiryRssi.clear()
        seenAdvertisers.clear()
    }

    @SuppressLint("MissingPermission")
    private fun handleClassicDiscoveryResult(intent: Intent) {
        val bluetoothDevice = intent.extractBluetoothDevice() ?: return
        val address = resolveDeviceAddress(bluetoothDevice)
        val extraRssi = intent.getShortExtra(
            BluetoothDevice.EXTRA_RSSI,
            Constants.UNKNOWN_RSSI_DBM.toShort()
        ).toInt()
        if (extraRssi != Constants.UNKNOWN_RSSI_DBM && address.isNotBlank()) {
            classicInquiryRssi[address] = extraRssi
        }
        val baseSignal = ClassicDiscoverySignal(
            deviceName = resolveCachedDeviceName(bluetoothDevice),
            extraName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME),
            alias = resolveDeviceAlias(bluetoothDevice),
            address = address,
            rssi = ClassicDiscoveryPolicy.resolveRssi(
                extraRssi = extraRssi,
                previouslySeenRssi = classicInquiryRssi[address]
            ),
            deviceClass = intent.extractBluetoothClass()?.deviceClass
                ?: resolveDeviceClass(bluetoothDevice)
        ).toDetectionSignal()
        handleDetectionSignal(baseSignal, action = intent.action)
    }

    @SuppressLint("MissingPermission")
    private fun resolveCachedDeviceName(device: BluetoothDevice): String? {
        if (!hasBluetoothConnectPermission()) {
            return null
        }

        return try {
            device.name
        } catch (_: SecurityException) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun resolveDeviceAlias(device: BluetoothDevice): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return null
        }
        if (!hasBluetoothConnectPermission()) {
            return null
        }

        return try {
            device.alias
        } catch (_: SecurityException) {
            null
        }
    }

    private fun resolveDeviceClass(device: BluetoothDevice): Int? {
        if (!hasBluetoothConnectPermission()) {
            return null
        }

        return try {
            device.bluetoothClass?.deviceClass
        } catch (_: SecurityException) {
            null
        }
    }

    private fun resolveDeviceAddress(result: ScanResult): String {
        if (!hasBluetoothConnectPermission()) {
            return ""
        }

        return try {
            result.device.address
        } catch (_: SecurityException) {
            ""
        }
    }

    private fun resolveDeviceAddress(device: BluetoothDevice): String {
        if (!hasBluetoothConnectPermission()) {
            return ""
        }

        return try {
            device.address
        } catch (_: SecurityException) {
            ""
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasRequiredScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                hasBluetoothConnectPermission()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun ensureClassicDiscoveryReceiverRegistered() {
        if (!isClassicDiscoveryReceiverRegistered.compareAndSet(false, true)) {
            return
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            addAction(BluetoothDevice.ACTION_CLASS_CHANGED)
        }
        ContextCompat.registerReceiver(
            context,
            classicDiscoveryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun unregisterClassicDiscoveryReceiver() {
        if (!isClassicDiscoveryReceiverRegistered.compareAndSet(true, false)) {
            return
        }

        context.unregisterReceiver(classicDiscoveryReceiver)
    }

    @SuppressLint("MissingPermission")
    private fun startClassicDiscoveryIfNeeded(immediate: Boolean = false) {
        if (!ClassicDiscoveryPolicy.shouldStartClassicDiscovery(classicDiscoveryStarted.get())) {
            return
        }
        if (!classicDiscoveryStarted.compareAndSet(false, true)) {
            return
        }
        classicDiscoveryJob?.cancel()
        val delayMs = ClassicDiscoveryPolicy.startDelayMs(immediate = immediate)
        classicDiscoveryJob = diagnosticPersistenceScope.launch {
            if (delayMs > 0L) {
                delay(delayMs)
            }
            if (
                userRequestedScanning.get() &&
                bluetoothAdapter?.isEnabled == true &&
                hasRequiredScanPermission() &&
                isLocationServicesSatisfied()
            ) {
                startClassicDiscovery()
            } else {
                classicDiscoveryStarted.set(false)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun startClassicDiscovery() {
        val adapter = bluetoothAdapter ?: run {
            classicDiscoveryStarted.set(false)
            return
        }
        if (!hasRequiredScanPermission() || !adapter.isEnabled || !isLocationServicesSatisfied()) {
            classicDiscoveryStarted.set(false)
            return
        }

        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
                delay(ClassicDiscoveryPolicy.cancelToRestartDelayMs())
            }
            if (!adapter.startDiscovery()) {
                Log.w(TAG, "Bluetooth Classic discovery did not start.")
                classicDiscoveryStarted.set(false)
                scheduleClassicDiscoveryRetry()
                return
            }
            lastClassicDiscoveryStartedAt.set(System.currentTimeMillis())
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to start Bluetooth Classic discovery", e)
            classicDiscoveryStarted.set(false)
            scheduleClassicDiscoveryRetry()
        }
    }

    private fun scheduleClassicDiscoveryRetry() {
        if (!userRequestedScanning.get()) {
            return
        }
        classicDiscoveryRetryJob?.cancel()
        classicDiscoveryRetryJob = diagnosticPersistenceScope.launch {
            delay(ClassicDiscoveryPolicy.failedStartRetryDelayMs())
            if (
                userRequestedScanning.get() &&
                bluetoothAdapter?.isEnabled == true &&
                hasRequiredScanPermission() &&
                isLocationServicesSatisfied()
            ) {
                startClassicDiscoveryIfNeeded(immediate = true)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopClassicDiscovery() {
        val adapter = bluetoothAdapter ?: return
        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to stop Bluetooth Classic discovery", e)
        }
    }
    
    @SuppressLint("MissingPermission")
    fun startScanning(sensitivity: ScanSensitivity) {
        if (bluetoothAdapter == null) {
            _isScanning.value = false
            _hardwareScanRunning.value = false
            throw IllegalStateException("Bluetooth adapter is unavailable.")
        }

        if (!hasRequiredScanPermission()) {
            _isScanning.value = false
            _hardwareScanRunning.value = false
            throw SecurityException("Bluetooth scan permission is missing.")
        }

        lastSensitivity = sensitivity
        val alreadyRequested = userRequestedScanning.getAndSet(true)
        if (ScanResumePolicy.shouldResetScanSession(alreadyRequested)) {
            usingExtendedAdvertising = true
            usingMatchAllFilter = true
            matchAllFilterRejectedThisSession = false
            classicDiscoveryStarted.set(false)
            lastClassicDiscoveryStartedAt.set(0L)
            clearClassicInquiryMemory()
            retryAttempt = 0
            detectionCooldownGate.clear()
            nearbyDeviceTracker.clear()
            diagnosticWriteGate.clear()
            _nearbyDevices.value = emptyList()
        }
        _isScanning.value = HardwareScanStatePolicy.isActive(
            userRequestedScanning = true,
            bluetoothEnabled = bluetoothAdapter.isEnabled,
            scanPermissionGranted = true,
            locationServicesSatisfied = isLocationServicesSatisfied()
        )
        ensureClassicDiscoveryReceiverRegistered()
        ensureBluetoothStateReceiverRegistered()
        ensureLocationModeReceiverRegistered()
        ensureScanPermissionWatch()
        startScanWatchdog()
        startNearbyPrune()

        if (!bluetoothAdapter.isEnabled || !isLocationServicesSatisfied()) {
            _hardwareScanRunning.value = false
            return
        }

        if (alreadyRequested) {
            ensureHardwareScanning()
        } else {
            startLeAndClassicScanning()
        }
    }

    fun ensureHardwareScanning() {
        if (!userRequestedScanning.get()) {
            return
        }
        refreshBleScan()
        refreshClassicDiscoveryIfNeeded()
    }

    fun updateSensitivity(sensitivity: ScanSensitivity) {
        if (lastSensitivity == sensitivity) {
            return
        }

        lastSensitivity = sensitivity
        if (userRequestedScanning.get() && bluetoothAdapter?.isEnabled == true && isLocationServicesSatisfied()) {
            refreshBleScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        userRequestedScanning.set(false)
        retryJob?.cancel()
        scanWatchdogJob?.cancel()
        nearbyPruneJob?.cancel()
        classicDiscoveryJob?.cancel()
        classicDiscoveryRetryJob?.cancel()
        retryJob = null
        scanWatchdogJob = null
        nearbyPruneJob = null
        classicDiscoveryJob = null
        classicDiscoveryRetryJob = null
        pauseHardwareScan()
        unregisterClassicDiscoveryReceiver()
        unregisterBluetoothStateReceiver()
        unregisterLocationModeReceiver()
        stopScanPermissionWatch()
        detectionCooldownGate.clear()
        diagnosticWriteGate.clear()
        classicDiscoveryStarted.set(false)
        lastClassicDiscoveryStartedAt.set(0L)
        clearClassicInquiryMemory()
        _nearbyDevices.value = nearbyDeviceTracker.clear()
    }

    @SuppressLint("MissingPermission")
    private fun startLeAndClassicScanning() {
        val adapter = bluetoothAdapter ?: return
        if (
            !userRequestedScanning.get() ||
            !adapter.isEnabled ||
            !hasRequiredScanPermission() ||
            !isLocationServicesSatisfied()
        ) {
            _isScanning.value = HardwareScanStatePolicy.isActive(
                userRequestedScanning = userRequestedScanning.get(),
                bluetoothEnabled = adapter.isEnabled,
                scanPermissionGranted = hasRequiredScanPermission(),
                locationServicesSatisfied = isLocationServicesSatisfied()
            )
            _hardwareScanRunning.value = false
            return
        }

        _isScanning.value = true

        val scanner = adapter.bluetoothLeScanner ?: run {
            Log.w(TAG, "Bluetooth LE scanner is unavailable.")
            _hardwareScanRunning.value = false
            scheduleScanRetry()
            return
        }

        try {
            scanner.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop previous BLE scan before restart", e)
        }
        _hardwareScanRunning.value = false

        try {
            startLeScan(scanner, usingExtendedAdvertising)
            markHardwareScanRunning()
            retryAttempt = 0
            startClassicDiscoveryIfNeeded()
        } catch (e: Exception) {
            if (usingExtendedAdvertising) {
                Log.w(TAG, "Extended BLE scan failed, retrying with legacy advertisements", e)
                usingExtendedAdvertising = false
                try {
                    startLeScan(scanner, extendedAdvertising = false)
                    markHardwareScanRunning()
                    retryAttempt = 0
                    startClassicDiscoveryIfNeeded()
                } catch (legacyError: Exception) {
                    Log.w(TAG, "Failed to start BLE scan", legacyError)
                    scheduleScanRetry()
                }
            } else {
                Log.w(TAG, "Failed to start BLE scan", e)
                scheduleScanRetry()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLeScan(
        scanner: BluetoothLeScanner,
        extendedAdvertising: Boolean
    ) {
        val settings = buildScanSettings(lastSensitivity, extendedAdvertising)
        if (usingMatchAllFilter) {
            try {
                scanner.startScan(matchAllScanFilters(), settings, scanCallback)
                return
            } catch (e: IllegalArgumentException) {
                when (
                    BleScanCompatibilityPolicy.nextStep(
                        usingMatchAllFilter = usingMatchAllFilter,
                        usingExtendedAdvertising = usingExtendedAdvertising
                    )
                ) {
                    BleScanCompatibilityStep.DISABLE_EXTENDED_ADVERTISING -> {
                        Log.w(TAG, "Match-all BLE scan with extended advertising was rejected, retrying legacy advertisements", e)
                        usingExtendedAdvertising = false
                        startLeScan(scanner, extendedAdvertising = false)
                        return
                    }
                    BleScanCompatibilityStep.DROP_MATCH_ALL_FILTER -> {
                        Log.w(TAG, "Match-all BLE scan filter was rejected, falling back to an unfiltered scan", e)
                        rejectMatchAllFilter()
                    }
                    BleScanCompatibilityStep.NONE -> throw e
                }
            }
        }
        scanner.startScan(null, settings, scanCallback)
    }

    private fun refreshClassicDiscoveryIfNeeded() {
        val refreshDue = ClassicDiscoveryPolicy.shouldRefreshClassicDiscovery(
            lastStartedAtMs = lastClassicDiscoveryStartedAt.get(),
            nowMs = System.currentTimeMillis(),
            intervalMs = ClassicDiscoveryPolicy.refreshIntervalMs(isAppInForeground())
        )
        if (refreshDue) {
            classicDiscoveryStarted.set(false)
        }
        startClassicDiscoveryIfNeeded(immediate = refreshDue)
    }

    @SuppressLint("MissingPermission")
    private fun pauseHardwareScan(
        bluetoothEnabled: Boolean? = null,
        locationServicesSatisfied: Boolean? = null,
        scanPermissionGranted: Boolean? = null
    ) {
        _isScanning.value = HardwareScanStatePolicy.isActive(
            userRequestedScanning = userRequestedScanning.get(),
            bluetoothEnabled = bluetoothEnabled ?: (bluetoothAdapter?.isEnabled == true),
            scanPermissionGranted = scanPermissionGranted ?: hasRequiredScanPermission(),
            locationServicesSatisfied = locationServicesSatisfied ?: isLocationServicesSatisfied()
        )
        _hardwareScanRunning.value = false
        scanEnvironmentSignals.notifyChanged()
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop BLE scan", e)
        }
        classicDiscoveryJob?.cancel()
        classicDiscoveryJob = null
        classicDiscoveryRetryJob?.cancel()
        classicDiscoveryRetryJob = null
        classicDiscoveryStarted.set(false)
        stopClassicDiscovery()
    }

    @SuppressLint("MissingPermission")
    private fun refreshBleScan() {
        if (
            !userRequestedScanning.get() ||
            bluetoothAdapter?.isEnabled != true ||
            !hasRequiredScanPermission() ||
            !isLocationServicesSatisfied()
        ) {
            return
        }

        try {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh BLE scan", e)
        }
        _hardwareScanRunning.value = false
        if (
            BleScanCompatibilityPolicy.shouldRestoreMatchAllFilterOnRefresh(
                usingMatchAllFilter = usingMatchAllFilter,
                matchAllRejectedThisSession = matchAllFilterRejectedThisSession
            )
        ) {
            usingMatchAllFilter = true
        }
        startLeAndClassicScanning()
    }

    private fun rejectMatchAllFilter() {
        usingMatchAllFilter = false
        matchAllFilterRejectedThisSession = true
    }

    private fun markHardwareScanRunning() {
        _hardwareScanRunning.value = true
    }

    private fun scheduleScanRetry() {
        if (!userRequestedScanning.get()) {
            return
        }

        retryJob?.cancel()
        retryAttempt += 1
        val delayMs = ScanFailurePolicy.retryDelayMs(retryAttempt)
        retryJob = diagnosticPersistenceScope.launch {
            delay(delayMs)
            if (
                userRequestedScanning.get() &&
                bluetoothAdapter?.isEnabled == true &&
                hasRequiredScanPermission() &&
                isLocationServicesSatisfied()
            ) {
                startLeAndClassicScanning()
            }
        }
    }

    private fun startScanWatchdog() {
        scanWatchdogJob?.cancel()
        scanWatchdogJob = diagnosticPersistenceScope.launch {
            while (isActive) {
                delay(BleScanRefreshPolicy.intervalMs(isAppInForeground()))
                if (
                    userRequestedScanning.get() &&
                    bluetoothAdapter?.isEnabled == true &&
                    hasRequiredScanPermission() &&
                    isLocationServicesSatisfied()
                ) {
                    refreshBleScan()
                    refreshClassicDiscoveryIfNeeded()
                }
            }
        }
    }

    private fun startNearbyPrune() {
        nearbyPruneJob?.cancel()
        nearbyPruneJob = diagnosticPersistenceScope.launch {
            while (isActive) {
                delay(Constants.NEARBY_DEVICE_PRUNE_INTERVAL_MS)
                if (userRequestedScanning.get()) {
                    pruneSeenAdvertisers()
                    _nearbyDevices.value = nearbyDeviceTracker.snapshot()
                }
            }
        }
    }

    private fun pruneSeenAdvertisers(nowMs: Long = System.currentTimeMillis()) {
        SeenAdvertiserPolicy.expiredAddresses(
            snapshots = seenAdvertisers,
            nowMs = nowMs,
            ttlMs = Constants.NEARBY_DEVICE_TTL_MS
        ).forEach { address ->
            seenAdvertisers.remove(address)
            classicInquiryAddresses.remove(address)
            classicInquiryRssi.remove(address)
        }
    }

    private fun rememberNearbyDevice(device: SmartGlassesDevice?) {
        if (device == null) {
            return
        }

        _nearbyDevices.value = nearbyDeviceTracker.record(device)
    }

    private fun ensureBluetoothStateReceiverRegistered() {
        if (!isBluetoothStateReceiverRegistered.compareAndSet(false, true)) {
            return
        }

        ContextCompat.registerReceiver(
            context,
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun unregisterBluetoothStateReceiver() {
        if (!isBluetoothStateReceiverRegistered.compareAndSet(true, false)) {
            return
        }

        context.unregisterReceiver(bluetoothStateReceiver)
    }

    private fun ensureLocationModeReceiverRegistered() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return
        }
        if (!isLocationModeReceiverRegistered.compareAndSet(false, true)) {
            return
        }

        ContextCompat.registerReceiver(
            context,
            locationModeReceiver,
            IntentFilter(LocationManager.MODE_CHANGED_ACTION),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun unregisterLocationModeReceiver() {
        if (!isLocationModeReceiverRegistered.compareAndSet(true, false)) {
            return
        }

        context.unregisterReceiver(locationModeReceiver)
    }

    private fun isLocationServicesSatisfied(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return true
        }

        val locationManager = context.getSystemService(LocationManager::class.java) ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun ensureScanPermissionWatch() {
        if (scanPermissionMonitor != null) {
            return
        }

        val monitor = ScanPermissionAppOpsMonitor(context) {
            if (!userRequestedScanning.get()) {
                return@ScanPermissionAppOpsMonitor
            }
            if (!hasRequiredScanPermission()) {
                pauseHardwareScan(scanPermissionGranted = false)
                _scanFailures.trySend(
                    BluetoothScanFailure(ScanFailurePolicy.SCAN_ENVIRONMENT_PERMISSION_DENIED)
                )
            } else if (!_isScanning.value) {
                scanEnvironmentSignals.notifyChanged()
                startLeAndClassicScanning()
            }
        }
        if (monitor.start()) {
            scanPermissionMonitor = monitor
        }
    }

    private fun stopScanPermissionWatch() {
        scanPermissionMonitor?.stop()
        scanPermissionMonitor = null
    }
    
    fun hasBleHardwareSupport(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    private fun persistDiagnosticLog(log: DiagnosticLog) {
        if (!log.hasPayload() || !diagnosticWriteGate.shouldWrite(log.deduplicationKey())) {
            return
        }

        diagnosticPersistenceScope.launch {
            diagnosticLogRepository.insertLog(log)
        }
    }

    private fun buildScanSettings(
        sensitivity: ScanSensitivity,
        extendedAdvertising: Boolean
    ): ScanSettings {
        val scanMode = when (sensitivity) {
            ScanSensitivity.LOW_POWER -> ScanSettings.SCAN_MODE_LOW_POWER
            ScanSensitivity.BALANCED -> ScanSettings.SCAN_MODE_BALANCED
            ScanSensitivity.HIGH_ACCURACY -> ScanSettings.SCAN_MODE_LOW_LATENCY
        }
        val matchMode = if (sensitivity == ScanSensitivity.LOW_POWER) {
            ScanSettings.MATCH_MODE_STICKY
        } else {
            ScanSettings.MATCH_MODE_AGGRESSIVE
        }
        val numOfMatches = if (sensitivity == ScanSensitivity.LOW_POWER) {
            ScanSettings.MATCH_NUM_FEW_ADVERTISEMENT
        } else {
            ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT
        }

        val builder = ScanSettings.Builder()
            .setScanMode(scanMode)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(matchMode)
            .setNumOfMatches(numOfMatches)
            .setReportDelay(0)
            .setLegacy(!extendedAdvertising)

        if (extendedAdvertising) {
            builder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
        }

        return builder.build()
    }

    private fun matchAllScanFilters(): List<ScanFilter> {
        return listOf(ScanFilter.Builder().build())
    }

    private fun isAppInForeground(): Boolean {
        return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    companion object {
        private const val TAG = "SmartGlassesDetector"
    }
}

private fun ByteArray.toHexString(): String {
    val builder = StringBuilder(size * 2)
    forEach { byte ->
        builder.append(byte.toInt().and(0xFF).toString(16).uppercase().padStart(2, '0'))
    }
    return builder.toString()
}

internal data class ProcessedScanSignal(
    val detectedDevice: SmartGlassesDevice?,
    val diagnosticLog: DiagnosticLog
)

internal class ScanSignalProcessor(
    private val classifier: SmartGlassesClassifier = SmartGlassesClassifier()
) {
    fun process(
        signal: DetectionSignal,
        sensitivity: ScanSensitivity = ScanSensitivity.BALANCED
    ): ProcessedScanSignal {
        return ProcessedScanSignal(
            detectedDevice = classifier.classify(signal, sensitivity),
            diagnosticLog = signal.toDiagnosticLog()
        )
    }

    fun detectDevice(
        signal: DetectionSignal,
        sensitivity: ScanSensitivity = ScanSensitivity.BALANCED
    ): SmartGlassesDevice? {
        return classifier.classify(signal, sensitivity)
    }
}

internal fun DetectionSignal.hasDiagnosticPayload(): Boolean {
    return address.isNotBlank() ||
        deviceName?.isNotBlank() == true ||
        companyIds.isNotEmpty() ||
        serviceUuids.isNotEmpty() ||
        advertisementDataHex.isNotBlank() ||
        extraPayloadHex.isNotBlank()
}

internal fun DetectionSignal.toDiagnosticLog(
    detectedAt: Long = System.currentTimeMillis()
): DiagnosticLog {
    return DiagnosticLog(
        advertisedName = deviceName.orEmpty(),
        deviceAddress = address,
        companyIds = companyIds
            .sorted()
            .joinToString(",") { companyId -> "0x${companyId.toString(16).uppercase().padStart(4, '0')}" },
        serviceUuids = serviceUuids.sorted().joinToString(","),
        advertisementDataHex = payloadHex(),
        rssi = rssi,
        detectedAt = detectedAt
    )
}

internal data class ClassicDiscoverySignal(
    val deviceName: String?,
    val address: String,
    val rssi: Int,
    val extraName: String? = null,
    val alias: String? = null,
    val deviceClass: Int? = null
) {
    fun toDetectionSignal(): DetectionSignal {
        return DetectionSignal(
            deviceName = BluetoothAdvertisedNamePolicy.resolve(
                extraName,
                deviceName,
                alias
            ),
            address = address,
            companyIds = emptySet(),
            rssi = rssi,
            deviceClass = deviceClass
        )
    }
}

internal fun ClassicDiscoverySignal.toDiagnosticLog(
    detectedAt: Long = System.currentTimeMillis()
): DiagnosticLog {
    return toDetectionSignal().toDiagnosticLog(detectedAt)
}

private fun Intent.extractBluetoothDevice(): BluetoothDevice? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }
}

private fun Intent.extractBluetoothClass(): BluetoothClass? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_CLASS, BluetoothClass::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(BluetoothDevice.EXTRA_CLASS)
    }
}
