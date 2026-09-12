package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants

object ClassicDiscoveryPolicy {
    const val ACTION_FOUND = "android.bluetooth.device.action.FOUND"
    const val ACTION_NAME_CHANGED = "android.bluetooth.device.action.NAME_CHANGED"
    const val ACTION_CLASS_CHANGED = "android.bluetooth.device.action.CLASS_CHANGED"
    const val ACTION_UUID = "android.bluetooth.device.action.UUID"
    const val ACTION_ALIAS_CHANGED = "android.bluetooth.device.action.ALIAS_CHANGED"
    const val ACTION_DISCOVERY_FINISHED = "android.bluetooth.adapter.action.DISCOVERY_FINISHED"

    fun startDelayMs(immediate: Boolean): Long {
        return if (immediate) {
            0L
        } else {
            Constants.CLASSIC_DISCOVERY_DELAY_MS
        }
    }

    fun cancelToRestartDelayMs(): Long {
        return 800L
    }

    fun failedStartRetryDelayMs(): Long {
        return 10_000L
    }

    fun shouldStartClassicDiscovery(alreadyStartedThisSession: Boolean): Boolean {
        return !alreadyStartedThisSession
    }

    fun shouldRefreshClassicDiscovery(
        lastStartedAtMs: Long,
        nowMs: Long,
        intervalMs: Long
    ): Boolean {
        if (lastStartedAtMs <= 0L || intervalMs <= 0L) {
            return false
        }
        return nowMs - lastStartedAtMs >= intervalMs
    }

    fun refreshIntervalMs(appInForeground: Boolean): Long {
        return if (appInForeground) {
            Constants.CLASSIC_DISCOVERY_REFRESH_INTERVAL_MS
        } else {
            Constants.BLE_SCAN_REFRESH_INTERVAL_MS
        }
    }

    fun shouldAttemptAfterBleLaunchFailure(): Boolean {
        return true
    }

    fun shouldRememberSeenAdvertiser(address: String): Boolean {
        return address.isNotBlank()
    }

    fun shouldExportDiscoveryReceiver(): Boolean {
        // Android 13 以降、一部 OEM は NOT_EXPORTED だと機器・接続ブロードキャストを落とす
        return true
    }

    fun inquiryBroadcastActions(): List<String> {
        return listOf(
            ACTION_FOUND,
            ACTION_NAME_CHANGED,
            ACTION_CLASS_CHANGED,
            ACTION_UUID,
            ACTION_ALIAS_CHANGED,
            ACTION_DISCOVERY_FINISHED
        )
    }

    fun shouldPollConnectedAfterDiscoveryFinished(
        action: String?,
        scanningRequested: Boolean
    ): Boolean {
        return scanningRequested && action == ACTION_DISCOVERY_FINISHED
    }

    fun shouldRememberInquiryAdvertiser(
        action: String?,
        scanningRequested: Boolean,
        fromConnection: Boolean,
        classify: Boolean
    ): Boolean {
        if (!scanningRequested) {
            return false
        }
        if (fromConnection || action == ACTION_FOUND) {
            return true
        }
        return classify && action != ACTION_DISCOVERY_FINISHED
    }

    fun shouldApplyInquiryUpdate(
        action: String?,
        scanningRequested: Boolean,
        alreadySeenAddress: Boolean,
        deviceClass: Int? = null,
        extraRssi: Int = Constants.UNKNOWN_RSSI_DBM
    ): Boolean {
        if (!scanningRequested) {
            return false
        }

        val nearbyOrGlasses = alreadySeenAddress ||
            BluetoothDeviceClassPolicy.isGlassesDeviceClass(deviceClass) ||
            extraRssi != Constants.UNKNOWN_RSSI_DBM

        return when (action) {
            ACTION_FOUND -> true
            // Inquiry の遅延名前は RSSI 付きなら FOUND 相当。Glasses CoD は未観測でも例外。
            ACTION_NAME_CHANGED -> nearbyOrGlasses
            ACTION_UUID ->
                alreadySeenAddress || BluetoothDeviceClassPolicy.isGlassesDeviceClass(deviceClass)
            // エイリアス変更は端末ローカルなので、近くで見た機器に限る
            ACTION_ALIAS_CHANGED -> alreadySeenAddress
            ACTION_CLASS_CHANGED ->
                alreadySeenAddress || BluetoothDeviceClassPolicy.isGlassesDeviceClass(deviceClass)
            else -> false
        }
    }

    fun resolveRssi(extraRssi: Int, previouslySeenRssi: Int?): Int {
        return if (extraRssi != Constants.UNKNOWN_RSSI_DBM) {
            extraRssi
        } else {
            previouslySeenRssi ?: Constants.UNKNOWN_RSSI_DBM
        }
    }
}
