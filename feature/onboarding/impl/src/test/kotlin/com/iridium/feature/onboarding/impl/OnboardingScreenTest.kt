package com.iridium.feature.onboarding.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
                    onPickFolder = {},
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
                    onPickFolder = {},
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
    fun `the folder step explains that nothing is copied and can be skipped`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                OnboardingScreen(
                    uiState = OnboardingUiState.Folder(pickerHintVisible = false),
                    onPickFolder = {},
                    onAction = {},
                    onOnboardingComplete = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Where are your books?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Continue without linking").assertIsDisplayed()
    }

    @Test
    fun `a dismissed picker shows the inline hint`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                OnboardingScreen(
                    uiState = OnboardingUiState.Folder(pickerHintVisible = true),
                    onPickFolder = {},
                    onAction = {},
                    onOnboardingComplete = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Choose folder").assertIsDisplayed()
        // The hint text is rendered when the picker was dismissed.
        composeTestRule.onNodeWithText(
            "Folder access is needed to read your books. Try again, or continue without linking.",
        ).assertIsDisplayed()
    }

    @Test
    fun `the appearance step shows the theme controls`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                OnboardingScreen(
                    uiState = OnboardingUiState.Appearance(ThemePreferences()),
                    onPickFolder = {},
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
