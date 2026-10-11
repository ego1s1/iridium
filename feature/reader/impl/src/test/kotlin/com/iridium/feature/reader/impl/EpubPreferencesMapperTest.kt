package com.iridium.feature.reader.impl

import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.CustomReaderTheme
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ReadingFlow
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EpubPreferencesMapperTest {
    @Test
    fun `scrolled flow maps to scroll true, paged and auto to false`() {
        assertEquals(
            true,
            EpubPreferencesMapper.map(ReaderPreferences(flow = ReadingFlow.SCROLLED)).scroll,
        )
        assertEquals(
            false,
            EpubPreferencesMapper.map(ReaderPreferences(flow = ReadingFlow.PAGED)).scroll,
        )
        assertEquals(
            false,
            EpubPreferencesMapper.map(ReaderPreferences(flow = ReadingFlow.AUTO)).scroll,
        )
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
        assertEquals(0.5, low ?: 0.0, 0.0001)
        assertEquals(3.0, high ?: 0.0, 0.0001)
    }

    @Test
    fun `custom palettes map by luminance`() {
        val white = CustomReaderTheme("w", "White", 0xFFFFFFFF.toInt(), 0xFF000000.toInt())
        val mid = CustomReaderTheme("m", "Mid", 0xFFB89B5E.toInt(), 0xFF000000.toInt())
        val black = CustomReaderTheme("b", "Black", 0xFF000000.toInt(), 0xFFFFFFFF.toInt())
        assertEquals(
            ReadiumTheme.LIGHT,
            EpubPreferencesMapper.map(ReaderPreferences(customThemes = listOf(white), customThemeId = "w")).theme,
        )
        assertEquals(
            ReadiumTheme.SEPIA,
            EpubPreferencesMapper.map(ReaderPreferences(customThemes = listOf(mid), customThemeId = "m")).theme,
        )
        assertEquals(
            ReadiumTheme.DARK,
            EpubPreferencesMapper.map(ReaderPreferences(customThemes = listOf(black), customThemeId = "b")).theme,
        )
    }

    @Test
    fun `dangling custom id falls back to the preset`() {
        val prefs = ReaderPreferences(
            theme = ColorSchemeChoice.SEPIA,
            customThemes = emptyList(),
            customThemeId = "gone",
        )
        assertEquals(ReadiumTheme.SEPIA, EpubPreferencesMapper.map(prefs).theme)
        assertEquals(
            ColorSchemeChoice.SEPIA.pageBackgroundArgb() to ColorSchemeChoice.SEPIA.pageForegroundArgb(),
            prefs.resolvedColors(),
        )
    }
}
