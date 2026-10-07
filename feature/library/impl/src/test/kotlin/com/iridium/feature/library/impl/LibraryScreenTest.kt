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
        text: String = "",
    ) = LibraryUiState(
        books = books,
        query = LibraryQuery(text = text),
        refreshing = false,
        filterOpen = false,
        searchOpen = false,
        continueReading = emptyList(),
    )

    private fun setScreen(
        uiState: LibraryUiState = state(),
        hasStorageAccess: Boolean = false,
        onGrantAccess: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibraryScreen(
                    uiState = uiState,
                    hasStorageAccess = hasStorageAccess,
                    onGrantAccess = onGrantAccess,
                    onAction = {},
                    onReadClick = {},
                    onDetailsClick = {},
                )
            }
        }
    }

    @Test
    fun `a library without storage access invites the grant`() {
        setScreen()
        composeTestRule.onNodeWithText("No books yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant access").assertIsDisplayed()
    }

    @Test
    fun `a library with access but no books offers a rescan`() {
        setScreen(hasStorageAccess = true)
        composeTestRule.onNodeWithText("No books yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rescan library").assertIsDisplayed()
    }

    @Test
    fun `an empty search result explains itself and offers a rescan`() {
        setScreen(uiState = state(text = "zzz"), hasStorageAccess = true)
        composeTestRule.onNodeWithText("No matches").assertIsDisplayed()
    }

    @Test
    fun `the empty-state action is wired to the callback`() {
        var granted = false
        setScreen(onGrantAccess = { granted = true })
        composeTestRule.onNodeWithText("Grant access").performClick()
        assertTrue(granted)
    }

    @Test
    fun `books render their titles`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibraryScreen(
                    uiState = state(
                        books = listOf(TestData.book(id = "1", title = "Treasure Island")),
                    ),
                    hasStorageAccess = true,
                    onGrantAccess = {},
                    onAction = {},
                    onReadClick = {},
                    onDetailsClick = {},
                )
            }
        }
        composeTestRule.onNodeWithText("Treasure Island").assertIsDisplayed()
    }
}
