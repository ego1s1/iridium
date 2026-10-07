package com.iridium.feature.reader.impl

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Second-wave tap-zone coverage: invert switch crossed with both reading
 * directions, plus boundary/clamp edges the first suite leaves out.
 * Pure zone math only — no composition, so plain JUnit (no Robolectric).
 */
class ReaderChromeParityTest2 {

    private val ltr = ChromeReadingDirection.LEFT_TO_RIGHT
    private val rtl = ChromeReadingDirection.RIGHT_TO_LEFT

    @Test
    fun `thirds hold in both directions`() {
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.05f, 0.5f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.95f, 0.5f, ltr))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.05f, 0.5f, rtl))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, rtl))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.95f, 0.5f, rtl))
    }

    @Test
    fun `invert mirrors in both directions`() {
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.05f, 0.5f, ltr, invertTaps = true))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr, invertTaps = true))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.95f, 0.5f, ltr, invertTaps = true))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.05f, 0.5f, rtl, invertTaps = true))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, rtl, invertTaps = true))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.95f, 0.5f, rtl, invertTaps = true))
    }

    @Test
    fun `top strip forces menu in every direction and invert state`() {
        listOf(ltr, rtl).forEach { dir ->
            listOf(false, true).forEach { invert ->
                assertEquals(
                    "$dir invert=$invert",
                    ChromeTapZone.MENU,
                    chromeZoneForTap(0.1f, 0.01f, dir, invertTaps = invert),
                )
                assertEquals(
                    "$dir invert=$invert",
                    ChromeTapZone.MENU,
                    chromeZoneForTap(0.9f, 0.049f, dir, invertTaps = invert),
                )
            }
        }
    }

    @Test
    fun `top strip boundary is strict below five percent`() {
        // y = 0.05 escapes the strip: the thirds apply.
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.05f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.1f, 0.049f, ltr))
    }

    @Test
    fun `out-of-range fractions clamp to the surface edges`() {
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(-1f, 0.5f, ltr))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(2f, 0.5f, ltr))
        // Negative y clamps into the top strip.
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.1f, -0.5f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.1f, -0.5f, rtl))
    }

    @Test
    fun `scrub helpers clamp degenerate and out-of-range inputs`() {
        assertEquals(0, scrubIndexForProgression(0.9f, 1))
        assertEquals(0, scrubIndexForProgression(0.9f, 0))
        assertEquals(0f, progressionForIndex(-4, 173), 0.0001f)
        assertEquals(1f, progressionForIndex(999, 173), 0.0001f)
        assertEquals(false, canChromeGoForward(0, 1))
        assertEquals(false, canChromeGoBackward(0))
    }
}
