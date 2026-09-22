package com.iridium.feature.reader.impl

import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ReadingFlow
import com.iridium.core.model.TextAlign
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.TextAlign as ReadiumTextAlign
import org.readium.r2.navigator.preferences.Theme as ReadiumTheme

/**
 * Pure mapping from Iridium reader prefs to Readium [EpubPreferences].
 * Unit-testable; the navigator receives the result via submitPreferences.
 */
object EpubPreferencesMapper {

    fun map(prefs: ReaderPreferences): EpubPreferences = EpubPreferences(
        scroll = when (prefs.flow) {
            ReadingFlow.SCROLLED -> true
            ReadingFlow.AUTO, ReadingFlow.PAGED -> false
        },
        fontSize = prefs.fontScale.coerceIn(0.5f, 3f).toDouble(),
        textAlign = when (prefs.textAlign) {
            TextAlign.ORIGINAL -> null
            TextAlign.LEFT -> ReadiumTextAlign.LEFT
            TextAlign.JUSTIFY -> ReadiumTextAlign.JUSTIFY
        },
        // Advanced alignment settings only take effect with publisher styles off.
        publisherStyles = if (prefs.textAlign == TextAlign.ORIGINAL) null else false,
        theme = when (prefs.theme) {
            ColorSchemeChoice.LIGHT, ColorSchemeChoice.GREY -> ReadiumTheme.LIGHT
            ColorSchemeChoice.SEPIA -> ReadiumTheme.SEPIA
            ColorSchemeChoice.DARK, ColorSchemeChoice.BLACK -> ReadiumTheme.DARK
        },
        // NOTE: Readium's preference Color has a private constructor, so custom
        // grey/pure-black backgrounds map to the nearest built-in theme for
        // now (follow-up: RsProperties CSS injection via factory config).
    )
}
