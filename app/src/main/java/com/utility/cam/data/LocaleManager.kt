package com.utility.cam.data

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.localeDataStore by preferencesDataStore(name = "locale_preferences")

/**
 * Manages app language/locale settings
 */
class LocaleManager(private val context: Context) {

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("selected_language")
        const val SYSTEM_DEFAULT = "system"
    }

    /**
     * Get the currently selected language code
     */
    fun getSelectedLanguage(): Flow<String> {
        return context.localeDataStore.data.map { preferences ->
            preferences[LANGUAGE_KEY] ?: SYSTEM_DEFAULT
        }
    }

    /**
     * Save the selected language code
     */
    suspend fun setSelectedLanguage(languageCode: String) {
        context.localeDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }

    /**
     * Apply the locale to the context
     */
    @Suppress("DEPRECATION")
    fun applyLocale(languageCode: String): Context {
        val locale = if (languageCode == SYSTEM_DEFAULT) {
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(languageCode)
        }

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    /**
     * Get list of supported languages
     */
    fun getSupportedLanguages(): List<Language> {
        return listOf(
            Language(SYSTEM_DEFAULT, "System Default"),
            Language("en", "English"),
            Language("pt", "Português"),
            Language("in", "Bahasa Indonesia"),
            Language("hi", "हिन्दी")
        )
    }
}

/**
 * Represents a language option
 */
data class Language(
    val code: String,
    val displayName: String
)
