package com.iridium.feature.reader.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.iridium.core.designsystem.IridiumTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Expressive floating toolbar chrome: expanded/collapsed states, the
 * reduced-motion fallback (always expanded, instant), and host callback
 * wiring for the FAB plus all five reader actions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderChromeFloatingToolbarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `effective expansion follows the request only with expressive motion`() {
        assertEquals(true, resolveFloatingToolbarExpanded(true, true))
        assertEquals(false, resolveFloatingToolbarExpanded(false, true))
        assertEquals(true, resolveFloatingToolbarExpanded(true, false))
        assertEquals(true, resolveFloatingToolbarExpanded(false, false))
    }

    @Test
    fun `expanded toolbar shows the collapse FAB and all five actions`() {
        composeTestRule.setContent {
            IridiumTheme {
                ReaderChromeFloatingToolbar(
                    config = ReaderChromeConfig(),
                    expanded = true,
                    onExpandedChange = {},
                    onDirectionToggle = {},
                    onFitCycle = {},
                    onCropToggle = {},
                    onOverviewClick = {},
                    onSettingsClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderFloatingToolbarTestTags.Toolbar).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderFloatingToolbarTestTags.ExpandFab).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Collapse reader toolbar").assertIsDisplayed()
        listOf(
            ReaderChromeTestTags.DirectionButton,
            ReaderChromeTestTags.FitButton,
            ReaderChromeTestTags.CropButton,
            ReaderChromeTestTags.OverviewButton,
            ReaderChromeTestTags.SettingsButton,
        ).forEach { tag ->
            composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }

    @Test
    fun `FAB click reports the flipped expansion state`() {
        var expanded: Boolean? = null
        composeTestRule.setContent {
            IridiumTheme {
                ReaderChromeFloatingToolbar(
                    config = ReaderChromeConfig(),
                    expanded = true,
                    onExpandedChange = { expanded = it },
                    onDirectionToggle = {},
                    onFitCycle = {},
                    onCropToggle = {},
                    onOverviewClick = {},
                    onSettingsClick = {},
                )
            }
        }

        composeTestRule.onNodeWithTag(ReaderFloatingToolbarTestTags.ExpandFab).performClick()
        assertEquals(false, expanded)
    }

    @Test
    fun `reduced motion keeps the toolbar expanded and instant`() {
        var expanded: Boolean? = null
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                ReaderChromeFloatingToolbar(
                    config = ReaderChromeConfig(),
                    expanded = false,
                    onExpandedChange = { expanded = it },
                    onDirectionToggle = {},
                    onFitCycle = {},
                    onCropToggle = {},
                    onOverviewClick = {},
                    onSettingsClick = {},
                )
            }
        }

        // Still expanded: every action reachable, collapse affordance shown.
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.DirectionButton).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.SettingsButton).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Collapse reader toolbar").assertIsDisplayed()
        // The tap is still acknowledged to the host (haptics + callback).
        composeTestRule.onNodeWithTag(ReaderFloatingToolbarTestTags.ExpandFab).performClick()
        assertEquals(false, expanded)
    }

    @Test
    fun `toolbar action clicks reach the host callbacks`() {
        var docked = ""
        composeTestRule.setContent {
            IridiumTheme {
                ReaderChromeFloatingToolbar(
                    config = ReaderChromeConfig(),
                    expanded = true,
                    onExpandedChange = {},
                    onDirectionToggle = { docked = "direction" },
                    onFitCycle = { docked = "fit" },
                    onCropToggle = { docked = "crop" },
                    onOverviewClick = { docked = "overview" },
                    onSettingsClick = { docked = "settings" },
                )
            }
        }

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
}
