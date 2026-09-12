package jp.smartglasses.detector.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import jp.smartglasses.detector.util.ScanSensitivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPreferencesTest {
    @Test
    fun `defaults match documented values`() = runBlocking {
        val preferences = AppPreferences(InMemoryPreferencesDataStore())

        assertTrue(preferences.backgroundEnabled.first())
        assertTrue(preferences.notificationEnabled.first())
        assertTrue(preferences.vibrationEnabled.first())
        assertTrue(preferences.soundEnabled.first())
        assertEquals(ScanSensitivity.BALANCED, preferences.sensitivity.first())
        assertFalse(preferences.onboardingCompleted.first())
        assertFalse(preferences.isScanning.first())
    }

    @Test
    fun `persists scanning intent and sensitivity`() = runBlocking {
        val preferences = AppPreferences(InMemoryPreferencesDataStore())

        preferences.setIsScanning(true)
        preferences.setSensitivity(ScanSensitivity.HIGH_ACCURACY)
        preferences.setBackgroundEnabled(false)
        preferences.setOnboardingCompleted(true)

        assertTrue(preferences.isScanning.first())
        assertEquals(ScanSensitivity.HIGH_ACCURACY, preferences.sensitivity.first())
        assertFalse(preferences.backgroundEnabled.first())
        assertTrue(preferences.onboardingCompleted.first())

        preferences.setSensitivity(ScanSensitivity.LOW_POWER)
        preferences.setIsScanning(false)

        assertEquals(ScanSensitivity.LOW_POWER, preferences.sensitivity.first())
        assertFalse(preferences.isScanning.first())
    }
}

private class InMemoryPreferencesDataStore(
    initial: Preferences = emptyPreferences()
) : DataStore<Preferences> {
    private val mutex = Mutex()
    private val state = MutableStateFlow(initial)

    override val data: Flow<Preferences> = state

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences
    ): Preferences {
        return mutex.withLock {
            val updated = transform(state.value)
            state.value = updated
            updated
        }
    }
}
