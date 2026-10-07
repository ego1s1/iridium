package com.iridium.feature.reader.impl

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Focused unit coverage for the [chromeZoneForTap] 6-mode zone map.
 * Pure function — direct assertions, no Compose runtime, plain JUnit.
 */
class ChromeTapZoneTest {

    private val ltr = ChromeReadingDirection.LEFT_TO_RIGHT
    private val rtl = ChromeReadingDirection.RIGHT_TO_LEFT
    private val third = 1f / 3f
    private val twoThirds = 2f / 3f

    @Test
    fun `default ltr splits into prev menu next thirds`() {
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.5f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.9f, 0.5f, ltr))
    }

    @Test
    fun `default uses navMode default parameter`() {
        // Same calls without the navMode arg must match DEFAULT thirds.
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.5f, ltr, ChromeNavMode.DEFAULT))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr, ChromeNavMode.DEFAULT))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.9f, 0.5f, ltr, ChromeNavMode.DEFAULT))
    }

    @Test
    fun `default rtl mirrors prev and next but keeps menu`() {
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.1f, 0.5f, rtl))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, rtl))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.9f, 0.5f, rtl))
    }

    @Test
    fun `top strip forces menu below five percent in every mode`() {
        ChromeNavMode.entries.forEach { mode ->
            assertEquals(
                "ltr $mode",
                ChromeTapZone.MENU,
                chromeZoneForTap(0.1f, 0.01f, ltr, mode),
            )
            assertEquals(
                "rtl $mode",
                ChromeTapZone.MENU,
                chromeZoneForTap(0.9f, 0.049f, rtl, mode),
            )
        }
    }

    @Test
    fun `top strip boundary is strict at five percent`() {
        // y = 0.05 escapes the strip; the mode zone applies (DEFAULT left third = PREV).
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
    fun `third boundaries fall back to menu in default`() {
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(third, 0.5f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(twoThirds, 0.5f, ltr))
        // Just inside the outer thirds navigates.
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(third - 0.01f, 0.5f, ltr))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(twoThirds + 0.01f, 0.5f, ltr))
    }

    @Test
    fun `l-shape bands navigate vertically with a menu center`() {
        val mode = ChromeNavMode.L_SHAPE
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.5f, 0.2f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.5f, 0.8f, ltr, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.5f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.9f, 0.5f, ltr, mode))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr, mode))
    }

    @Test
    fun `l-shape rtl mirrors prev and next but keeps menu`() {
        val mode = ChromeNavMode.L_SHAPE
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.5f, 0.2f, rtl, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.5f, 0.8f, rtl, mode))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, rtl, mode))
    }

    @Test
    fun `l-shape third boundaries resolve through the next band`() {
        val mode = ChromeNavMode.L_SHAPE
        // y exactly on a horizontal boundary is not in a band, so the x column applies.
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, third, ltr, mode))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, twoThirds, ltr, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, third, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.9f, twoThirds, ltr, mode))
    }

    @Test
    fun `kindlish keeps menu header prev column and next elsewhere`() {
        val mode = ChromeNavMode.KINDLISH
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.9f, 0.1f, ltr, mode))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.2f, ltr, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.9f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.9f, 0.9f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.5f, 0.5f, ltr, mode))
    }

    @Test
    fun `kindlish rtl mirrors prev and next but keeps the menu header`() {
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
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.05f, 0.5f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.95f, 0.9f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.1f, 0.1f, ltr, mode))
    }

    @Test
    fun `edge rtl mirrors prev and next but keeps the center menu`() {
        val mode = ChromeNavMode.EDGE
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, rtl, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.5f, 0.9f, rtl, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.05f, 0.5f, rtl, mode))
    }

    @Test
    fun `right-and-left routes sides without a double mirror`() {
        val mode = ChromeNavMode.RIGHT_AND_LEFT
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.05f, 0.5f, ltr, mode))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.95f, 0.5f, ltr, mode))
        // RTL inverts directly; the outer mirror must not flip it back.
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.05f, 0.5f, rtl, mode))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, rtl, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.95f, 0.5f, rtl, mode))
    }

    @Test
    fun `disabled always routes to menu`() {
        val mode = ChromeNavMode.DISABLED
        listOf(
            Pair(0.05f, 0.5f),
            Pair(0.5f, 0.5f),
            Pair(0.95f, 0.5f),
            Pair(0.5f, 0.01f),
            Pair(0.5f, 0.99f),
            Pair(0.1f, 0.9f),
        ).forEach { (x, y) ->
            assertEquals("tap $x,$y", ChromeTapZone.MENU, chromeZoneForTap(x, y, ltr, mode))
            assertEquals("rtl tap $x,$y", ChromeTapZone.MENU, chromeZoneForTap(x, y, rtl, mode))
        }
    }
}
