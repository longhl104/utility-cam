package com.utility.cam.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages app preferences using DataStore
 */
class PreferencesManager(private val context: Context) {

    companion object {
        val DEFAULT_TTL_KEY = stringPreferencesKey("default_ttl")
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch")
        val CLEANUP_DELAY_SECONDS_KEY = intPreferencesKey("cleanup_delay_seconds")
    }

    /**
     * Get the default TTL duration
     */
    fun getDefaultTTL(): Flow<TTLDuration> = context.dataStore.data.map { preferences ->
        val ttlName = preferences[DEFAULT_TTL_KEY] ?: TTLDuration.TWENTY_FOUR_HOURS.name
        try {
            TTLDuration.valueOf(ttlName)
        } catch (_: Exception) {
            TTLDuration.TWENTY_FOUR_HOURS
        }
    }

    /**
     * Set the default TTL duration
     */
    suspend fun setDefaultTTL(duration: TTLDuration) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_TTL_KEY] = duration.name
        }
    }

    /**
     * Get whether cleanup notifications are enabled
     */
    fun getNotificationsEnabled(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED_KEY] ?: true // Default to enabled
    }

    /**
     * Set whether cleanup notifications are enabled
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    /**
     * Check if this is the first launch
     */
    fun isFirstLaunch(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_LAUNCH_KEY] ?: true // Default to true (first launch)
    }

    /**
     * Mark that the app has been launched
     */
    suspend fun setFirstLaunchComplete() {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = false
        }
    }

    /**
     * Get the cleanup delay in seconds (for debug mode)
     */
    fun getCleanupDelaySeconds(): Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CLEANUP_DELAY_SECONDS_KEY] ?: 10 // Default to 10 seconds
    }

    /**
     * Set the cleanup delay in seconds (for debug mode)
     */
    suspend fun setCleanupDelaySeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[CLEANUP_DELAY_SECONDS_KEY] = seconds
        }
    }
}
