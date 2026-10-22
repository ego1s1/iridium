package com.iridium.core.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** Text alignment for reflowable EPUB content. */
enum class TextAlign {
    ORIGINAL,
    LEFT,
    JUSTIFY,
}

/** Reader body font family selection. */
enum class ReaderFontFamily(val cssName: String?) {
    /** Follow the book's own fonts. */
    BOOK(null),
    SERIF("serif"),
    SANS_SERIF("sans-serif"),
    MONOSPACE("monospace");

    fun next(): ReaderFontFamily = entries[(ordinal + 1) % entries.size]
    fun previous(): ReaderFontFamily =
        entries[(ordinal - 1 + entries.size) % entries.size]
}

/**
 * A user-defined reading palette. Colors are opaque ARGB ints; [id] is a
 * UUID assigned at creation. Pro-gated in the UI; the model itself is inert.
 */
data class CustomReaderTheme(
    val id: String,
    val name: String,
    val backgroundArgb: Int,
    val foregroundArgb: Int,
)

/** Reader preferences persisted in DataStore. */
data class ReaderPreferences(
    val flow: ReadingFlow = ReadingFlow.AUTO,
    val fontScale: Float = 1f,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.BOOK,
    val textAlign: TextAlign = TextAlign.ORIGINAL,
    val theme: ColorSchemeChoice = ColorSchemeChoice.SEPIA,
    val brightness: Float = -1f,
    val keepScreenOn: Boolean = true,
    val showPageCounter: Boolean = true,
    val volumeKeys: Boolean = false,
    val volumeKeysInverted: Boolean = false,
    /** Mirror left/right page-turn tap zones (accessibility/read-handedness). */
    val invertTaps: Boolean = false,
    val nightLight: Boolean = false,
    val nightLightIntensity: Float = 0.25f,
    val pageMargins: Float = 1f,
    val lineHeight: Float = 1.4f,
    /** User palettes; [customThemeId] selects the active one (null = preset). */
    val customThemes: ImmutableList<CustomReaderTheme> = persistentListOf(),
    val customThemeId: String? = null,
)

/** Exact page colors after resolving a custom theme over the preset. */
fun ReaderPreferences.resolvedColors(): Pair<Int, Int> {
    val custom = customThemeId?.let { id -> customThemes.firstOrNull { it.id == id } }
    return if (custom != null) {
        custom.backgroundArgb to custom.foregroundArgb
    } else {
        theme.pageBackgroundArgb() to theme.pageForegroundArgb()
    }
}

/** True when a custom palette is selected and present. */
fun ReaderPreferences.isCustomThemeActive(): Boolean =
    customThemeId?.let { id -> customThemes.any { it.id == id } } == true

