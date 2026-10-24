package com.epfl.esl.simongame.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    ENGLISH("en", "English", "🇬🇧"),
    FRENCH("fr", "Français", "🇫🇷"),
    SPANISH("es", "Español", "🇪🇸");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.find { it.code == code } ?: ENGLISH
    }
}

data class GameSettings(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.ENGLISH
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "game_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settings: Flow<GameSettings> = context.settingsDataStore.data.map { prefs ->
        GameSettings(
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: "en")
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }
}