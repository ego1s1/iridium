package com.iridium.app

import androidx.compose.ui.test.assertIsDisplayed
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainNavigatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tapping a destination reports its index`() {
        var selected = -1
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                MainNavigator(selectedTab = 0, onSelectTab = { selected = it })
            }
        }
        composeTestRule.onNodeWithTag(MainTestTags.HistoryTab).performClick()
        assertEquals(1, selected)

        composeTestRule.onNodeWithTag(MainTestTags.SettingsTab).performClick()
        assertEquals(2, selected)
    }

    @Test
    fun `only the selected destination shows its label`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                MainNavigator(selectedTab = 2, onSelectTab = {})
            }
        }
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
    }
}
