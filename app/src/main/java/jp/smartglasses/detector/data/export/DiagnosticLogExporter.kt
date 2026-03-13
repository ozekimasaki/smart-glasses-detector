package jp.smartglasses.detector.data.export

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.smartglasses.detector.domain.repository.DiagnosticLogRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticLogExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnosticLogRepository: DiagnosticLogRepository
) {
    suspend fun exportLatestLogs(limit: Int = DEFAULT_EXPORT_LIMIT): Uri? {
        val logs = diagnosticLogRepository.getLatestLogs(limit)
        if (logs.isEmpty()) {
            return null
        }

        val exportDir = File(context.cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }
        val fileName = "diagnostic-logs-${timestampFormatter.format(Date())}.json"
        val exportFile = File(exportDir, fileName)
        exportFile.writeText(buildPayload(logs).toString(2))

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )
    }

    private fun buildPayload(logs: List<jp.smartglasses.detector.domain.model.DiagnosticLog>): JSONObject {
        val logArray = JSONArray()
        logs.forEach { log ->
            logArray.put(
                JSONObject()
                    .put("advertisedName", log.advertisedName)
                    .put("deviceAddressHash", hashAddress(log.deviceAddress))
                    .put("companyIds", splitCsv(log.companyIds))
                    .put("serviceUuids", splitCsv(log.serviceUuids))
                    .put("advertisementDataHex", log.advertisementDataHex)
                    .put("rssi", log.rssi)
                    .put("detectedAt", log.detectedAt)
            )
        }

        return JSONObject()
            .put("appPackage", context.packageName)
            .put("appVersion", resolveVersionName())
            .put("exportedAt", System.currentTimeMillis())
            .put("logCount", logs.size)
            .put("logs", logArray)
    }

    private fun splitCsv(value: String): JSONArray {
        val array = JSONArray()
        value.split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach(array::put)
        return array
    }

    private fun hashAddress(address: String): String {
        if (address.isBlank()) {
            return ""
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(address.trim().uppercase().toByteArray())
        return bytes.joinToString("") { byte ->
            byte.toInt().and(0xFF).toString(16).padStart(2, '0')
        }.take(16)
    }

    private fun resolveVersionName(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName ?: "unknown"
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
            }
        } catch (_: Exception) {
            "unknown"
        }
    }

    private companion object {
        const val DEFAULT_EXPORT_LIMIT = 200
        const val EXPORT_DIRECTORY = "shared"
        val timestampFormatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
    }
}
