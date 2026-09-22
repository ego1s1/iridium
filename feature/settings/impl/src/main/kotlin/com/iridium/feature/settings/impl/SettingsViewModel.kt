package com.iridium.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: IridiumPreferencesDataSource,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.themePreferences,
        preferences.motionStyle,
        preferences.readerPreferences,
        preferences.libraryDisplay,
        ::SettingsUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode -> updateTheme { it.copy(mode = action.mode) }
            is SettingsAction.SetDynamicColor -> updateTheme { it.copy(dynamicColor = action.enabled) }
            is SettingsAction.SetColorScheme ->
                updateTheme { it.copy(colorScheme = action.scheme, dynamicColor = false) }
            is SettingsAction.SetAmoled -> updateTheme { it.copy(amoled = action.enabled) }
            is SettingsAction.SetMotionStyle -> viewModelScope.launch {
                preferences.updateMotionStyle(action.style)
            }
            is SettingsAction.SetSortOrder ->
                updateDisplay { it.copy(sortOrder = action.order) }
            is SettingsAction.SetFilter -> updateDisplay { it.copy(filter = action.filter) }
            is SettingsAction.SetHideErrors -> updateDisplay { it.copy(hideErrors = action.hide) }
            is SettingsAction.SetKeepScreenOn -> updateReader { it.copy(keepScreenOn = action.enabled) }
            is SettingsAction.SetShowPageCounter -> updateReader { it.copy(showPageCounter = action.enabled) }
            is SettingsAction.SetVolumeKeys -> updateReader { it.copy(volumeKeys = action.enabled) }
        }
    }

    private fun updateTheme(transform: (ThemePreferences) -> ThemePreferences) {
        viewModelScope.launch { preferences.updateThemePreferences(transform) }
    }

    private fun updateReader(transform: (ReaderPreferences) -> ReaderPreferences) {
        viewModelScope.launch { preferences.updateReaderPreferences(transform) }
    }

    private fun updateDisplay(transform: (LibraryDisplay) -> LibraryDisplay) {
        viewModelScope.launch { preferences.updateLibraryDisplay(transform) }
    }
}
