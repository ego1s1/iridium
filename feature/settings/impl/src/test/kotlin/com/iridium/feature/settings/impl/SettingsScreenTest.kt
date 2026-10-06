package com.iridium.feature.settings.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.model.AppColorScheme
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemeMode
import com.iridium.core.model.ThemePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sectionsAndExpressiveCardsRender() {
        val actions = mutableListOf<SettingsAction>()
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                SettingsScreen(
                    state = SettingsUiState(
                        theme = ThemePreferences(mode = ThemeMode.SYSTEM),
                        reader = ReaderPreferences(
                            fontScale = 1.0f,
                            pageMargins = 1.0f,
                            lineHeight = 1.4f,
                        ),
                    ),
                    appVersion = "1.0.0-test",
                    onAction = actions::add,
                )
            }
        }

        // Section cards
        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Motion").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Library").performScrollTo().assertIsDisplayed()

        // Reading section with squiggly slider rows
        composeTestRule.onNodeWithText("Reading").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Font size").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("100%").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Margins").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1.0x").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Line height").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1.40").performScrollTo().assertIsDisplayed()

        // Modular Storage section
        composeTestRule.onNodeWithText("Storage").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Storage location").performScrollTo().assertIsDisplayed()

        // About section
        composeTestRule.onNodeWithText("About").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1.0.0-test").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun appearanceAndPaletteOptionsRender() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                SettingsScreen(
                    state = SettingsUiState(),
                    appVersion = "1.0.0-test",
                    onAction = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeTestRule.onNodeWithText("System").assertIsDisplayed()
        composeTestRule.onNodeWithText("Light").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wallpaper color").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pure black").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dynamic").assertIsDisplayed()
        composeTestRule.onNodeWithText("Iridium").assertIsDisplayed()
    }

    @Test
    fun readingToggleDispatchesAction() {
        val actions = mutableListOf<SettingsAction>()
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                SettingsScreen(
                    state = SettingsUiState(),
                    appVersion = "1.0.0-test",
                    onAction = actions::add,
                )
            }
        }

        composeTestRule.onNodeWithText("Keep screen on").performScrollTo().performClick()
        assertTrue(actions.any { it is SettingsAction.SetKeepScreenOn && !it.enabled })
    }
}
