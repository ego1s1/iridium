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

/** App-wide theme preferences persisted in DataStore. */
data class ThemePreferences(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val amoled: Boolean = false,
)
