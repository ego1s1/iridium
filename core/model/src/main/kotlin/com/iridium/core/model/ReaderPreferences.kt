package com.iridium.core.model

/** Text alignment for reflowable EPUB content (Lithium's Text align row). */
enum class TextAlign {
    ORIGINAL,
    LEFT,
    JUSTIFY,
}

/** Reader preferences persisted in DataStore. */
data class ReaderPreferences(
    val flow: ReadingFlow = ReadingFlow.AUTO,
    val fontScale: Float = 1f,
    val textAlign: TextAlign = TextAlign.ORIGINAL,
    val theme: ColorSchemeChoice = ColorSchemeChoice.SEPIA,
    val brightness: Float = -1f,
    val keepScreenOn: Boolean = true,
    val showPageCounter: Boolean = true,
    val volumeKeys: Boolean = false,
)
