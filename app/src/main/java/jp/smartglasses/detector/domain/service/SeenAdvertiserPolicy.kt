package jp.smartglasses.detector.domain.service

import jp.smartglasses.detector.util.Constants

data class SeenAdvertiserSnapshot(
    val rssi: Int = Constants.UNKNOWN_RSSI_DBM,
    val companyIds: Set<Int> = emptySet(),
    val serviceUuids: List<String> = emptyList(),
    val appearance: Int? = null,
    val deviceClass: Int? = null,
    val deviceName: String? = null,
    val lastSeenAtMs: Long = 0L
)

object SeenAdvertiserPolicy {
    fun merge(
        existing: SeenAdvertiserSnapshot?,
        rssi: Int,
        companyIds: Set<Int> = emptySet(),
        serviceUuids: List<String> = emptyList(),
        appearance: Int? = null,
        deviceClass: Int? = null,
        deviceName: String? = null,
        nowMs: Long = 0L
    ): SeenAdvertiserSnapshot {
        return SeenAdvertiserSnapshot(
            rssi = if (rssi != Constants.UNKNOWN_RSSI_DBM) {
                rssi
            } else {
                existing?.rssi ?: Constants.UNKNOWN_RSSI_DBM
            },
            companyIds = (existing?.companyIds ?: emptySet()) + companyIds,
            serviceUuids = ((existing?.serviceUuids ?: emptyList()) + serviceUuids).distinct(),
            appearance = appearance ?: existing?.appearance,
            deviceClass = deviceClass ?: existing?.deviceClass,
            deviceName = BluetoothAdvertisedNamePolicy.resolve(
                deviceName,
                existing?.deviceName
            ),
            lastSeenAtMs = if (nowMs > 0L) {
                nowMs
            } else {
                existing?.lastSeenAtMs ?: 0L
            }
        )
    }

    fun isExpired(lastSeenAtMs: Long, nowMs: Long, ttlMs: Long): Boolean {
        if (ttlMs <= 0L) {
            return false
        }
        return nowMs - lastSeenAtMs >= ttlMs
    }

    fun expiredAddresses(
        snapshots: Map<String, SeenAdvertiserSnapshot>,
        nowMs: Long,
        ttlMs: Long
    ): List<String> {
        return snapshots.mapNotNull { (address, snapshot) ->
            address.takeIf { isExpired(snapshot.lastSeenAtMs, nowMs, ttlMs) }
        }
    }

    fun shouldForgetNearbyAfterDelayedUpdate(
        action: String?,
        detected: Boolean
    ): Boolean {
        return shouldForgetNearbyAfterIdentityUpdate(
            detected = detected,
            action = action,
            hasUsableName = false
        )
    }

    fun shouldForgetNearbyAfterIdentityUpdate(
        detected: Boolean,
        action: String? = null,
        hasUsableName: Boolean = false
    ): Boolean {
        if (detected) {
            return false
        }
        if (hasUsableName) {
            return true
        }
        return action == ClassicDiscoveryPolicy.ACTION_NAME_CHANGED ||
            action == ClassicDiscoveryPolicy.ACTION_CLASS_CHANGED
    }
}
