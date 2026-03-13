package jp.smartglasses.detector.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import jp.smartglasses.detector.util.ScanSensitivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_BACKGROUND_ENABLED = booleanPreferencesKey("background_enabled")
        val KEY_NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_SENSITIVITY = intPreferencesKey("sensitivity")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_IS_SCANNING = booleanPreferencesKey("is_scanning")
    }
    
    val backgroundEnabled: Flow<Boolean> = dataStore.data
        .map { it[KEY_BACKGROUND_ENABLED] ?: true }
    
    val notificationEnabled: Flow<Boolean> = dataStore.data
        .map { it[KEY_NOTIFICATION_ENABLED] ?: true }
    
    val vibrationEnabled: Flow<Boolean> = dataStore.data
        .map { it[KEY_VIBRATION_ENABLED] ?: true }
    
    val soundEnabled: Flow<Boolean> = dataStore.data
        .map { it[KEY_SOUND_ENABLED] ?: true }
    
    val sensitivity: Flow<ScanSensitivity> = dataStore.data
        .map { 
            when (it[KEY_SENSITIVITY] ?: 1) {
                0 -> ScanSensitivity.LOW_POWER
                2 -> ScanSensitivity.HIGH_ACCURACY
                else -> ScanSensitivity.BALANCED
            }
        }
    
    val onboardingCompleted: Flow<Boolean> = dataStore.data
        .map { it[KEY_ONBOARDING_COMPLETED] ?: false }
    
    val isScanning: Flow<Boolean> = dataStore.data
        .map { it[KEY_IS_SCANNING] ?: false }
    
    suspend fun setBackgroundEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BACKGROUND_ENABLED] = enabled }
    }
    
    suspend fun setNotificationEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATION_ENABLED] = enabled }
    }
    
    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_VIBRATION_ENABLED] = enabled }
    }
    
    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }
    
    suspend fun setSensitivity(sensitivity: ScanSensitivity) {
        val value = when (sensitivity) {
            ScanSensitivity.LOW_POWER -> 0
            ScanSensitivity.BALANCED -> 1
            ScanSensitivity.HIGH_ACCURACY -> 2
        }
        dataStore.edit { it[KEY_SENSITIVITY] = value }
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }
    
    suspend fun setIsScanning(scanning: Boolean) {
        dataStore.edit { it[KEY_IS_SCANNING] = scanning }
    }
}
