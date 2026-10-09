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

/**
 * Page background ARGB per reader scheme. Single source of truth for every
 * swatch, preview card, and system-bar tint — add a scheme here, never a
 * fourth hex table at the call site.
 */
fun ColorSchemeChoice.pageBackgroundArgb(): Int = when (this) {
    ColorSchemeChoice.LIGHT -> 0xFFFFFFFF.toInt()
    ColorSchemeChoice.SEPIA -> 0xFFF5E6C8.toInt()
    ColorSchemeChoice.GREY -> 0xFF444444.toInt()
    ColorSchemeChoice.DARK -> 0xFF121212.toInt()
    ColorSchemeChoice.BLACK -> 0xFF000000.toInt()
}

/** Body-text ARGB paired with [pageBackgroundArgb]. */
fun ColorSchemeChoice.pageForegroundArgb(): Int = when (this) {
    ColorSchemeChoice.LIGHT -> 0xFF1A1C1E.toInt()
    ColorSchemeChoice.SEPIA -> 0xFF3E2F1C.toInt()
    ColorSchemeChoice.GREY -> 0xFFF2F2F2.toInt()
    ColorSchemeChoice.DARK -> 0xFFE3E1E5.toInt()
    ColorSchemeChoice.BLACK -> 0xFFFFFFFF.toInt()
}

/** True on schemes that need dark status/nav icons and scrims. */
fun ColorSchemeChoice.isLightScheme(): Boolean =
    this == ColorSchemeChoice.LIGHT || this == ColorSchemeChoice.SEPIA

/** App-wide Material color presets (used when dynamic color is off). */
enum class AppColorScheme(val displayName: String) {
    IRIDIUM("Iridium"),
    OCEAN("Ocean"),
    FOREST("Forest"),
    SUNSET("Sunset"),
    CATPPUCCIN("Catppuccin"),
    NORD("Nord"),
    GRUVBOX("Gruvbox"),
    DRACULA("Dracula"),
    TOKYO_NIGHT("Tokyo Night"),
    EVERFOREST("Everforest"),
    MONOCHROME("Monochrome"),
}

/** App-wide theme preferences persisted in DataStore. */
data class ThemePreferences(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val colorScheme: AppColorScheme = AppColorScheme.IRIDIUM,
    val amoled: Boolean = false,
    val hapticsEnabled: Boolean = true,
)
