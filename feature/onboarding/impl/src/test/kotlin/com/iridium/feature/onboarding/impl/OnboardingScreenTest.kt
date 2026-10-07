package com.iridium.feature.onboarding.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.model.ThemePreferences
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `welcome offers a way in and a way out`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                OnboardingScreen(
                    uiState = OnboardingUiState.Welcome,
                    onAction = {},
                    onOnboardingComplete = {},
                )
            }
        }
        // The hero staggers in, so the CTA appears a beat after composition.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Get started").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip").assertIsDisplayed()
    }

    @Test
    fun `get started reports the action`() {
        var started = false
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                OnboardingScreen(
                    uiState = OnboardingUiState.Welcome,
                    onAction = { if (it == OnboardingAction.GetStarted) started = true },
                    onOnboardingComplete = {},
                )
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Get started").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Get started").performClick()
        assertTrue(started)
    }

    @Test
    fun `the access step explains the permission and offers the grant`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                OnboardingScreen(
                    uiState = OnboardingUiState.Access,
                    onAction = {},
                    onOnboardingComplete = {},
                )
            }
        }
        composeTestRule.onNodeWithText("One permission first").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant access").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").assertIsDisplayed()
    }

    @Test
    fun `the reading step shows text size spacing and colors`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                OnboardingScreen(
                    uiState = OnboardingUiState.Reading(
                        com.iridium.core.model.ReaderPreferences(),
                    ),
                    onAction = {},
                    onOnboardingComplete = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Make reading yours").assertIsDisplayed()
        // Lower rows may sit below the fold in the small test window.
        composeTestRule.onNodeWithText("Text size").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Line spacing").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Book colors").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the appearance step shows the theme controls`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                OnboardingScreen(
                    uiState = OnboardingUiState.Appearance(ThemePreferences()),
                    onAction = {},
                    onOnboardingComplete = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Make it yours").assertIsDisplayed()
        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue").assertIsDisplayed()
    }
}
