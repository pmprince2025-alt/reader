package com.folio.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "folio_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val TURN_MODE = stringPreferencesKey("turn_mode")
        val APP_THEME = stringPreferencesKey("app_theme")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val HAPTICS_INTENSITY = floatPreferencesKey("haptics_intensity")
        val STORAGE_BUDGET_MB = intPreferencesKey("storage_budget_mb")
    }

    enum class TurnMode(val value: String) {
        CURL("curl"), SLIDE("slide"), SCROLL("scroll");

        companion object {
            fun fromValue(value: String): TurnMode =
                entries.firstOrNull { it.value == value } ?: CURL
        }
    }

    enum class AppTheme(val value: String) {
        SYSTEM("system"), LIGHT("light"), DARK("dark");

        companion object {
            fun fromValue(value: String): AppTheme =
                entries.firstOrNull { it.value == value } ?: SYSTEM
        }
    }

    enum class ReadingMode(val value: String) {
        STANDARD("standard"), SEPIA("sepia"), NIGHT("night");

        companion object {
            fun fromValue(value: String): ReadingMode =
                entries.firstOrNull { it.value == value } ?: STANDARD
        }
    }

    val turnMode: Flow<TurnMode> = context.dataStore.data.map { prefs ->
        TurnMode.fromValue(prefs[Keys.TURN_MODE] ?: TurnMode.CURL.value)
    }

    val appTheme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        AppTheme.fromValue(prefs[Keys.APP_THEME] ?: AppTheme.SYSTEM.value)
    }

    val readingMode: Flow<ReadingMode> = context.dataStore.data.map { prefs ->
        ReadingMode.fromValue(prefs[Keys.READING_MODE] ?: ReadingMode.STANDARD.value)
    }

    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAPTICS_ENABLED] ?: true
    }

    val hapticsIntensity: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAPTICS_INTENSITY] ?: 1.0f
    }

    val storageBudgetMb: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.STORAGE_BUDGET_MB] ?: 500
    }

    suspend fun setTurnMode(mode: TurnMode) {
        context.dataStore.edit { it[Keys.TURN_MODE] = mode.value }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.APP_THEME] = theme.value }
    }

    suspend fun setReadingMode(mode: ReadingMode) {
        context.dataStore.edit { it[Keys.READING_MODE] = mode.value }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setHapticsIntensity(intensity: Float) {
        context.dataStore.edit { it[Keys.HAPTICS_INTENSITY] = intensity }
    }

    suspend fun setStorageBudgetMb(budget: Int) {
        context.dataStore.edit { it[Keys.STORAGE_BUDGET_MB] = budget }
    }
}
