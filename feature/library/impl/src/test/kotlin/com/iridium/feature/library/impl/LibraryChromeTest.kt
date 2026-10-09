package com.iridium.feature.library.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.performSemanticsAction
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.model.LibraryFilter
import com.iridium.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Chrome parity tests: Mori section 3.1 building blocks adapted to EPUBs.
 * Covers the pure shelf derivation plus one UI test per chrome island.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalMaterial3Api::class)
class LibraryChromeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // --- Pure logic ---

    @Test
    fun `shelves derive counts from reading state`() {
        val books = listOf(
            TestData.book(id = "reading", progress = 0.4f, updatedAt = 3L),
            TestData.book(id = "fresh", progress = 0f, updatedAt = 2L),
            TestData.book(id = "done", progress = 1f, updatedAt = 1L),
            TestData.book(id = "loved", progress = 0f, bookmarked = true, updatedAt = 0L),
        )
        val shelves = libraryShelves(books).associateBy { it.id }
        assertEquals(1, shelves.getValue("continue").count)
        // Both untouched books count as unread, including the bookmarked one.
        assertEquals(2, shelves.getValue("unread").count)
        assertEquals(1, shelves.getValue("finished").count)
        assertEquals(1, shelves.getValue("favorites").count)
    }

    @Test
    fun `shelves map onto quick-filter values`() {
        val filters = libraryShelves(emptyList()).associate { it.id to it.filter }
        assertEquals(LibraryFilter.IN_PROGRESS, filters["continue"])
        assertEquals(LibraryFilter.UNREAD, filters["unread"])
        assertEquals(LibraryFilter.FINISHED, filters["finished"])
        assertEquals(LibraryFilter.FAVORITES, filters["favorites"])
    }

    // --- Collapsing top bar ---

    @Test
    fun `top bar shows 28sp title and filter action with active dot`() {
        var clicked = false
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibraryCollapsingTopBar(
                    title = "My books",
                    filterActive = true,
                    onFilterClick = { clicked = true },
                )
            }
        }
        composeTestRule.onNodeWithTag(LibraryChromeTestTags.TopBar).assertIsDisplayed()
        composeTestRule.onNodeWithText("My books").assertIsDisplayed()
        composeTestRule.onNodeWithTag(LibraryChromeTestTags.FilterDot).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LibraryChromeTestTags.FilterButton).performClick()
        assertTrue(clicked)
    }

    @Test
    fun `top bar hides the active dot when no filter applies`() {
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibraryCollapsingTopBar(
                    title = "My books",
                    filterActive = false,
                    onFilterClick = {},
                )
            }
        }
        composeTestRule.onNodeWithTag(LibraryChromeTestTags.FilterDot).assertDoesNotExist()
    }

    // --- Search island ---

    @Test
    fun `search island reports typed text`() {
        val seen = mutableListOf<String>()
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibrarySearchIsland(text = "", onTextChange = seen::add)
            }
        }
        composeTestRule.onNodeWithTag(LibraryChromeTestTags.SearchIsland).assertIsDisplayed()
        // Drive the same SetText path the IME uses (performTextInput no-ops
        // under Robolectric on this BOM: input never reaches onValueChange).
        composeTestRule.onNodeWithTag(LibraryChromeTestTags.SearchField)
            .performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString("dune")) }
        assertEquals(listOf("dune"), seen)
    }

    @Test
    fun `search island clear emits empty text`() {
        val seen = mutableListOf<String>()
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibrarySearchIsland(text = "dune", onTextChange = seen::add)
            }
        }
        composeTestRule.onNodeWithTag(LibraryChromeTestTags.SearchClear)
            .assertIsDisplayed()
            .performClick()
        assertEquals(listOf(""), seen)
    }

    // --- Quick filters ---

    @Test
    fun `quick filters cover the required segments and report selection`() {
        var selected = LibraryFilter.ALL
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                var current by remember { mutableStateOf(LibraryFilter.ALL) }
                LibraryQuickFilters(
                    selected = current,
                    onSelect = {
                        current = it
                        selected = it
                    },
                )
            }
        }
        composeTestRule.onNodeWithTag(LibraryChromeTestTags.QuickFilters).assertIsDisplayed()
        // Required parity segments per the task: All / In Progress / Unread / Favorites.
        composeTestRule.onNodeWithText("All").assertIsDisplayed()
        composeTestRule.onNodeWithText("In progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unread").assertIsDisplayed()
        composeTestRule.onNodeWithText("Favorites").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unread").performClick()
        assertEquals(LibraryFilter.UNREAD, selected)
    }

    // --- Shelves carousel ---

    @Test
    fun `shelves carousel shows counts and reports the shelf filter`() {
        val books = listOf(
            TestData.book(id = "reading", progress = 0.4f),
            TestData.book(id = "fresh", progress = 0f),
        )
        var picked: LibraryShelf? = null
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                LibraryShelvesCarousel(
                    shelves = libraryShelves(books),
                    selectedId = "unread",
                    onShelfSelect = { picked = it },
                )
            }
        }
        composeTestRule.onNodeWithTag(LibraryChromeTestTags.ShelvesCarousel).assertExists()
        composeTestRule.onNodeWithText("Continue reading").assertExists()
        composeTestRule.onNodeWithText("Unread").assertExists()
        composeTestRule.onNodeWithTag(LibraryChromeTestTags.shelfCard("finished"))
            .performClick()
        assertEquals(LibraryFilter.FINISHED, picked?.filter)
    }
}
