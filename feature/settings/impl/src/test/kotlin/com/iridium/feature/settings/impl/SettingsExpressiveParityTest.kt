package com.iridium.feature.settings.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.model.LibrarySortOrder
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemeMode
import com.iridium.core.model.ThemePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Second parity ring for settings expressive UI: slider value formatting at
 * range extremes, every section rendering under extreme prefs, and action
 * dispatch (the UI half of persistence) for motion/sort/toggles/theme.
 *
 * Complements [SettingsScreenTest] without touching sources.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsExpressiveParityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(state: SettingsUiState, onAction: (SettingsAction) -> Unit = {}) {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                SettingsScreen(
                    state = state,
                    appVersion = "9.9.9-t10",
                    onAction = onAction,
                )
            }
        }
    }

    @Test
    fun `defaults match the parity baseline`() {
        val state = SettingsUiState()
        assertEquals(ThemeMode.SYSTEM, state.theme.mode)
        assertEquals(MotionStyle.EXPRESSIVE, state.motionStyle)
        assertEquals(1.0f, state.reader.fontScale)
        assertEquals(1.0f, state.reader.pageMargins)
        assertEquals(1.4f, state.reader.lineHeight)
        assertFalse(state.crashReportingEnabled)
    }

    @Test
    fun `slider minimums format as parity copy`() {
        setScreen(
            SettingsUiState(
                reader = ReaderPreferences(fontScale = 0.5f, pageMargins = 0.5f, lineHeight = 1.0f),
            ),
        )
        composeTestRule.onNodeWithText("50%").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("0.5x").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("1.00").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `slider maximums format as parity copy`() {
        setScreen(
            SettingsUiState(
                reader = ReaderPreferences(fontScale = 2.5f, pageMargins = 3.0f, lineHeight = 2.5f),
            ),
        )
        composeTestRule.onNodeWithText("250%").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("3.0x").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("2.50").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `all sections render under extreme prefs`() {
        setScreen(
            SettingsUiState(
                theme = ThemePreferences(mode = ThemeMode.DARK, amoled = true),
                motionStyle = MotionStyle.CALM,
                reader = ReaderPreferences(fontScale = 2.5f, keepScreenOn = false),
            ),
        )
        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Motion").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Expressive").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Calm").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Library").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Reading").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Storage").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("About").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("9.9.9-t10").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `motion toggle dispatches calm`() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(SettingsUiState(), onAction = actions::add)
        composeTestRule.onNodeWithText("Calm").performScrollTo().performClick()
        assertTrue(actions.any { it is SettingsAction.SetMotionStyle && it.style == MotionStyle.CALM })
    }

    @Test
    fun `motion toggle dispatches expressive from calm`() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(SettingsUiState(motionStyle = MotionStyle.CALM), onAction = actions::add)
        composeTestRule.onNodeWithText("Expressive").performScrollTo().performClick()
        assertTrue(actions.any { it is SettingsAction.SetMotionStyle && it.style == MotionStyle.EXPRESSIVE })
    }

    @Test
    fun `library sort dispatches title`() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(SettingsUiState(), onAction = actions::add)
        composeTestRule.onNodeWithText("Title").performScrollTo().performClick()
        assertTrue(actions.any { it is SettingsAction.SetSortOrder && it.order == LibrarySortOrder.TITLE })
    }

    @Test
    fun `hide-errors switch dispatches`() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(SettingsUiState(), onAction = actions::add)
        composeTestRule.onNodeWithText("Hide unreadable books").performScrollTo().performClick()
        assertTrue(actions.any { it is SettingsAction.SetHideErrors && it.hide })
    }

    @Test
    fun `pure black dispatches when dark`() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(
            SettingsUiState(theme = ThemePreferences(mode = ThemeMode.DARK, amoled = false)),
            onAction = actions::add,
        )
        composeTestRule.onNodeWithText("Pure black").performScrollTo().performClick()
        assertTrue(actions.any { it is SettingsAction.SetAmoled && it.enabled })
    }

    @Test
    fun `pure black never dispatches when light`() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(
            SettingsUiState(theme = ThemePreferences(mode = ThemeMode.LIGHT, amoled = false)),
            onAction = actions::add,
        )
        composeTestRule.onNodeWithText("Pure black").performScrollTo()
        runCatching {
            composeTestRule.onNodeWithText("Pure black").performClick()
        }
        assertTrue(actions.none { it is SettingsAction.SetAmoled })
    }

    @Test
    fun `reading toggles dispatch page-counter and volume-keys`() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(SettingsUiState(), onAction = actions::add)
        composeTestRule.onNodeWithText("Show page counter").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Volume keys turn pages").performScrollTo().performClick()
        assertTrue(actions.any { it is SettingsAction.SetShowPageCounter && !it.enabled })
        assertTrue(actions.any { it is SettingsAction.SetVolumeKeys && it.enabled })
    }

    @Test
    fun `crash reporting dispatches`() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(SettingsUiState(crashReportingEnabled = false), onAction = actions::add)
        composeTestRule.onNodeWithText("Send crash reports").performScrollTo().performClick()
        assertTrue(actions.any { it is SettingsAction.SetCrashReporting && it.enabled })
    }

    @Test
    fun `scheme picker dispatches dynamic color`() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(SettingsUiState(), onAction = actions::add)
        composeTestRule.onNodeWithText("Dynamic").performScrollTo().performClick()
        assertTrue(actions.any { it is SettingsAction.SetDynamicColor && it.enabled })
    }

    @Test
    fun `scheme picker dispatches forest preset`() {
        val actions = mutableListOf<SettingsAction>()
        setScreen(SettingsUiState(), onAction = actions::add)
        composeTestRule.onNodeWithText("Forest").performScrollTo().performClick()
        assertTrue(
            actions.any {
                it is SettingsAction.SetColorScheme &&
                    it.scheme == com.iridium.core.model.AppColorScheme.FOREST
            },
        )
    }
}
