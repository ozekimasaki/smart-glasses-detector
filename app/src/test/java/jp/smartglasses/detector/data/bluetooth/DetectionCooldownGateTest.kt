package jp.smartglasses.detector.data.bluetooth

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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
        assertTrue(gate.shouldEmitDetection("address:AA:01", "manufacturer:meta"))

        now += 29_999L
        assertFalse(gate.shouldEmitDetection("address:AA:01", "manufacturer:meta"))

        now += 1L
        assertTrue(gate.shouldEmitDetection("address:AA:01", "manufacturer:meta"))
    }

    @Test
    fun `different addressed devices from the same manufacturer are not throttled`() {
        assertTrue(gate.shouldEmitDetection("address:AA:01", "manufacturer:meta"))

        now += 10_000L
        assertTrue(gate.shouldEmitDetection("address:AA:02", "manufacturer:meta"))
    }

    @Test
    fun `indistinguishable devices from the same manufacturer stay throttled`() {
        assertTrue(gate.shouldEmitDetection("fallback:meta:ray-ban", "manufacturer:meta"))

        now += 10_000L
        assertFalse(gate.shouldEmitDetection("fallback:meta:oakley", "manufacturer:meta"))

        now += 5_000L
        assertTrue(gate.shouldEmitDetection("fallback:meta:oakley", "manufacturer:meta"))
    }
}
