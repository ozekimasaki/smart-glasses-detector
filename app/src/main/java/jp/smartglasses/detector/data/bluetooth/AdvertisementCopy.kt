package jp.smartglasses.detector.data.bluetooth

internal object AdvertisementCopy {
    fun copyBytes(bytes: ByteArray?): ByteArray {
        return bytes?.copyOf() ?: byteArrayOf()
    }

    fun copyMap(entries: Map<Int, ByteArray>?): Map<Int, ByteArray> {
        if (entries.isNullOrEmpty()) {
            return emptyMap()
        }
        val snapshot = HashMap<Int, ByteArray>(entries.size)
        for ((type, data) in entries) {
            snapshot[type] = data.copyOf()
        }
        return snapshot
    }

    fun extraPayload(
        manufacturerEntries: List<Pair<Int, ByteArray>>,
        serviceDataValues: Collection<ByteArray> = emptyList()
    ): ByteArray {
        val parts = ArrayList<ByteArray>(manufacturerEntries.size + serviceDataValues.size)
        var totalSize = 0
        for ((companyId, payload) in manufacturerEntries) {
            val encoded = AdvertisementParser.encodeManufacturerSpecificTlvBytes(
                companyId = companyId,
                payload = payload.copyOf()
            )
            parts += encoded
            totalSize += encoded.size
        }
        for (value in serviceDataValues) {
            val copied = value.copyOf()
            if (copied.isEmpty()) {
                continue
            }
            parts += copied
            totalSize += copied.size
        }
        if (totalSize == 0) {
            return byteArrayOf()
        }
        val merged = ByteArray(totalSize)
        var offset = 0
        for (part in parts) {
            System.arraycopy(part, 0, merged, offset, part.size)
            offset += part.size
        }
        return merged
    }

    fun copyManufacturerEntries(
        entries: List<Pair<Int, ByteArray>>
    ): List<Pair<Int, ByteArray>> {
        if (entries.isEmpty()) {
            return emptyList()
        }
        val copied = ArrayList<Pair<Int, ByteArray>>(entries.size)
        for ((companyId, payload) in entries) {
            copied += companyId to payload.copyOf()
        }
        return copied
    }
}
