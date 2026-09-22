package com.iridium.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iridium.core.datastore.IridiumPreferencesDataSource
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
        ::SettingsUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode ->
                updateTheme { it.copy(mode = action.mode) }
            is SettingsAction.SetDynamicColor ->
                updateTheme { it.copy(dynamicColor = action.enabled) }
            is SettingsAction.SetColorScheme ->
                updateTheme { it.copy(colorScheme = action.scheme) }
            is SettingsAction.SetAmoled ->
                updateTheme { it.copy(amoled = action.enabled) }
            is SettingsAction.SetMotionStyle -> viewModelScope.launch {
                preferences.updateMotionStyle(action.style)
            }
            is SettingsAction.SetKeepScreenOn ->
                updateReader { it.copy(keepScreenOn = action.enabled) }
            is SettingsAction.SetShowPageCounter ->
                updateReader { it.copy(showPageCounter = action.enabled) }
            is SettingsAction.SetVolumeKeys ->
                updateReader { it.copy(volumeKeys = action.enabled) }
        }
    }

    private fun updateTheme(transform: (com.iridium.core.model.ThemePreferences) -> com.iridium.core.model.ThemePreferences) {
        viewModelScope.launch { preferences.updateThemePreferences(transform) }
    }

    private fun updateReader(transform: (com.iridium.core.model.ReaderPreferences) -> com.iridium.core.model.ReaderPreferences) {
        viewModelScope.launch { preferences.updateReaderPreferences(transform) }
    }
}
