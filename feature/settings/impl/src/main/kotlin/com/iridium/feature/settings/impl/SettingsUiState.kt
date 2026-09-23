package com.iridium.feature.settings.impl

import com.iridium.core.model.AppColorScheme
import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.LibraryFilter
import com.iridium.core.model.LibrarySortOrder
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemeMode
import com.iridium.core.model.ThemePreferences

data class SettingsUiState(
    val theme: ThemePreferences = ThemePreferences(),
    val motionStyle: MotionStyle = MotionStyle.EXPRESSIVE,
    val reader: ReaderPreferences = ReaderPreferences(),
    val libraryDisplay: LibraryDisplay = LibraryDisplay(),
    val crashReportingEnabled: Boolean = false,
)

sealed interface SettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : SettingsAction
    data class SetDynamicColor(val enabled: Boolean) : SettingsAction
    data class SetColorScheme(val scheme: AppColorScheme) : SettingsAction
    data class SetAmoled(val enabled: Boolean) : SettingsAction
    data class SetMotionStyle(val style: MotionStyle) : SettingsAction
    data class SetSortOrder(val order: LibrarySortOrder) : SettingsAction
    data class SetFilter(val filter: LibraryFilter) : SettingsAction
    data class SetHideErrors(val hide: Boolean) : SettingsAction
    data class SetKeepScreenOn(val enabled: Boolean) : SettingsAction
    data class SetShowPageCounter(val enabled: Boolean) : SettingsAction
    data class SetVolumeKeys(val enabled: Boolean) : SettingsAction
    data class SetCrashReporting(val enabled: Boolean) : SettingsAction
}
