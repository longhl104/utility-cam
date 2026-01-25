package com.utility.cam.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
     * Get list of supported languages
     */
    fun getSupportedLanguages(): List<Language> {
        return listOf(
            Language(SYSTEM_DEFAULT, "System Default"),
            Language("en", "English"),
            Language("vi", "Tiếng Việt (Vietnamese)"),
            Language("ne", "नेपाली (Nepali)"),
            Language("hi", "हिन्दी (Hindi)"),
            Language("bn", "বাংলা (Bengali)"),
            Language("ur", "اردو (Urdu)"),
            Language("ar", "العربية (Arabic)"),
            Language("fi", "Suomi (Finnish)"),
            Language("pl", "Polski (Polish)"),
            Language("pt", "Português (Portuguese)"),
            Language("in", "Bahasa Indonesia (Indonesian)"),
            Language("ko", "한국어 (Korean)"),
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
