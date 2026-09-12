package jp.smartglasses.detector.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test

class ScanEnvironmentSignalsTest {
    @Test
    fun `revision advances so restore UI can re-read bluetooth and permission state`() {
        val signals = ScanEnvironmentSignals()

        assertEquals(0, signals.revision.value)
        signals.notifyChanged()
        assertEquals(1, signals.revision.value)
        signals.notifyChanged()
        assertEquals(2, signals.revision.value)
    }
}
