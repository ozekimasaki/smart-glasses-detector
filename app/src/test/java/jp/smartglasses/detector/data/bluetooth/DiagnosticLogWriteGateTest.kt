package jp.smartglasses.detector.data.bluetooth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticLogWriteGateTest {
    @Test
    fun `same key is suppressed until cooldown expires`() {
        var now = 1_000L
        val gate = DiagnosticLogWriteGate(cooldownMs = 30_000L, clock = { now })

        assertTrue(gate.shouldWrite("address:AA:01"))
        now += 29_999L
        assertFalse(gate.shouldWrite("address:AA:01"))
        now += 1L
        assertTrue(gate.shouldWrite("address:AA:01"))
    }

    @Test
    fun `different keys can be written immediately`() {
        val gate = DiagnosticLogWriteGate(cooldownMs = 30_000L, clock = { 1_000L })

        assertTrue(gate.shouldWrite("address:AA:01"))
        assertTrue(gate.shouldWrite("address:AA:02"))
    }
}
