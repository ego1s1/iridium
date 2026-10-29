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
    fun `scrub helpers round-trip`() {
        assertEquals(0, scrubIndexForProgression(0f, 10))
        assertEquals(9, scrubIndexForProgression(1f, 10))
        assertEquals(5, scrubIndexForProgression(0.55f, 10))
        assertEquals(0.5f, progressionForIndex(5, 11), 0.001f)
    }
}
