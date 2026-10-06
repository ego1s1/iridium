package com.iridium.feature.reader.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.model.ReadingFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Mori chrome parity coverage for the EPUB reader: pure scrub/zone math
 * plus composition of the scrubber island, action dock, and settings sheet
 * content (rendered directly — the modal sheet does not settle under
 * Robolectric).
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
    fun `direction and fit cycle through every option`() {
        assertEquals(
            ChromeReadingDirection.RIGHT_TO_LEFT,
            nextChromeDirection(ChromeReadingDirection.LEFT_TO_RIGHT),
        )
        assertEquals(
            ChromeReadingDirection.LEFT_TO_RIGHT,
            nextChromeDirection(ChromeReadingDirection.RIGHT_TO_LEFT),
        )
        assertEquals(ChromePageFit.HEIGHT, nextChromeFit(ChromePageFit.WIDTH))
        assertEquals(ChromePageFit.ORIGINAL, nextChromeFit(ChromePageFit.HEIGHT))
        assertEquals(ChromePageFit.WIDTH, nextChromeFit(ChromePageFit.ORIGINAL))
    }

    @Test
    fun `counter formats as one-based position over total`() {
        assertEquals("13 / 173", formatChromeCounter(13, 173))
    }

    @Test
    fun `default zones split thirds and mirror for rtl`() {
        assertEquals(
            ChromeTapZone.PREV,
            chromeZoneForTap(0.1f, 0.5f, ChromeReadingDirection.LEFT_TO_RIGHT),
        )
        assertEquals(
            ChromeTapZone.MENU,
            chromeZoneForTap(0.5f, 0.5f, ChromeReadingDirection.LEFT_TO_RIGHT),
        )
        assertEquals(
            ChromeTapZone.NEXT,
            chromeZoneForTap(0.9f, 0.5f, ChromeReadingDirection.LEFT_TO_RIGHT),
        )
        // RTL mirrors forward: the left third advances.
        assertEquals(
            ChromeTapZone.NEXT,
            chromeZoneForTap(0.1f, 0.5f, ChromeReadingDirection.RIGHT_TO_LEFT),
        )
        assertEquals(
            ChromeTapZone.PREV,
            chromeZoneForTap(0.9f, 0.5f, ChromeReadingDirection.RIGHT_TO_LEFT),
        )
    }

    @Test
    fun `top strip always toggles chrome and disabled routes to menu`() {
        ChromeNavMode.entries.forEach { mode ->
            assertEquals(
                "mode $mode",
                ChromeTapZone.MENU,
                chromeZoneForTap(
                    0.5f,
                    0.01f,
                    ChromeReadingDirection.LEFT_TO_RIGHT,
                    mode,
                ),
            )
        }
        assertEquals(
            ChromeTapZone.MENU,
            chromeZoneForTap(
                0.05f,
                0.6f,
                ChromeReadingDirection.LEFT_TO_RIGHT,
                ChromeNavMode.DISABLED,
            ),
        )
    }

    @Test
    fun `kindlish keeps a menu header with a narrow prev column`() {
        val mode = ChromeNavMode.KINDLISH
        val dir = ChromeReadingDirection.LEFT_TO_RIGHT
        assertEquals(ChromeTapZone.MENU, chromeZoneForTap(0.5f, 0.1f, dir, mode))
        assertEquals(ChromeTapZone.PREV, chromeZoneForTap(0.1f, 0.6f, dir, mode))
        assertEquals(ChromeTapZone.NEXT, chromeZoneForTap(0.6f, 0.6f, dir, mode))
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
    fun `action dock exposes all five mori actions`() {
        var docked = ""
        composeTestRule.setContent {
            IridiumTheme {
                ReaderActionDock(
                    config = ReaderChromeConfig(),
                    onDirectionToggle = { docked = "direction" },
                    onFitCycle = { docked = "fit" },
                    onCropToggle = { docked = "crop" },
                    onOverviewClick = { docked = "overview" },
                    onSettingsClick = { docked = "settings" },
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderChromeTestTags.ActionDock).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.DirectionButton).performClick()
        assertEquals("direction", docked)
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.FitButton).performClick()
        assertEquals("fit", docked)
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.CropButton).performClick()
        assertEquals("crop", docked)
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.OverviewButton).performClick()
        assertEquals("overview", docked)
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.SettingsButton).performClick()
        assertEquals("settings", docked)
    }

    @Test
    fun `settings content renders every mori section`() {
        composeTestRule.setContent {
            IridiumTheme {
                ReaderChromeSettingsContent(
                    config = ReaderChromeConfig(),
                    onDirectionChange = {},
                    onFitChange = {},
                    onCropChange = {},
                    onNavModeChange = {},
                    onFlowChange = {},
                )
            }
        }

        // Sections below the fold are scrolled into view (the sheet body
        // is a scrolled column, like the Mori settings sheet).
        composeTestRule.onNodeWithText("Reading settings").assertIsDisplayed()
        listOf("Flow", "Reading direction", "Page fit", "Crop page margins", "Tap zones")
            .forEach { section ->
                composeTestRule.onNodeWithText(section).performScrollTo()
                composeTestRule.onNodeWithText(section).assertIsDisplayed()
            }
        // One choice card per nav mode.
        ChromeNavMode.entries.forEach { mode ->
            composeTestRule
                .onNodeWithTag(ReaderChromeTestTags.navCardFor(mode))
                .performScrollTo()
            composeTestRule
                .onNodeWithTag(ReaderChromeTestTags.navCardFor(mode))
                .assertIsDisplayed()
        }
    }

    @Test
    fun `settings choices dispatch typed callbacks`() {
        var direction: ChromeReadingDirection? = null
        var fit: ChromePageFit? = null
        var navMode: ChromeNavMode? = null
        var flow: ReadingFlow? = null
        composeTestRule.setContent {
            IridiumTheme {
                ReaderChromeSettingsContent(
                    config = ReaderChromeConfig(),
                    onDirectionChange = { direction = it },
                    onFitChange = { fit = it },
                    onCropChange = {},
                    onNavModeChange = { navMode = it },
                    onFlowChange = { flow = it },
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderChromeTestTags.DirectionRtl).performScrollTo()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.DirectionRtl).performClick()
        assertEquals(ChromeReadingDirection.RIGHT_TO_LEFT, direction)
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.FitHeight).performScrollTo()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.FitHeight).performClick()
        assertEquals(ChromePageFit.HEIGHT, fit)
        composeTestRule
            .onNodeWithTag(ReaderChromeTestTags.navCardFor(ChromeNavMode.KINDLISH))
            .performScrollTo()
        composeTestRule
            .onNodeWithTag(ReaderChromeTestTags.navCardFor(ChromeNavMode.KINDLISH))
            .performClick()
        assertEquals(ChromeNavMode.KINDLISH, navMode)
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.FlowScrolled).performScrollTo()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.FlowScrolled).performClick()
        assertEquals(ReadingFlow.SCROLLED, flow)
    }
}
