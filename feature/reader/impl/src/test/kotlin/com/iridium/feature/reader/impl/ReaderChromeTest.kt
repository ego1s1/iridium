package com.iridium.feature.reader.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.iridium.core.designsystem.IridiumTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Reader-chrome coverage: pure scrub/zone math plus composition of the
 * scrubber island and the EPUB dock (rendered directly — sheets and modals
 * do not settle under Robolectric).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderChromeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Pure helpers

    @Test
    fun `scrub math round-trips across the position range`() {
        assertEquals(0, scrubIndexForProgression(0f, 173))
        assertEquals(172, scrubIndexForProgression(1f, 173))
        assertEquals(86, scrubIndexForProgression(0.5f, 173))
        assertEquals(0, scrubIndexForProgression(-0.2f, 173))
        assertEquals(172, scrubIndexForProgression(1.4f, 173))
        assertEquals(0, scrubIndexForProgression(0.5f, 1))
        assertEquals(0f, progressionForIndex(0, 173), 0.0001f)
        assertEquals(1f, progressionForIndex(172, 173), 0.0001f)
        assertEquals(0.5f, progressionForIndex(86, 173), 0.01f)
        assertEquals(0f, progressionForIndex(0, 1), 0.0001f)
    }

    @Test
    fun `chrome navigation enables only away from the ends`() {
        assertEquals(false, canChromeGoBackward(0))
        assertEquals(true, canChromeGoBackward(1))
        assertEquals(true, canChromeGoForward(0, 173))
        assertEquals(false, canChromeGoForward(172, 173))
    }

    @Test
    fun `counter formats as one-based position over total`() {
        assertEquals("13 / 173", formatChromeCounter(13, 173))
    }

    @Test
    fun `zones split thirds and mirror for rtl`() {
        val ltr = ChromeReadingDirection.LEFT_TO_RIGHT
        val rtl = ChromeReadingDirection.RIGHT_TO_LEFT
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.5f, ltr))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.9f, 0.5f, ltr))
        // RTL mirrors forward: the left third advances.
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.1f, 0.5f, rtl))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.9f, 0.5f, rtl))
    }

    @Test
    fun `invert switch mirrors the thirds`() {
        val ltr = ChromeReadingDirection.LEFT_TO_RIGHT
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.1f, 0.5f, ltr, invertTaps = true))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, ltr, invertTaps = true))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.9f, 0.5f, ltr, invertTaps = true))
    }

    @Test
    fun `top strip always toggles chrome`() {
        assertEquals(
            ChromeTapZone.MENU,
            chromeZoneForTap(0.5f, 0.01f, ChromeReadingDirection.LEFT_TO_RIGHT),
        )
    }

    // Composables

    @Test
    fun `scrubber island shows the position counter and both steppers`() {
        composeTestRule.setContent {
            IridiumTheme {
                ReaderScrubberIsland(
                    positionIndex = 12,
                    positionCount = 173,
                    direction = ChromeReadingDirection.LEFT_TO_RIGHT,
                    onSeek = {},
                    onPrevious = {},
                    onNext = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderChromeTestTags.ScrubberIsland).assertIsDisplayed()
        composeTestRule.onNodeWithText("13").assertIsDisplayed()
        composeTestRule.onNodeWithText("173").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.ScrubSlider).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.ScrubPrev).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.ScrubNext).assertIsDisplayed()
    }

    @Test
    fun `scrubber steppers disable at the ends of the book`() {
        composeTestRule.setContent {
            IridiumTheme {
                ReaderScrubberIsland(
                    positionIndex = 0,
                    positionCount = 173,
                    direction = ChromeReadingDirection.LEFT_TO_RIGHT,
                    onSeek = {},
                    onPrevious = {},
                    onNext = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderChromeTestTags.ScrubPrev).assertIsNotEnabled()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.ScrubNext).assertIsEnabled()
    }

    @Test
    fun `stepper clicks reach the host callbacks`() {
        var previous = 0
        var next = 0
        composeTestRule.setContent {
            IridiumTheme {
                ReaderScrubberIsland(
                    positionIndex = 12,
                    positionCount = 173,
                    direction = ChromeReadingDirection.LEFT_TO_RIGHT,
                    onSeek = {},
                    onPrevious = { previous++ },
                    onNext = { next++ },
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderChromeTestTags.ScrubPrev).performClick()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.ScrubNext).performClick()
        assertEquals(1, previous)
        assertEquals(1, next)
    }

    @Test
    fun `epub dock exposes flow theme toc highlights and settings`() {
        var themeClicks = 0
        var settingsClicks = 0
        composeTestRule.setContent {
            IridiumTheme {
                ReaderEpubDock(
                    onFlowCycle = {},
                    onThemeClick = { themeClicks++ },
                    onTocClick = {},
                    onHighlightsClick = {},
                    onSettingsClick = { settingsClicks++ },
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderChromeTestTags.EpubDock).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.EpubFlowButton).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.EpubThemeButton).performClick()
        assertEquals(1, themeClicks)
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.EpubTocButton).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.EpubHighlightsButton).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.EpubSettingsButton).performClick()
        assertEquals(1, settingsClicks)
    }
}
