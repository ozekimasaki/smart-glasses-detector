package jp.smartglasses.detector.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceTest {
    @Test
    fun `stored enum names round trip`() {
        Distance.entries.forEach { distance ->
            assertEquals(distance, Distance.fromStored(distance.name))
        }
    }

    @Test
    fun `legacy japanese labels keep mapping`() {
        assertEquals(Distance.VERY_CLOSE, Distance.fromStored("とても近い"))
        assertEquals(Distance.CLOSE, Distance.fromStored("近い"))
        assertEquals(Distance.MODERATE, Distance.fromStored("少し離れている"))
        assertEquals(Distance.FAR, Distance.fromStored("離れている"))
    }
}
