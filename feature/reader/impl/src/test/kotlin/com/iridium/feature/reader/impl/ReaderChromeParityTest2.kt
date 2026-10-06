package com.iridium.feature.reader.impl

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Second-wave reader-chrome parity: every tap-zone mode crossed with both
 * reading directions, plus boundary/clamp edges the first suite leaves out.
 * Pure zone math only — no composition, so plain JUnit (no Robolectric).
 */
class ReaderChromeParityTest2 {

    private val ltr = ChromeReadingDirection.LEFT_TO_RIGHT
    private val rtl = ChromeReadingDirection.RIGHT_TO_LEFT

    @Test
    fun `default ltr splits the surface into prev menu next thirds`() {
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.05f, 0.5f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.95f, 0.5f, ltr))
    }

    @Test
    fun `default rtl mirrors prev and next but keeps menu`() {
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.05f, 0.5f, rtl))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, rtl))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.95f, 0.5f, rtl))
    }

    @Test
    fun `third boundaries fall back to menu`() {
        val third = 1f / 3f
        val twoThirds = 2f / 3f
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(third, 0.5f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(twoThirds, 0.5f, ltr))
    }

    @Test
    fun `right-and-left ltr routes sides directly`() {
        val mode = ChromeNavMode.RIGHT_AND_LEFT
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.05f, 0.5f, ltr, mode))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.95f, 0.5f, ltr, mode))
    }

    @Test
    fun `right-and-left rtl inverts without a double mirror`() {
        val mode = ChromeNavMode.RIGHT_AND_LEFT
        // Left third advances under RTL; the outer RTL mirror must not flip it back.
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.05f, 0.5f, rtl, mode))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, rtl, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.95f, 0.5f, rtl, mode))
    }

    @Test
    fun `l-shape bands navigate vertically with a menu center`() {
        val mode = ChromeNavMode.L_SHAPE
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.5f, 0.2f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.5f, 0.8f, ltr, mode))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.5f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.9f, 0.5f, ltr, mode))
    }

    @Test
    fun `l-shape rtl mirrors side and band outcomes`() {
        val mode = ChromeNavMode.L_SHAPE
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.5f, 0.2f, rtl, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.5f, 0.8f, rtl, mode))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, rtl, mode))
    }

    @Test
    fun `kindlish keeps menu header prev column and next elsewhere`() {
        val mode = ChromeNavMode.KINDLISH
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.9f, 0.1f, ltr, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.9f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.9f, 0.9f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.5f, 0.5f, ltr, mode))
    }

    @Test
    fun `kindlish rtl mirrors the prev column into next`() {
        val mode = ChromeNavMode.KINDLISH
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.1f, rtl, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.1f, 0.6f, rtl, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.9f, 0.6f, rtl, mode))
    }

    @Test
    fun `edge centers menu with prev below and next everywhere else`() {
        val mode = ChromeNavMode.EDGE
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.5f, 0.9f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.05f, 0.05f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.05f, 0.5f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.95f, 0.9f, ltr, mode))
    }

    @Test
    fun `edge rtl mirrors prev and next but keeps the center menu`() {
        val mode = ChromeNavMode.EDGE
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, rtl, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.5f, 0.9f, rtl, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.05f, 0.5f, rtl, mode))
    }

    @Test
    fun `disabled routes every tap to menu`() {
        val mode = ChromeNavMode.DISABLED
        listOf(
            Pair(0.05f, 0.5f),
            Pair(0.5f, 0.5f),
            Pair(0.95f, 0.5f),
            Pair(0.5f, 0.01f),
            Pair(0.5f, 0.99f),
        ).forEach { (x, y) ->
            assertEquals("tap $x,$y", ChromeTapZone.MENU, chromeZoneForTap(x, y, ltr, mode))
            assertEquals("rtl tap $x,$y", ChromeTapZone.MENU, chromeZoneForTap(x, y, rtl, mode))
        }
    }

    @Test
    fun `top strip forces menu in every mode and direction`() {
        val modes = ChromeNavMode.entries.filter { it != ChromeNavMode.DISABLED }
        modes.forEach { mode ->
            assertEquals("ltr $mode", ChromeTapZone.MENU, chromeZoneForTap(0.1f, 0.01f, ltr, mode))
            assertEquals("rtl $mode", ChromeTapZone.MENU, chromeZoneForTap(0.9f, 0.049f, rtl, mode))
        }
    }

    @Test
    fun `top strip boundary is strict below five percent`() {
        // y = 0.05 escapes the strip: the mode's own zone applies.
        assertEquals(
            ChromeTapZone.PREV,
            chromeZoneForTap(0.1f, 0.05f, ltr, ChromeNavMode.DEFAULT),
        )
        assertEquals(
            ChromeTapZone.MENU,
            chromeZoneForTap(0.1f, 0.049f, ltr, ChromeNavMode.DEFAULT),
        )
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

    @Test
    fun `direction and fit cycles return to start after a full loop`() {
        var direction = ChromeReadingDirection.LEFT_TO_RIGHT
        repeat(2) { direction = nextChromeDirection(direction) }
        assertEquals(ChromeReadingDirection.LEFT_TO_RIGHT, direction)

        var fit = ChromePageFit.WIDTH
        repeat(3) { fit = nextChromeFit(fit) }
        assertEquals(ChromePageFit.WIDTH, fit)
    }
}
