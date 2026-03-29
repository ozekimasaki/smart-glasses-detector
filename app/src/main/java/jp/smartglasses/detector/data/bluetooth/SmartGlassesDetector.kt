package jp.smartglasses.detector.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.smartglasses.detector.domain.model.BluetoothScanFailure
import jp.smartglasses.detector.domain.model.DiagnosticLog
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.domain.repository.DiagnosticLogRepository
import jp.smartglasses.detector.util.ScanSensitivity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
                    if (_isScanning.value && isClassicDiscoveryReceiverRegistered.get()) {
                        startClassicDiscovery()
                    }
                }
            }
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val signal = extractSignal(result)
            val processedSignal = scanSignalProcessor.process(signal)

            val diagnosticLog = processedSignal.diagnosticLog
            persistDiagnosticLog(diagnosticLog)

            val device = processedSignal.detectedDevice
            if (device != null && shouldEmitDetection(device)) {
                _scannedDevices.trySend(device)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _scanFailures.trySend(BluetoothScanFailure(errorCode))
        }
    }
    
    fun detectSmartGlasses(result: ScanResult): SmartGlassesDevice? {
        return scanSignalProcessor.detectDevice(extractSignal(result))
    }

    private fun extractSignal(result: ScanResult): DetectionSignal {
        val scanRecord = result.scanRecord
        return DetectionSignal(
            deviceName = resolveDeviceName(result, scanRecord),
            address = resolveDeviceAddress(result),
            companyIds = scanRecord?.let(::extractCompanyIds).orEmpty(),
            rssi = result.rssi,
            serviceUuids = scanRecord?.serviceUuids?.map { it.toString() }.orEmpty(),
            advertisementDataHex = scanRecord?.bytes?.toHexString().orEmpty()
        )
    }

    private fun extractCompanyIds(scanRecord: ScanRecord): Set<Int> {
        val companyIds = mutableSetOf<Int>()
        val manufacturerSpecificData = scanRecord.manufacturerSpecificData
        for (index in 0 until manufacturerSpecificData.size()) {
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
        val device = intent.extractBluetoothDevice() ?: return
        val diagnosticLog = ClassicDiscoverySignal(
            deviceName = resolveDeviceName(device),
            address = resolveDeviceAddress(device),
            rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, UNKNOWN_CLASSIC_RSSI.toShort()).toInt()
        ).toDiagnosticLog()

        persistDiagnosticLog(diagnosticLog)
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
        if (!hasRequiredScanPermission()) {
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

        if (!bluetoothAdapter.isEnabled) {
            _isScanning.value = false
            throw IllegalStateException("Bluetooth is disabled.")
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            _isScanning.value = false
            throw IllegalStateException("Bluetooth LE scanner is unavailable.")
        }

        if (!hasRequiredScanPermission()) {
            _isScanning.value = false
            throw SecurityException("Bluetooth scan permission is missing.")
        }

        detectionCooldownGate.clear()
        ensureClassicDiscoveryReceiverRegistered()
        
        val settings = when (sensitivity) {
            ScanSensitivity.LOW_POWER -> ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build()
            ScanSensitivity.BALANCED -> ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build()
            ScanSensitivity.HIGH_ACCURACY -> ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
        }
        
        try {
            scanner.startScan(null, settings, scanCallback)
            _isScanning.value = true
            startClassicDiscovery()
        } catch (e: Exception) {
            _isScanning.value = false
            stopClassicDiscovery()
            unregisterClassicDiscoveryReceiver()
            throw e
        }
    }
    
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        _isScanning.value = false
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop BLE scan", e)
        } finally {
            stopClassicDiscovery()
            unregisterClassicDiscoveryReceiver()
            detectionCooldownGate.clear()
        }
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

    companion object {
        private const val TAG = "SmartGlassesDetector"
        private const val UNKNOWN_CLASSIC_RSSI = -127
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
    fun process(signal: DetectionSignal): ProcessedScanSignal {
        return ProcessedScanSignal(
            detectedDevice = classifier.classify(signal),
            diagnosticLog = signal.toDiagnosticLog()
        )
    }

    fun detectDevice(signal: DetectionSignal): SmartGlassesDevice? {
        return classifier.classify(signal)
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
