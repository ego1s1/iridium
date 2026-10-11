package com.iridium.core.model

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
)

