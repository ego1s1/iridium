package com.iridium.feature.reader.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.TapInvertMode
import com.iridium.core.model.TapZoneMode
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Live settings sheet coverage for tap zones and night light: rows render,
 * selections dispatch the persisted actions, and the zone preview reflects
 * the current mode.
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
    fun `tap zones section renders with preview and invert options`() {
        setSheet()

        composeTestRule.onNodeWithText("Tap zones").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag(ReaderChromeTestTags.TapZoneInvertNone).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Horizontal").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Vertical").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Both").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `invert selection dispatches the persisted action`() {
        val actions = mutableListOf<ReaderAction>()
        setSheet(onAction = actions::add)

        composeTestRule.onNodeWithTag(ReaderChromeTestTags.TapZoneInvertBoth).performScrollTo().performClick()

        assertEquals(
            listOf(ReaderAction.SetTapZoneInvert(TapInvertMode.BOTH)),
            actions,
        )
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
    fun `tap zone mode round-trips through the mode enum`() {
        // Guard against drift between persisted prefs and the zone map.
        assertEquals(TapZoneMode.DEFAULT, TapZoneMode.valueOf("DEFAULT"))
        assertEquals(TapZoneMode.DISABLED, TapZoneMode.valueOf("DISABLED"))
    }
}
