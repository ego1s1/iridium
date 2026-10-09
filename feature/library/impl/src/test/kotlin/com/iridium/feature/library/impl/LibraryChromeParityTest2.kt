package com.iridium.feature.library.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.iridium.core.data.ContentHit
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.model.BookError
import com.iridium.core.model.LibraryFilter
import com.iridium.core.model.LibraryQuery
import com.iridium.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Second parity ring for library chrome: edge cases around shelf derivation
 * boundaries plus empty/loading/search states of [LibraryScreen].
 *
 * Complements [LibraryChromeTest] without touching sources.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalMaterial3Api::class)
class LibraryChromeParityTest2 {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun state(
        books: List<com.iridium.core.model.Book> = emptyList(),
        text: String = "",
        continueReading: List<com.iridium.core.model.Book> = emptyList(),
        contentHits: List<ContentHit> = emptyList(),
        refreshing: Boolean = false,
        indexProgress: IndexProgress? = null,
        indexing: Boolean = false,
    ) = LibraryUiState(
        books = books,
        query = LibraryQuery(text = text),
        refreshing = refreshing,
        filterOpen = false,
        continueReading = continueReading,
        indexProgress = indexProgress,
        contentHits = contentHits,
        indexing = indexing,
    )

    private fun setScreen(
        uiState: LibraryUiState,
        onAction: (LibraryAction) -> Unit = {},
        hasStorageAccess: Boolean = true,
    ) {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibraryScreen(
                    uiState = uiState,
                    hasStorageAccess = hasStorageAccess,
                    onGrantAccess = {},
                    onAction = onAction,
                    onReadClick = {},
                    onDetailsClick = {},
                )
            }
        }
    }

    // --- Shelf derivation edges ---

    @Test
    fun `empty library yields zeroed shelves in stable order`() {
        val shelves = libraryShelves(emptyList())
        assertEquals(listOf("continue", "unread", "finished", "favorites"), shelves.map { it.id })
        assertTrue(shelves.all { it.count == 0 })
    }

    @Test
    fun `shelf boundaries follow progress fractions`() {
        val books = listOf(
            TestData.book(id = "zero", progress = 0f, updatedAt = 1L),
            TestData.book(id = "half", progress = 0.5f, updatedAt = 2L),
            TestData.book(id = "nearly", progress = 0.99f, updatedAt = 3L),
            TestData.book(id = "done", progress = 1f, updatedAt = 4L),
        )
        val shelves = libraryShelves(books).associateBy { it.id }
        assertEquals(1, shelves.getValue("unread").count)
        assertEquals(2, shelves.getValue("continue").count)
        assertEquals(1, shelves.getValue("finished").count)
        assertEquals(0, shelves.getValue("favorites").count)
    }

    @Test
    fun `errored books never count as unread but keep favorite status`() {
        val books = listOf(
            TestData.book(id = "errFresh", progress = 0f, error = BookError.CORRUPT, bookmarked = true),
            TestData.book(id = "cleanFresh", progress = 0f),
            TestData.book(id = "loved", progress = 0f, bookmarked = true),
        )
        val shelves = libraryShelves(books).associateBy { it.id }
        // Only the two readable untouched books count as unread.
        assertEquals(2, shelves.getValue("unread").count)
        // Favorites ignore readability: the corrupt bookmark still counts.
        assertEquals(2, shelves.getValue("favorites").count)
    }

    @Test
    fun `in-progress shelf keeps errored readers while finished keeps errored completions`() {
        val books = listOf(
            TestData.book(id = "errReading", progress = 0.4f, error = BookError.CORRUPT),
            TestData.book(id = "errDone", progress = 1f, error = BookError.EMPTY),
        )
        val shelves = libraryShelves(books).associateBy { it.id }
        assertEquals(1, shelves.getValue("continue").count)
        assertEquals(1, shelves.getValue("finished").count)
        assertEquals(0, shelves.getValue("unread").count)
    }

    @Test
    fun `isEmpty reflects the book list`() {
        assertTrue(state().isEmpty)
        assertFalse(state(books = listOf(TestData.book())).isEmpty)
    }

    // --- Empty states ---

    @Test
    fun `granted empty library offers rescan`() {
        setScreen(state(), hasStorageAccess = true)
        composeTestRule.onNodeWithText("No books yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rescan library").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant access").assertDoesNotExist()
    }

    @Test
    fun `denied empty library invites the grant`() {
        setScreen(state(), hasStorageAccess = false)
        composeTestRule.onNodeWithText("No books yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant access").assertIsDisplayed()
    }

    @Test
    fun `searching offers rescan not grant`() {
        setScreen(state(text = "zzz"), hasStorageAccess = true)
        composeTestRule.onNodeWithText("No matches").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant access").assertDoesNotExist()
    }

    @Test
    fun `empty-state rescan action dispatches rescan`() {
        val actions = mutableListOf<LibraryAction>()
        setScreen(state(), onAction = actions::add)
        composeTestRule.onNodeWithText("Rescan library").performClick()
        assertTrue(actions.any { it is LibraryAction.Rescan })
    }

    // --- Search vs shelf ---

    @Test
    fun `continue shelf hides while searching`() {
        val book = TestData.book(id = "1", title = "Treasure Island", progress = 0.4f)
        setScreen(state(books = listOf(book), text = "treasure", continueReading = listOf(book)))
        composeTestRule.onNodeWithText("Continue reading").assertDoesNotExist()
        composeTestRule.onNodeWithText("Treasure Island").assertIsDisplayed()
    }

    @Test
    fun `now-reading hero shows without a query`() {
        val book = TestData.book(id = "1", title = "Treasure Island", progress = 0.4f)
        setScreen(state(books = listOf(book), continueReading = listOf(book)))
        composeTestRule.onNodeWithTag(LibraryTestTags.NowReadingHero).assertIsDisplayed()
        composeTestRule.onNodeWithText("NOW READING").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resume").assertIsDisplayed()
    }

    // --- Loading never hides content ---

    @Test
    fun `determinate scan progress never hides the grid`() {
        val book = TestData.book(id = "1", title = "Treasure Island")
        setScreen(
            state(
                books = listOf(book),
                refreshing = true,
                indexProgress = IndexProgress(done = 1, total = 4),
            ),
        )
        composeTestRule.onNodeWithText("Treasure Island").assertIsDisplayed()
    }

    @Test
    fun `chapter indexing never hides the grid`() {
        val book = TestData.book(id = "1", title = "Treasure Island")
        setScreen(state(books = listOf(book), indexing = true))
        composeTestRule.onNodeWithText("Treasure Island").assertIsDisplayed()
    }

    // --- Chrome islands ---

    @Test
    fun `quick filters include Finished and report it`() {
        var picked: LibraryFilter? = null
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibraryQuickFilters(selected = LibraryFilter.ALL, onSelect = { picked = it })
            }
        }
        composeTestRule.onNodeWithText("Finished").assertIsDisplayed()
        composeTestRule.onNodeWithText("Finished").performClick()
        assertEquals(LibraryFilter.FINISHED, picked)
    }

    @Test
    fun `search island hides clear when empty and shows custom placeholder`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibrarySearchIsland(text = "", onTextChange = {}, placeholder = "Custom placeholder")
            }
        }
        composeTestRule.onNodeWithText("Custom placeholder").assertIsDisplayed()
        composeTestRule.onNodeWithTag(LibraryChromeTestTags.SearchClear).assertDoesNotExist()
    }

    @Test
    fun `content hits header renders with the match count`() {
        val book = TestData.book(id = "1", title = "Treasure Island")
        val hits = listOf(
            ContentHit(
                bookId = "1",
                bookTitle = "Treasure Island",
                href = "OEBPS/ch1.xhtml",
                chapterTitle = "Chapter 1",
                snippet = "the Hispaniola was rolling",
            ),
        )
        setScreen(state(books = listOf(book), contentHits = hits))
        composeTestRule.onNodeWithText("In books (1)").assertIsDisplayed()
    }
}
