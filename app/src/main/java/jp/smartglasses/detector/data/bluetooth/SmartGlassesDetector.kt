package jp.smartglasses.detector.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.util.size
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.smartglasses.detector.domain.model.BluetoothScanFailure
import jp.smartglasses.detector.domain.model.DiagnosticLog
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.domain.repository.DiagnosticLogRepository
import jp.smartglasses.detector.domain.service.ScanFailurePolicy
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartGlassesDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val diagnosticLogRepository: DiagnosticLogRepository
) {
    private val _scannedDevices = Channel<SmartGlassesDevice>(capacity = Channel.BUFFERED)
    val scannedDevices: Flow<SmartGlassesDevice> = _scannedDevices.receiveAsFlow()
    private val _scanFailures = Channel<BluetoothScanFailure>(capacity = Channel.BUFFERED)
    val scanFailures: Flow<BluetoothScanFailure> = _scanFailures.receiveAsFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    private val detectionCooldownGate = DetectionCooldownGate()
    private val scanSignalProcessor = ScanSignalProcessor()
    private val isClassicDiscoveryReceiverRegistered = AtomicBoolean(false)
    private val isBluetoothStateReceiverRegistered = AtomicBoolean(false)
    private val userRequestedScanning = AtomicBoolean(false)
    private var lastSensitivity: ScanSensitivity = ScanSensitivity.BALANCED
    private var retryAttempt = 0
    private var scanWatchdogJob: Job? = null
    private var retryJob: Job? = null
    private val diagnosticPersistenceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "Failed to persist diagnostic log", throwable)
        }
    )

    private val classicDiscoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> handleClassicDiscoveryResult(intent)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (userRequestedScanning.get() && isClassicDiscoveryReceiverRegistered.get()) {
                        startClassicDiscovery()
                    }
                }
            }
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) {
                return
            }

            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_ON -> {
                    if (userRequestedScanning.get()) {
                        startLeAndClassicScanning()
                    }
                }
                BluetoothAdapter.STATE_OFF,
                BluetoothAdapter.STATE_TURNING_OFF -> pauseHardwareScan()
            }
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val signal = extractSignal(result)
            val processedSignal = scanSignalProcessor.process(signal, lastSensitivity)

            val diagnosticLog = processedSignal.diagnosticLog
            persistDiagnosticLog(diagnosticLog)

            val device = processedSignal.detectedDevice
            if (device != null && shouldEmitDetection(device)) {
                _scannedDevices.trySend(device)
            }
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
            deviceName = resolveDeviceName(result, scanRecord)
                ?: parsedAdvertisement.completeName
                ?: parsedAdvertisement.shortName,
            address = resolveDeviceAddress(result),
            companyIds = (scanRecord?.let(::extractCompanyIds).orEmpty()) + parsedAdvertisement.companyIds,
            rssi = result.rssi,
            serviceUuids = BleUuid.merge(
                scanRecord?.serviceUuids?.map { uuid -> uuid.toString() }.orEmpty(),
                scanRecord?.serviceData?.keys?.map { uuid -> uuid.toString() }.orEmpty(),
                parsedAdvertisement.serviceUuids
            ),
            advertisementDataHex = scanRecord?.bytes?.toHexString().orEmpty(),
            appearance = parsedAdvertisement.appearance
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

    @SuppressLint("MissingPermission")
    private fun handleClassicDiscoveryResult(intent: Intent) {
        val bluetoothDevice = intent.extractBluetoothDevice() ?: return
        val signal = DetectionSignal(
            deviceName = resolveDeviceName(bluetoothDevice),
            address = resolveDeviceAddress(bluetoothDevice),
            companyIds = emptySet(),
            rssi = intent.getShortExtra(
                BluetoothDevice.EXTRA_RSSI,
                Constants.UNKNOWN_RSSI_DBM.toShort()
            ).toInt()
        )
        val processed = scanSignalProcessor.process(signal, lastSensitivity)
        persistDiagnosticLog(processed.diagnosticLog)

        val detectedDevice = processed.detectedDevice
        if (detectedDevice != null && shouldEmitDetection(detectedDevice)) {
            _scannedDevices.trySend(detectedDevice)
        }
    }

    private fun resolveDeviceName(result: ScanResult, scanRecord: ScanRecord?): String? {
        if (!hasBluetoothConnectPermission()) {
            return scanRecord?.deviceName
        }

        return try {
            result.device.name ?: scanRecord?.deviceName
        } catch (_: SecurityException) {
            scanRecord?.deviceName
        }
    }

    private fun resolveDeviceName(device: BluetoothDevice): String? {
        if (!hasBluetoothConnectPermission()) {
            return null
        }

        return try {
            device.name
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
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }

        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureClassicDiscoveryReceiverRegistered() {
        if (!isClassicDiscoveryReceiverRegistered.compareAndSet(false, true)) {
            return
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
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
    private fun startClassicDiscovery() {
        val adapter = bluetoothAdapter ?: return
        if (!hasRequiredScanPermission() || !adapter.isEnabled) {
            return
        }

        try {
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            if (!adapter.startDiscovery()) {
                Log.w(TAG, "Bluetooth Classic discovery did not start.")
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Failed to start Bluetooth Classic discovery", e)
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
            throw IllegalStateException("Bluetooth adapter is unavailable.")
        }

        if (!hasRequiredScanPermission()) {
            _isScanning.value = false
            throw SecurityException("Bluetooth scan permission is missing.")
        }

        lastSensitivity = sensitivity
        userRequestedScanning.set(true)
        _isScanning.value = true
        retryAttempt = 0
        detectionCooldownGate.clear()
        ensureClassicDiscoveryReceiverRegistered()
        ensureBluetoothStateReceiverRegistered()
        startScanWatchdog()

        if (!bluetoothAdapter.isEnabled) {
            return
        }

        startLeAndClassicScanning()
    }

    fun updateSensitivity(sensitivity: ScanSensitivity) {
        if (lastSensitivity == sensitivity) {
            return
        }

        lastSensitivity = sensitivity
        if (userRequestedScanning.get() && bluetoothAdapter?.isEnabled == true) {
            refreshBleScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        userRequestedScanning.set(false)
        retryJob?.cancel()
        scanWatchdogJob?.cancel()
        retryJob = null
        scanWatchdogJob = null
        pauseHardwareScan()
        unregisterClassicDiscoveryReceiver()
        unregisterBluetoothStateReceiver()
        detectionCooldownGate.clear()
    }

    @SuppressLint("MissingPermission")
    private fun startLeAndClassicScanning() {
        val adapter = bluetoothAdapter ?: return
        if (!userRequestedScanning.get() || !adapter.isEnabled || !hasRequiredScanPermission()) {
            return
        }

        val scanner = adapter.bluetoothLeScanner ?: run {
            Log.w(TAG, "Bluetooth LE scanner is unavailable.")
            scheduleScanRetry()
            return
        }

        try {
            startLeScan(scanner, extendedAdvertising = true)
            retryAttempt = 0
            startClassicDiscovery()
        } catch (e: Exception) {
            Log.w(TAG, "Extended BLE scan failed, retrying with legacy advertisements", e)
            try {
                startLeScan(scanner, extendedAdvertising = false)
                retryAttempt = 0
                startClassicDiscovery()
            } catch (legacyError: Exception) {
                Log.w(TAG, "Failed to start BLE scan", legacyError)
                scheduleScanRetry()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLeScan(
        scanner: BluetoothLeScanner,
        extendedAdvertising: Boolean
    ) {
        scanner.startScan(null, buildScanSettings(lastSensitivity, extendedAdvertising), scanCallback)
    }

    @SuppressLint("MissingPermission")
    private fun pauseHardwareScan() {
        if (!userRequestedScanning.get()) {
            _isScanning.value = false
        }
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop BLE scan", e)
        }
        stopClassicDiscovery()
    }

    @SuppressLint("MissingPermission")
    private fun refreshBleScan() {
        if (!userRequestedScanning.get() || bluetoothAdapter?.isEnabled != true) {
            return
        }

        try {
            bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh BLE scan", e)
        }
        startLeAndClassicScanning()
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
            if (userRequestedScanning.get() && bluetoothAdapter?.isEnabled == true) {
                startLeAndClassicScanning()
            }
        }
    }

    private fun startScanWatchdog() {
        scanWatchdogJob?.cancel()
        scanWatchdogJob = diagnosticPersistenceScope.launch {
            while (isActive) {
                delay(Constants.BLE_SCAN_REFRESH_INTERVAL_MS)
                if (userRequestedScanning.get() && bluetoothAdapter?.isEnabled == true) {
                    refreshBleScan()
                }
            }
        }
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
    
    fun hasBleHardwareSupport(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    private fun persistDiagnosticLog(log: DiagnosticLog) {
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
        val numOfMatches = if (sensitivity == ScanSensitivity.HIGH_ACCURACY) {
            ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT
        } else {
            ScanSettings.MATCH_NUM_FEW_ADVERTISEMENT
        }

        val builder = ScanSettings.Builder()
            .setScanMode(scanMode)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(matchMode)
            .setNumOfMatches(numOfMatches)
            .setLegacy(!extendedAdvertising)

        if (extendedAdvertising) {
            builder.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
        }

        return builder.build()
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
        advertisementDataHex.isNotBlank()
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
        advertisementDataHex = advertisementDataHex,
        rssi = rssi,
        detectedAt = detectedAt
    )
}

internal data class ClassicDiscoverySignal(
    val deviceName: String?,
    val address: String,
    val rssi: Int
)

internal fun ClassicDiscoverySignal.toDiagnosticLog(
    detectedAt: Long = System.currentTimeMillis()
): DiagnosticLog {
    return DiagnosticLog(
        advertisedName = deviceName.orEmpty(),
        deviceAddress = address,
        companyIds = "",
        serviceUuids = "",
        advertisementDataHex = "",
        rssi = rssi,
        detectedAt = detectedAt
    )
}

private fun Intent.extractBluetoothDevice(): BluetoothDevice? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }
}
