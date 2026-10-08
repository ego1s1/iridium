package com.iridium.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemeMode
import com.iridium.core.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
        preferences.crashReportingEnabled,
        preferences.linkedFolders,
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            theme = args[0] as ThemePreferences,
            motionStyle = args[1] as MotionStyle,
            reader = args[2] as ReaderPreferences,
            libraryDisplay = args[3] as LibraryDisplay,
            crashReportingEnabled = args[4] as Boolean,
            linkedFolders = args[5] as Set<String>,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    /** Latest pending slider write; cancelled-and-replaced per drag tick so
     * scrubbing persists once, on settle, instead of hammering DataStore. */
    private var sliderJob: Job? = null

    private fun coalesceSliderWrite(transform: (ReaderPreferences) -> ReaderPreferences) {
        sliderJob?.cancel()
        sliderJob = viewModelScope.launch {
            delay(SLIDER_WRITE_DEBOUNCE_MS)
            preferences.updateReaderPreferences(transform)
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode -> updateTheme {
                // Pure black needs a dark canvas: leaving for LIGHT clears a
                // stale flag instead of resurrecting it later.
                it.copy(
                    mode = action.mode,
                    amoled = if (action.mode == ThemeMode.LIGHT) false else it.amoled,
                )
            }
            is SettingsAction.SetDynamicColor -> updateTheme { it.copy(dynamicColor = action.enabled) }
            is SettingsAction.SetColorScheme ->
                updateTheme { it.copy(colorScheme = action.scheme, dynamicColor = false) }
            is SettingsAction.SetAmoled -> updateTheme { it.copy(amoled = action.enabled) }
            is SettingsAction.SetHapticsEnabled ->
                updateTheme { it.copy(hapticsEnabled = action.enabled) }
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
            is SettingsAction.SetFontSize -> coalesceSliderWrite { it.copy(fontScale = action.scale) }
            is SettingsAction.SetMargins -> coalesceSliderWrite { it.copy(pageMargins = action.margins) }
            is SettingsAction.SetLineHeight -> coalesceSliderWrite { it.copy(lineHeight = action.lineHeight) }
            is SettingsAction.AddLinkedFolder -> viewModelScope.launch {
                preferences.addLinkedFolder(action.uri)
            }
            is SettingsAction.SetCrashReporting -> viewModelScope.launch {
                preferences.setCrashReporting(action.enabled)
            }
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

    private companion object {
        /** Settles slider drags before the single DataStore write. */
        const val SLIDER_WRITE_DEBOUNCE_MS = 300L
    }
}
