package com.iridium.feature.reader.impl

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Focused unit coverage for the [chromeZoneForTap] fixed-thirds zone map.
 * Pure function — direct assertions, no Compose runtime, plain JUnit.
 */
class ChromeTapZoneTest {

    private val ltr = ChromeReadingDirection.LEFT_TO_RIGHT
    private val rtl = ChromeReadingDirection.RIGHT_TO_LEFT
    private val third = 1f / 3f
    private val twoThirds = 2f / 3f

    @Test
    fun `ltr splits into prev menu next thirds`() {
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.5f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.9f, 0.5f, ltr))
    }

    @Test
    fun `rtl mirrors prev and next but keeps menu`() {
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.1f, 0.5f, rtl))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, rtl))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.9f, 0.5f, rtl))
    }

    @Test
    fun `invert switch mirrors the thirds`() {
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.1f, 0.5f, ltr, invertTaps = true))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr, invertTaps = true))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.9f, 0.5f, ltr, invertTaps = true))
    }

    @Test
    fun `invert and rtl cancel each other out`() {
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.5f, rtl, invertTaps = true))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.9f, 0.5f, rtl, invertTaps = true))
    }

    @Test
    fun `top strip always toggles chrome`() {
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.1f, 0.01f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.9f, 0.049f, rtl))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.1f, 0.01f, ltr, invertTaps = true))
    }

    @Test
    fun `top strip boundary is strict at five percent`() {
        // y = 0.05 escapes the strip; the thirds apply (left third = PREV).
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.05f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.1f, 0.049f, ltr))
    }

    @Test
    fun `third boundaries fall back to menu`() {
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(third, 0.5f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(twoThirds, 0.5f, ltr))
        // Just inside the outer thirds navigates.
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(third - 0.01f, 0.5f, ltr))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(twoThirds + 0.01f, 0.5f, ltr))
    }

    @Test
    fun `out-of-range fractions clamp to the surface edges`() {
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(-1f, 0.5f, ltr))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(2f, 0.5f, ltr))
        // Negative y clamps into the top strip.
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.1f, -0.5f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.1f, -0.5f, rtl))
    }
}
