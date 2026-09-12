package jp.smartglasses.detector.domain.service

object BluetoothAdvertisedNamePolicy {
    private val ignoredExactNames = setOf(
        "unknown",
        "n/a",
        "null"
    )

    fun resolve(vararg names: String?): String? {
        return names.asSequence()
            .mapNotNull(::usableName)
            .firstOrNull()
    }

    private fun usableName(name: String?): String? {
        val trimmed = name?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return null
        }
        if (trimmed.lowercase() in ignoredExactNames) {
            return null
        }
        return trimmed
    }
}
