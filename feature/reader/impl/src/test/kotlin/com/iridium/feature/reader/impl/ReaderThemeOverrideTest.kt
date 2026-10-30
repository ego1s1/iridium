package com.iridium.feature.reader.impl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderThemeOverrideTest {

    @Test
    fun `pure black detection`() {
        assertTrue(ReaderThemeOverride.isPureBlack(-16777216)) // 0xFF000000
        assertFalse(ReaderThemeOverride.isPureBlack(-15592942)) // 0xFF121212
        assertFalse(ReaderThemeOverride.isPureBlack(-1)) // 0xFFFFFFFF
    }

    @Test
    fun `zero intensity returns foreground unchanged`() {
        val fg = -1
        assertEquals(fg, ReaderThemeOverride.warmForeground(fg, 0f))
    }

    @Test
    fun `warming moves white toward amber`() {
        val warmed = ReaderThemeOverride.warmForeground(-1, 1f)
        val r = (warmed shr 16) and 0xFF
        val g = (warmed shr 8) and 0xFF
        val b = warmed and 0xFF
        // Amber target (255,158,66): red stays max, green/blue drop.
        assertEquals(255, r)
        assertTrue(g in 150..230)
        assertTrue(b in 100..180)
    }

    @Test
    fun `dimForeground lowers alpha but keeps rgb and never goes transparent`() {
        val dimmed = ReaderThemeOverride.dimForeground(0xFFFFFFFF.toInt(), 1f)
        val alpha = (dimmed ushr 24) and 0xFF
        assertTrue("alpha should drop", alpha < 0xFF)
        assertTrue("alpha should stay legible", alpha >= 64)
        assertEquals(0x00FFFFFF, dimmed and 0x00FFFFFF)

        val untouched = ReaderThemeOverride.dimForeground(0xFFFFFFFF.toInt(), 0f)
        assertEquals(0xFF, (untouched ushr 24) and 0xFF)
    }

    @Test
    fun `light pages allow less dimming than dark ones`() {
        val lightCap = ReaderThemeOverride.maxDimFor(0xFFFFFFFF.toInt())
        val darkCap = ReaderThemeOverride.maxDimFor(0xFF000000.toInt())
        assertTrue("light must be capped lower", lightCap < darkCap)
        assertTrue(ReaderThemeOverride.isLightPage(0xFFF5E6C8.toInt()))
        assertTrue(!ReaderThemeOverride.isLightPage(0xFF000000.toInt()))
    }

    @Test
    fun `css carries the alpha channel when the foreground is dimmed`() {
        // Translucent colours are emitted as rgba(): #RRGGBBAA is CSS Color 4
        // and older WebViews drop the declaration, which silently disabled
        // text dimming.
        val css = ReaderThemeOverride.overrideCssColors("000000", "ffffff80")
        assertTrue(css.contains("rgba(255, 255, 255,"))
        // currentcolor can drop the alpha on some WebView builds, so the fill
        // colour must be the literal value.
        assertTrue(css.contains("-webkit-text-fill-color: rgba(255, 255, 255,"))
    }

    @Test
    fun `cssColor keeps hex for opaque colours and converts translucent ones`() {
        assertEquals("#ffffff", ReaderThemeOverride.cssColor("ffffff"))
        assertEquals("rgba(255, 0, 0, 1)", ReaderThemeOverride.cssColor("ff0000ff"))
        assertTrue(ReaderThemeOverride.cssColor("00000000").startsWith("rgba(0, 0, 0, 0"))
    }

    @Test
    fun `hexWithAlpha writes RRGGBBAA not AARRGGBB`() {
        assertEquals("11223380", ReaderThemeOverride.hexWithAlpha(0x80112233.toInt()))
    }

    @Test
    fun `scrub helpers round-trip`() {
        assertEquals(0, scrubIndexForProgression(0f, 10))
        assertEquals(9, scrubIndexForProgression(1f, 10))
        assertEquals(5, scrubIndexForProgression(0.55f, 10))
        assertEquals(0.5f, progressionForIndex(5, 11), 0.001f)
    }
}
