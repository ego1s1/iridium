package com.iridium.feature.reader.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.ReaderPreferences
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Live settings sheet coverage for tap inversion and night light: rows
 * render, selections dispatch the persisted actions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderSettingsSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setSheet(
        prefs: ReaderPreferences = ReaderPreferences(),
        onAction: (ReaderAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                // Content directly: the modal sheet does not settle clicks
                // under Robolectric.
                ReaderSettingsContent(prefs = prefs, onAction = onAction)
            }
        }
    }

    @Test
    fun `invert switch renders off by default`() {
        setSheet()

        composeTestRule.onNodeWithText("Invert tap zones").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Invert tap zones").assertIsOff()
    }

    @Test
    fun `invert switch reflects prefs and dispatches the persisted action`() {
        val actions = mutableListOf<ReaderAction>()
        setSheet(
            prefs = ReaderPreferences(invertTaps = true),
            onAction = actions::add,
        )

        composeTestRule.onNodeWithText("Invert tap zones").performScrollTo().assertIsOn()
        composeTestRule.onNodeWithText("Invert tap zones").performClick()

        assertEquals(listOf(ReaderAction.SetInvertTaps(false)), actions)
    }

    @Test
    fun `enabling invert dispatches true`() {
        val actions = mutableListOf<ReaderAction>()
        setSheet(onAction = actions::add)

        composeTestRule.onNodeWithText("Invert tap zones").performScrollTo().performClick()

        assertEquals(listOf(ReaderAction.SetInvertTaps(true)), actions)
    }

    @Test
    fun `night light switch toggles and reveals warmth`() {
        val actions = mutableListOf<ReaderAction>()
        setSheet(
            prefs = ReaderPreferences(nightLight = true, nightLightIntensity = 0.5f),
            onAction = actions::add,
        )

        composeTestRule.onNodeWithText("Night light").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Warmth").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("50%").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `night light warmth hides while the light is off`() {
        setSheet(prefs = ReaderPreferences(nightLight = false))

        composeTestRule.onNodeWithText("Night light").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Warmth").assertDoesNotExist()
    }

    @Test
    fun `night light color scales alpha with intensity`() {
        assertEquals(0, (nightLightColor(0f).alpha * 255).roundToInt())
        assertEquals(102, (nightLightColor(1f).alpha * 255).roundToInt())
        assertEquals(26, (nightLightColor(0.25f).alpha * 255).roundToInt())
        assertEquals(102, (nightLightColor(99f).alpha * 255).roundToInt())
        assertEquals(0, (nightLightColor(-1f).alpha * 255).roundToInt())
    }

    @Test
    fun `theme sheet previews every scheme and dispatches selection`() {
        val actions = mutableListOf<ReaderAction>()
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                ReaderThemeSheetContent(
                    selected = ColorSchemeChoice.SEPIA,
                    onAction = actions::add,
                )
            }
        }

        listOf("Reading theme", "Light", "Sepia", "Grey", "Dark", "Pitch black").forEach {
            composeTestRule.onNodeWithText(it).assertIsDisplayed()
        }
        composeTestRule.onNodeWithText("Dark").performClick()
        assertEquals(listOf(ReaderAction.SetTheme(ColorSchemeChoice.DARK)), actions)
    }
}
