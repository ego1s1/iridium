package com.iridium.core.model

/** App theme selection. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Reader color scheme presets (reader themes, Lithium-style). */
enum class ColorSchemeChoice {
    LIGHT,
    SEPIA,
    GREY,
    DARK,
    BLACK,
}

/** App-wide Material color presets (used when dynamic color is off). */
enum class AppColorScheme {
    IRIDIUM,
    OCEAN,
    FOREST,
    SUNSET,
}

/** App-wide theme preferences persisted in DataStore. */
data class ThemePreferences(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val colorScheme: AppColorScheme = AppColorScheme.IRIDIUM,
    val amoled: Boolean = false,
)
