package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppThemeMode
import com.example.data.model.AssistantLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "paglii_prefs")

data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val preferredLanguage: AssistantLanguage = AssistantLanguage.HINGLISH,
    val autoSpeakEnabled: Boolean = false,
    val hasSeenWelcome: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val themeRaw = prefs[Keys.THEME_MODE]
        val theme = AppThemeMode.entries.firstOrNull { it.name == themeRaw } ?: AppThemeMode.DARK
        val lang = AssistantLanguage.fromLabel(prefs[Keys.PREFERRED_LANGUAGE])
        val autoSpeak = prefs[Keys.AUTO_SPEAK] ?: false
        val seenWelcome = prefs[Keys.HAS_SEEN_WELCOME] ?: false
        UserSettings(
            themeMode = theme,
            preferredLanguage = lang,
            autoSpeakEnabled = autoSpeak,
            hasSeenWelcome = seenWelcome
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setPreferredLanguage(language: AssistantLanguage) {
        context.dataStore.edit { it[Keys.PREFERRED_LANGUAGE] = language.label }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun markWelcomeSeen() {
        context.dataStore.edit { it[Keys.HAS_SEEN_WELCOME] = true }
    }
}
