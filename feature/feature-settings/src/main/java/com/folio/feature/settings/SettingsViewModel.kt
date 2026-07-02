package com.folio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.core.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val turnMode: SettingsDataStore.TurnMode = SettingsDataStore.TurnMode.CURL,
    val appTheme: SettingsDataStore.AppTheme = SettingsDataStore.AppTheme.SYSTEM,
    val readingMode: SettingsDataStore.ReadingMode = SettingsDataStore.ReadingMode.STANDARD,
    val hapticsEnabled: Boolean = true,
    val hapticsIntensity: Float = 1.0f,
    val storageBudgetMb: Int = 500
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsDataStore.settings
        .map { SettingsUiState(it.turnMode, it.appTheme, it.readingMode, it.hapticsEnabled, it.hapticsIntensity, it.storageBudgetMb) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setTurnMode(mode: SettingsDataStore.TurnMode) {
        viewModelScope.launch { settingsDataStore.setTurnMode(mode) }
    }

    fun setAppTheme(theme: SettingsDataStore.AppTheme) {
        viewModelScope.launch { settingsDataStore.setAppTheme(theme) }
    }

    fun setReadingMode(mode: SettingsDataStore.ReadingMode) {
        viewModelScope.launch { settingsDataStore.setReadingMode(mode) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setHapticsEnabled(enabled) }
    }

    fun setHapticsIntensity(intensity: Float) {
        viewModelScope.launch { settingsDataStore.setHapticsIntensity(intensity) }
    }

    fun setStorageBudgetMb(budget: Int) {
        viewModelScope.launch { settingsDataStore.setStorageBudgetMb(budget) }
    }
}
