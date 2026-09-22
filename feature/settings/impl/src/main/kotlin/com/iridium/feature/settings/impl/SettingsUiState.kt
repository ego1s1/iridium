package com.iridium.feature.settings.impl

import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemePreferences

data class SettingsUiState(
    val theme: ThemePreferences = ThemePreferences(),
    val motionStyle: MotionStyle = MotionStyle.EXPRESSIVE,
    val reader: ReaderPreferences = ReaderPreferences(),
)

sealed interface SettingsAction {
    data class SetThemeMode(val mode: com.iridium.core.model.ThemeMode) : SettingsAction
    data class SetDynamicColor(val enabled: Boolean) : SettingsAction
    data class SetColorScheme(val scheme: com.iridium.core.model.AppColorScheme) : SettingsAction
    data class SetAmoled(val enabled: Boolean) : SettingsAction
    data class SetMotionStyle(val style: MotionStyle) : SettingsAction
    data class SetKeepScreenOn(val enabled: Boolean) : SettingsAction
    data class SetShowPageCounter(val enabled: Boolean) : SettingsAction
    data class SetVolumeKeys(val enabled: Boolean) : SettingsAction
}
