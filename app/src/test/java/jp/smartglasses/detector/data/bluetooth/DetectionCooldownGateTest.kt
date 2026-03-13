package jp.smartglasses.detector.data.bluetooth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionCooldownGateTest {
    private var now = 1_000L
    private val gate = DetectionCooldownGate(
        sameDeviceCooldownMs = 30_000L,
        sameManufacturerCooldownMs = 15_000L,
        clock = { now }
    )

    @Test
    fun `same device is suppressed until cooldown expires`() {
        assertTrue(gate.shouldEmitDetection("device:a", "manufacturer:meta"))

        now += 29_999L
        assertFalse(gate.shouldEmitDetection("device:a", "manufacturer:meta"))

        now += 1L
        assertTrue(gate.shouldEmitDetection("device:a", "manufacturer:meta"))
    }

    @Test
    fun `different devices from same manufacturer are throttled briefly`() {
        assertTrue(gate.shouldEmitDetection("device:a", "manufacturer:meta"))

        now += 10_000L
        assertFalse(gate.shouldEmitDetection("device:b", "manufacturer:meta"))

        now += 5_000L
        assertTrue(gate.shouldEmitDetection("device:b", "manufacturer:meta"))
    }

    @Test
    fun `diagnostic logs are suppressed for the same key during cooldown`() {
        assertTrue(gate.shouldEmitDiagnostic("diagnostic:unknown-device"))

        now += 5_000L
        assertFalse(gate.shouldEmitDiagnostic("diagnostic:unknown-device"))

        now += 25_000L
        assertTrue(gate.shouldEmitDiagnostic("diagnostic:unknown-device"))
    }
}
