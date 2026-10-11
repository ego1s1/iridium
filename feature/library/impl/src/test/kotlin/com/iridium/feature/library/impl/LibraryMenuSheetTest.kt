package com.iridium.feature.library.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.testing.TestData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Subtitle copy on the favorites row is state-aware: "add" copy when the book
 * is not bookmarked, "Bookmarked" once it is (B2 regression).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryMenuSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setMenu(book: com.iridium.core.model.Book) {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibraryMenuContent(
                    book = book,
                    deleteConfirm = false,
                    onAction = {},
                    onReadClick = {},
                )
            }
        }
    }

    @Test
    fun `unbookmarked book shows add-favorite subtitle`() {
        setMenu(TestData.book(id = "1", title = "Treasure Island", bookmarked = false))
        composeTestRule.onNodeWithText("Mark this book as a favorite").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bookmarked").assertDoesNotExist()
    }

    @Test
    fun `bookmarked book shows Bookmarked subtitle`() {
        setMenu(TestData.book(id = "1", title = "Treasure Island", bookmarked = true))
        composeTestRule.onNodeWithText("Bookmarked").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mark this book as a favorite").assertDoesNotExist()
    }
}
