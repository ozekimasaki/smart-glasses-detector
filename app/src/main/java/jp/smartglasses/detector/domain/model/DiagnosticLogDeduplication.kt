package jp.smartglasses.detector.domain.model

internal fun DiagnosticLog.hasPayload(): Boolean {
    return deviceAddress.isNotBlank() ||
        advertisedName.isNotBlank() ||
        companyIds.isNotBlank() ||
        serviceUuids.isNotBlank() ||
        advertisementDataHex.isNotBlank()
}

internal fun DiagnosticLog.deduplicationKey(): String {
    val normalizedAddress = deviceAddress.trim().uppercase()
    if (normalizedAddress.isNotEmpty()) {
        return "address:$normalizedAddress"
    }

    val normalizedName = advertisedName.trim().lowercase()
    val normalizedCompanyIds = companyIds.trim().lowercase()
    val normalizedServiceUuids = serviceUuids.trim().lowercase()
    val normalizedAdvertisementData = advertisementDataHex.trim().lowercase()
    return "fallback:$normalizedName:$normalizedCompanyIds:$normalizedServiceUuids:$normalizedAdvertisementData"
}
