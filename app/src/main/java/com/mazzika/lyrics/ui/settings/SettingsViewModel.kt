package com.mazzika.lyrics.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import com.mazzika.lyrics.data.preferences.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = (application as MazzikaApplication).userPreferences

    val theme: StateFlow<UserPreferences.ThemeMode> = userPreferences.theme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences.ThemeMode.SYSTEM,
        )

    val autoSaveSync: StateFlow<Boolean> = userPreferences.autoSaveSync
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    val deviceName: StateFlow<String> = userPreferences.deviceName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = android.os.Build.MODEL,
        )

    fun setTheme(mode: UserPreferences.ThemeMode) {
        viewModelScope.launch { userPreferences.setTheme(mode) }
    }

    fun setAutoSaveSync(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setAutoSaveSync(enabled) }
    }

    fun setDeviceName(name: String) {
        viewModelScope.launch { userPreferences.setDeviceName(name) }
    }
}
