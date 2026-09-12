package jp.smartglasses.detector.data.bluetooth

import jp.smartglasses.detector.util.Constants

internal class DiagnosticLogWriteGate(
    private val cooldownMs: Long = Constants.DIAGNOSTIC_LOG_WRITE_COOLDOWN_MS,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val lock = Any()
    private val lastWrites = mutableMapOf<String, Long>()
    private var lastPruneAt = 0L

    fun shouldWrite(key: String): Boolean = synchronized(lock) {
        val now = clock()
        pruneExpiredEntries(now)

        val lastWrite = lastWrites[key]
        if (lastWrite != null && now - lastWrite < cooldownMs) {
            return false
        }

        lastWrites[key] = now
        true
    }

    fun clear() = synchronized(lock) {
        lastWrites.clear()
        lastPruneAt = 0L
    }

    private fun pruneExpiredEntries(now: Long) {
        if (lastWrites.size < PRUNE_SIZE_THRESHOLD && now - lastPruneAt < cooldownMs) {
            return
        }
        lastPruneAt = now
        lastWrites.entries.removeAll { (_, writtenAt) ->
            now - writtenAt >= cooldownMs
        }
    }

    private companion object {
        const val PRUNE_SIZE_THRESHOLD = 64
    }
}
