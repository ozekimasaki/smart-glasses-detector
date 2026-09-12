package jp.smartglasses.detector.data.bluetooth

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.util.Log

internal object ScanPermissionAppOps {
    fun usesFineLocationOp(sdkInt: Int): Boolean {
        return sdkInt < Build.VERSION_CODES.S
    }

    fun permissionsToWatch(sdkInt: Int): List<String> {
        if (usesFineLocationOp(sdkInt)) {
            return emptyList()
        }

        return listOf(
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH_CONNECT"
        )
    }
}

internal class ScanPermissionAppOpsMonitor(
    private val context: Context,
    private val onChanged: () -> Unit
) {
    private var listener: AppOpsManager.OnOpChangedListener? = null

    fun start(): Boolean {
        if (listener != null) {
            return true
        }

        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val watcher = AppOpsManager.OnOpChangedListener { _, packageName ->
            if (packageName != null && packageName != context.packageName) {
                return@OnOpChangedListener
            }
            onChanged()
        }
        listener = watcher

        return try {
            if (ScanPermissionAppOps.usesFineLocationOp(Build.VERSION.SDK_INT)) {
                appOps.startWatchingMode(
                    AppOpsManager.OPSTR_FINE_LOCATION,
                    context.packageName,
                    watcher
                )
            } else {
                ScanPermissionAppOps.permissionsToWatch(Build.VERSION.SDK_INT).forEach { permission ->
                    watch(appOps, permission, watcher)
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to watch scan permission changes", e)
            listener = null
            false
        }
    }

    fun stop() {
        val watcher = listener ?: return
        listener = null
        try {
            context.getSystemService(AppOpsManager::class.java)?.stopWatchingMode(watcher)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop watching scan permission changes", e)
        }
    }

    private fun watch(
        appOps: AppOpsManager,
        permission: String,
        watcher: AppOpsManager.OnOpChangedListener
    ) {
        val op = AppOpsManager.permissionToOp(permission) ?: return
        appOps.startWatchingMode(op, context.packageName, watcher)
    }

    companion object {
        private const val TAG = "ScanPermissionAppOps"
    }
}
