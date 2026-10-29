package com.iridium.feature.reader.impl

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Second-wave tap-zone coverage: invert switch crossed with both reading
 * directions, plus boundary/clamp edges the first suite leaves out.
 * Pure zone math only — no composition, so plain JUnit (no Robolectric).
 */
class ReaderChromeParityTest2 {

    @Test
    fun `thirds hold across the surface`() {
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.05f, 0.5f))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.95f, 0.5f))
    }

    @Test
    fun `invert mirrors the thirds`() {
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.05f, 0.5f, invertTaps = true))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, invertTaps = true))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.95f, 0.5f, invertTaps = true))
    }

    @Test
    fun `top strip forces menu in every invert state`() {
        listOf(false, true).forEach { invert ->
            assertEquals(
                "invert=$invert",
                ChromeTapZone.MENU,
                chromeZoneForTap(0.1f, 0.01f, invertTaps = invert),
            )
            assertEquals(
                "invert=$invert",
                ChromeTapZone.MENU,
                chromeZoneForTap(0.9f, 0.049f, invertTaps = invert),
            )
        }
    }

    @Test
    fun `top strip boundary is strict below five percent`() {
        // y = 0.05 escapes the strip: the thirds apply.
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.05f))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.1f, 0.049f))
    }

    @Test
    fun `out-of-range fractions clamp to the surface edges`() {
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(-1f, 0.5f))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(2f, 0.5f))
        // Negative y clamps into the top strip.
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.1f, -0.5f))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.1f, -0.5f, invertTaps = true))
    }

    @Test
    fun `scrub helpers clamp degenerate and out-of-range inputs`() {
        assertEquals(0, scrubIndexForProgression(0.9f, 1))
        assertEquals(0, scrubIndexForProgression(0.9f, 0))
        assertEquals(0f, progressionForIndex(-4, 173), 0.0001f)
        assertEquals(1f, progressionForIndex(999, 173), 0.0001f)
    }
}
