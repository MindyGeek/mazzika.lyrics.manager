package com.mazzika.lyrics.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val autoSaveSyncKey = booleanPreferencesKey("auto_save_sync")
    private val deviceNameKey = stringPreferencesKey("device_name")

    val theme: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[themeKey]) { "light" -> ThemeMode.LIGHT; "dark" -> ThemeMode.DARK; else -> ThemeMode.DARK }
    }
    val autoSaveSync: Flow<Boolean> = context.dataStore.data.map { prefs -> prefs[autoSaveSyncKey] ?: false }
    val deviceName: Flow<String> = context.dataStore.data.map { prefs -> prefs[deviceNameKey] ?: android.os.Build.MODEL }

    suspend fun setTheme(mode: ThemeMode) { context.dataStore.edit { it[themeKey] = when (mode) { ThemeMode.LIGHT -> "light"; ThemeMode.DARK -> "dark"; ThemeMode.SYSTEM -> "system" } } }
    suspend fun setAutoSaveSync(enabled: Boolean) { context.dataStore.edit { it[autoSaveSyncKey] = enabled } }
    suspend fun setDeviceName(name: String) { context.dataStore.edit { it[deviceNameKey] = name } }

    enum class ThemeMode { LIGHT, DARK, SYSTEM }
}
