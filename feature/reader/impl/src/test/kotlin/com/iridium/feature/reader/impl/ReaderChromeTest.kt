package com.iridium.feature.reader.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import com.iridium.core.designsystem.IridiumTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `counter formats as one-based position over total`() {
        assertEquals("13 / 173", formatChromeCounter(13, 173))
    }

    @Test
    fun `scrubber island shows the position counter and full-width slider`() {
        composeTestRule.setContent {
            IridiumTheme {
                ReaderScrubberIsland(
                    positionIndex = 12,
                    positionCount = 173,
                    onSeek = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderChromeTestTags.ScrubberIsland).assertIsDisplayed()
        composeTestRule.onNodeWithText("13").assertIsDisplayed()
        composeTestRule.onNodeWithText("173").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.ScrubSlider).assertIsDisplayed()
    }

    @Test
    fun `scrubber seeks through the slider`() {
        var sought = -1f
        composeTestRule.setContent {
            IridiumTheme {
                ReaderScrubberIsland(
                    positionIndex = 12,
                    positionCount = 173,
                    onSeek = { sought = it },
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderChromeTestTags.ScrubSlider)
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        assertTrue(sought in 0f..1f)
    }

    @Test
    fun `zones split fixed thirds`() {
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.5f))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.9f, 0.5f))
    }

    @Test
    fun `invert switch mirrors the thirds for reachability`() {
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.1f, 0.5f, invertTaps = true))
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.5f, invertTaps = true))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.9f, 0.5f, invertTaps = true))
    }

    @Test
    fun `top strip always toggles chrome`() {
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.01f))
    }

    // Composables

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
