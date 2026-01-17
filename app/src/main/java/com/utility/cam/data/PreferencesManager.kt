package com.utility.cam.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
}
