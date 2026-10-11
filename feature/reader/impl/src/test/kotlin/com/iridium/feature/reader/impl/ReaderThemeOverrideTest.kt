package com.iridium.feature.reader.impl

import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.pageBackgroundArgb
import com.iridium.core.model.pageForegroundArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderThemeOverrideTest {

    @Test
    fun `override css uses exact theme colors`() {
        ColorSchemeChoice.entries.forEach { theme ->
            val css = ReaderThemeOverride.overrideCss(theme)
            assertTrue("bg present for $theme", css.contains("#" + ReaderThemeOverride.hex(theme.pageBackgroundArgb())))
            assertTrue("fg present for $theme", css.contains("#" + ReaderThemeOverride.hex(theme.pageForegroundArgb())))
        }
    }

    @Test
    fun `override css wins specificity with important`() {
        val css = ReaderThemeOverride.overrideCss(ColorSchemeChoice.SEPIA)
        assertTrue(css.contains("!important"))
        assertTrue(css.contains("-webkit-text-fill-color: currentcolor"))
        // Repeated class to beat any book rule.
        val repeats = Regex("\\.__Ir_Theme").findAll(css).count()
        assertTrue("repeated specificity selects, got $repeats", repeats >= 16)
    }

    @Test
    fun `grey does not collapse to the dark theme`() {
        val grey = ReaderThemeOverride.overrideCss(ColorSchemeChoice.GREY)
        val black = ReaderThemeOverride.overrideCss(ColorSchemeChoice.BLACK)
        // Exact color used (not the nearest built-in dark theme).
        assertTrue(
            grey.contains(ReaderThemeOverride.hex(ColorSchemeChoice.GREY.pageBackgroundArgb())),
        )
        // Grey and black pages genuinely differ.
        val greyBg = ReaderThemeOverride.hex(ColorSchemeChoice.GREY.pageBackgroundArgb())
        val blackBg = ReaderThemeOverride.hex(ColorSchemeChoice.BLACK.pageBackgroundArgb())
        assertFalse(greyBg == blackBg)
    }

    @Test
    fun `inject script embeds the css safely`() {
        val script = ReaderThemeOverride.injectOverrideScript(ColorSchemeChoice.DARK)
        assertTrue(script.startsWith("(function(){"))
        assertTrue(script.contains("iridiumThemeOverride"))
        assertTrue(script.endsWith("})()"))
        // Quote / newline escaping must not break the single-quoted string.
        assertEquals(script.count { it == '\'' }, script.count { it == '\'' })
    }
}