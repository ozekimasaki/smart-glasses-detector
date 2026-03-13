package jp.smartglasses.detector.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.smartglasses.detector.domain.model.DiagnosticLog
import jp.smartglasses.detector.domain.model.SmartGlassesDevice
import jp.smartglasses.detector.util.ScanSensitivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartGlassesDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    private val _scannedDevices = Channel<SmartGlassesDevice>(capacity = Channel.BUFFERED)
    val scannedDevices: Flow<SmartGlassesDevice> = _scannedDevices.receiveAsFlow()
    private val _diagnosticLogs = Channel<DiagnosticLog>(capacity = Channel.BUFFERED)
    val diagnosticLogs: Flow<DiagnosticLog> = _diagnosticLogs.receiveAsFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    private val detectionCooldownGate = DetectionCooldownGate()
    private val classifier = SmartGlassesClassifier()
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val signal = extractSignal(result) ?: return
            val device = classifier.classify(signal)
            if (device != null) {
                if (shouldEmitDetection(device)) {
                    _scannedDevices.trySend(device)
                }
                return
            }

            val diagnosticLog = buildDiagnosticLog(signal) ?: return
            if (shouldEmitDiagnosticLog(diagnosticLog)) {
                _diagnosticLogs.trySend(diagnosticLog)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
        }
    }
    
    fun detectSmartGlasses(result: ScanResult): SmartGlassesDevice? {
        return extractSignal(result)?.let(classifier::classify)
    }

    private fun extractSignal(result: ScanResult): DetectionSignal? {
        val scanRecord = result.scanRecord ?: return null
        val device = result.device
        return DetectionSignal(
            deviceName = resolveDeviceName(result, scanRecord),
            address = resolveDeviceAddress(result),
            companyIds = extractCompanyIds(scanRecord),
            rssi = result.rssi,
            serviceUuids = scanRecord.serviceUuids?.map { it.toString() }.orEmpty(),
            advertisementDataHex = scanRecord.bytes?.toHexString().orEmpty()
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

    private fun buildDiagnosticLog(signal: DetectionSignal): DiagnosticLog? {
        val hasDiagnosticSignal = signal.deviceName?.isNotBlank() == true ||
            signal.companyIds.isNotEmpty() ||
            signal.serviceUuids.isNotEmpty()
        if (!hasDiagnosticSignal) {
            return null
        }

        return DiagnosticLog(
            advertisedName = signal.deviceName.orEmpty(),
            deviceAddress = signal.address,
            companyIds = signal.companyIds
                .sorted()
                .joinToString(",") { companyId -> "0x${companyId.toString(16).uppercase().padStart(4, '0')}" },
            serviceUuids = signal.serviceUuids.sorted().joinToString(","),
            advertisementDataHex = signal.advertisementDataHex,
            rssi = signal.rssi,
            detectedAt = System.currentTimeMillis()
        )
    }

    private fun shouldEmitDetection(device: SmartGlassesDevice): Boolean {
        return detectionCooldownGate.shouldEmitDetection(
            deviceKey = buildDeviceKey(device),
            manufacturerKey = buildManufacturerKey(device)
        )
    }

    private fun shouldEmitDiagnosticLog(log: DiagnosticLog): Boolean {
        return detectionCooldownGate.shouldEmitDiagnostic(buildDiagnosticKey(log))
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

    private fun buildDiagnosticKey(log: DiagnosticLog): String {
        val normalizedAddress = log.deviceAddress.trim().uppercase()
        if (normalizedAddress.isNotEmpty()) {
            return "address:$normalizedAddress"
        }

        val normalizedName = log.advertisedName.trim().lowercase()
        val normalizedCompanyIds = log.companyIds.trim().lowercase()
        return "fallback:$normalizedName:$normalizedCompanyIds"
    }

    private fun resolveDeviceName(result: ScanResult, scanRecord: ScanRecord): String? {
        if (!hasBluetoothConnectPermission()) {
            return scanRecord.deviceName
        }

        return try {
            result.device.name ?: scanRecord.deviceName
        } catch (_: SecurityException) {
            scanRecord.deviceName
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

    private fun hasBluetoothConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
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

        detectionCooldownGate.clear()
        
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
        } catch (e: Exception) {
            _isScanning.value = false
            throw e
        }
    }
    
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop BLE scan", e)
        } finally {
            detectionCooldownGate.clear()
            _isScanning.value = false
        }
    }
    
    fun hasBleHardwareSupport(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
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
