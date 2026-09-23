package com.iridium.feature.library.impl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.model.LibraryQuery
import com.iridium.core.testing.TestData
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun state(
        books: List<com.iridium.core.model.Book> = emptyList(),
        linked: Boolean = false,
        text: String = "",
    ) = LibraryUiState(
        books = books,
        query = LibraryQuery(text = text),
        refreshing = false,
        filterOpen = false,
        searchOpen = false,
        linked = linked,
        continueReading = emptyList(),
    )

    @Test
    fun `an unlinked library invites the user to link a folder`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibraryScreen(
                    uiState = state(),
                    onAction = {},
                    onReadClick = {},
                    onDetailsClick = {},
                )
            }
        }
        composeTestRule.onNodeWithText("No books yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Link folder").assertIsDisplayed()
    }

    @Test
    fun `an empty search result explains itself and offers a rescan`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibraryScreen(
                    uiState = state(linked = true, text = "zzz"),
                    onAction = {},
                    onReadClick = {},
                    onDetailsClick = {},
                )
            }
        }
        composeTestRule.onNodeWithText("No matches").assertIsDisplayed()
    }

    @Test
    fun `the empty-state action is wired to the callback`() {
        var linked = false
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibraryScreen(
                    uiState = state(),
                    onAction = {},
                    onReadClick = {},
                    onDetailsClick = {},
                    onLinkFolder = { linked = true },
                )
            }
        }
        composeTestRule.onNodeWithText("Link folder").performClick()
        assertTrue(linked)
    }

    @Test
    fun `books render their titles`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibraryScreen(
                    uiState = state(
                        books = listOf(TestData.book(id = "1", title = "Treasure Island")),
                        linked = true,
                    ),
                    onAction = {},
                    onReadClick = {},
                    onDetailsClick = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Treasure Island").assertIsDisplayed()
    }
}
