package jp.smartglasses.detector.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BluetoothAdvertisedNamePolicyTest {
    @Test
    fun `advertised name wins over cached name and alias`() {
        assertEquals(
            "G2_12_L",
            BluetoothAdvertisedNamePolicy.resolve(
                "G2_12_L",
                "Unknown",
                "My Glasses"
            )
        )
    }

    @Test
    fun `complete advertised name wins over a short scan record name`() {
        assertEquals(
            "Solos AirGo3 1234",
            BluetoothAdvertisedNamePolicy.resolve(
                "Solos AirGo3 1234",
                "Solos",
                "Unknown",
                "Kitchen"
            )
        )
    }

    @Test
    fun `cached name is used when advertisement has no name`() {
        assertEquals(
            "Solos AirGo3 1234",
            BluetoothAdvertisedNamePolicy.resolve(
                "  ",
                "Solos AirGo3 1234",
                "Kitchen"
            )
        )
    }

    @Test
    fun `placeholder cached names fall through to the next usable name`() {
        assertEquals(
            "Solos AirGo3 1234",
            BluetoothAdvertisedNamePolicy.resolve(
                "Unknown",
                "N/A",
                "Solos AirGo3 1234"
            )
        )
    }

    @Test
    fun `alias is used when advertisement and cache are blank`() {
        assertEquals(
            "Nimo-A1B2",
            BluetoothAdvertisedNamePolicy.resolve(
                null,
                "",
                "Nimo-A1B2"
            )
        )
    }

    @Test
    fun `blank values are ignored`() {
        assertNull(
            BluetoothAdvertisedNamePolicy.resolve(
                " ",
                "",
                null
            )
        )
    }
}
