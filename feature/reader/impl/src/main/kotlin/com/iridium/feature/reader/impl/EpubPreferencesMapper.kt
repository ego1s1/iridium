package com.iridium.feature.reader.impl

import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.ReaderFontFamily
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ReadingFlow
import com.iridium.core.model.TextAlign
import com.iridium.core.model.isCustomThemeActive
import com.iridium.core.model.resolvedColors
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily as ReadiumFontFamily
import org.readium.r2.navigator.preferences.TextAlign as ReadiumTextAlign
import org.readium.r2.navigator.preferences.Theme as ReadiumTheme

/**
 * Pure mapping from Iridium reader prefs to Readium [EpubPreferences].
 * Unit-testable; the navigator receives the result via submitPreferences.
 */
internal object EpubPreferencesMapper {

    /**
     * Baseline multiplier applied under the user's text-size preference:
     * the toolkit's 100% is noticeably smaller than a printed page, so the
     * slider's 100% maps to a readable body size instead of a tiny one.
     */
    private const val BASE_FONT_SCALE = 1.15f

    /**
     * Nearest Readium built-in for the resolved colors. Presets keep their
     * historic mapping; custom palettes pick by background luminance (sepia
     * band in the middle). Exact custom colors always ride the CSS override.
     */
    fun nearestBuiltinTheme(prefs: ReaderPreferences): ReadiumTheme {
        if (!prefs.isCustomThemeActive()) {
            return when (prefs.theme) {
                ColorSchemeChoice.LIGHT, ColorSchemeChoice.GREY -> ReadiumTheme.LIGHT
                ColorSchemeChoice.SEPIA -> ReadiumTheme.SEPIA
                ColorSchemeChoice.DARK, ColorSchemeChoice.BLACK -> ReadiumTheme.DARK
            }
        }
        val (bg, _) = prefs.resolvedColors()
        val luminance = (0.299 * ((bg shr 16) and 0xFF) +
            0.587 * ((bg shr 8) and 0xFF) +
            0.114 * (bg and 0xFF)) / 255.0
        return when {
            luminance > 0.75 -> ReadiumTheme.LIGHT
            luminance > 0.35 -> ReadiumTheme.SEPIA
            else -> ReadiumTheme.DARK
        }
    }

    fun map(prefs: ReaderPreferences): EpubPreferences = EpubPreferences(
        // AUTO is intentionally paged (scroll=false): it means "follow the
        // book" rather than a third layout mode, and Readium renders paged
        // unless explicitly told to scroll.
        scroll = when (prefs.flow) {
            ReadingFlow.SCROLLED -> true
            ReadingFlow.AUTO, ReadingFlow.PAGED -> false
        },
        // The stored scale is a multiplier of a comfortable base, so the UI's
        // "100%" already reads at a normal book size instead of the toolkit's
        // small default. 140% is the default and lands near the reference page.
        fontSize = (prefs.fontScale.coerceIn(0.5f, 3f) * BASE_FONT_SCALE).toDouble(),
        fontFamily = prefs.fontFamily.cssName?.let { ReadiumFontFamily(it) },
        pageMargins = prefs.pageMargins.coerceIn(0.5f, 4f).toDouble(),
        lineHeight = prefs.lineHeight.coerceIn(1f, 3f).toDouble(),
        textAlign = when (prefs.textAlign) {
            TextAlign.ORIGINAL -> null
            TextAlign.LEFT -> ReadiumTextAlign.LEFT
            TextAlign.JUSTIFY -> ReadiumTextAlign.JUSTIFY
        },
        // Advanced alignment settings only take effect with publisher styles off.
        publisherStyles = if (prefs.textAlign == TextAlign.ORIGINAL) null else false,
        theme = nearestBuiltinTheme(prefs),
        // NOTE: Readium's preference Color has a private constructor, so custom
        // grey/pure-black backgrounds map to the nearest built-in theme for
        // now (follow-up: RsProperties CSS injection via factory config).
    )
}
