package com.iridium.feature.reader.impl

import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.CustomReaderTheme
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.TextAlign
import com.iridium.core.model.pageBackgroundArgb
import com.iridium.core.model.pageForegroundArgb
import com.iridium.core.model.resolvedColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.readium.r2.navigator.preferences.Theme as ReadiumTheme
import kotlinx.collections.immutable.persistentListOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EpubPreferencesMapperTest {
    @Test
    fun `reading is always paged`() {
        // Scrolling was removed as a reading mode, so every preference set
        // must ask the navigator for paged layout.
        assertEquals(false, EpubPreferencesMapper.map(ReaderPreferences()).scroll)
    }

    @Test
    fun `text align original leaves publisher styles untouched`() {
        val original = EpubPreferencesMapper.map(ReaderPreferences(textAlign = TextAlign.ORIGINAL))
        assertNull(original.textAlign)
        assertNull(original.publisherStyles)
    }

    @Test
    fun `explicit align disables publisher styles`() {
        val justify = EpubPreferencesMapper.map(ReaderPreferences(textAlign = TextAlign.JUSTIFY))
        assertEquals(false, justify.publisherStyles)
        assertEquals(
            org.readium.r2.navigator.preferences.TextAlign.JUSTIFY,
            justify.textAlign,
        )
    }

    @Test
    fun `themes map to nearest readium theme`() {
        assertEquals(
            ReadiumTheme.SEPIA,
            EpubPreferencesMapper.map(ReaderPreferences(theme = ColorSchemeChoice.SEPIA)).theme,
        )
        assertEquals(
            ReadiumTheme.DARK,
            EpubPreferencesMapper.map(ReaderPreferences(theme = ColorSchemeChoice.BLACK)).theme,
        )
        assertEquals(
            ReadiumTheme.LIGHT,
            EpubPreferencesMapper.map(ReaderPreferences(theme = ColorSchemeChoice.GREY)).theme,
        )
    }

    @Test
    fun `font scale is clamped`() {
        val low = EpubPreferencesMapper.map(ReaderPreferences(fontScale = 0.01f)).fontSize
        val high = EpubPreferencesMapper.map(ReaderPreferences(fontScale = 99f)).fontSize
        // The stored scale is a multiplier of a comfortable base (1.15), so
        // the clamp applies to the preference before the base is applied.
        assertEquals(0.575, low ?: 0.0, 0.0001)
        assertEquals(3.45, high ?: 0.0, 0.0001)
    }

    @Test
    fun `custom palettes map by luminance`() {
        val white = CustomReaderTheme("w", "White", 0xFFFFFFFF.toInt(), 0xFF000000.toInt())
        val mid = CustomReaderTheme("m", "Mid", 0xFFB89B5E.toInt(), 0xFF000000.toInt())
        val black = CustomReaderTheme("b", "Black", 0xFF000000.toInt(), 0xFFFFFFFF.toInt())
        assertEquals(
            ReadiumTheme.LIGHT,
            EpubPreferencesMapper.map(ReaderPreferences(customThemes = persistentListOf(white), customThemeId = "w")).theme,
        )
        assertEquals(
            ReadiumTheme.SEPIA,
            EpubPreferencesMapper.map(ReaderPreferences(customThemes = persistentListOf(mid), customThemeId = "m")).theme,
        )
        assertEquals(
            ReadiumTheme.DARK,
            EpubPreferencesMapper.map(ReaderPreferences(customThemes = persistentListOf(black), customThemeId = "b")).theme,
        )
    }

    @Test
    fun `dangling custom id falls back to the preset`() {
        val prefs = ReaderPreferences(
            theme = ColorSchemeChoice.SEPIA,
            customThemes = persistentListOf(),
            customThemeId = "gone",
        )
        assertEquals(ReadiumTheme.SEPIA, EpubPreferencesMapper.map(prefs).theme)
        assertEquals(
            ColorSchemeChoice.SEPIA.pageBackgroundArgb() to ColorSchemeChoice.SEPIA.pageForegroundArgb(),
            prefs.resolvedColors(),
        )
    }
}
