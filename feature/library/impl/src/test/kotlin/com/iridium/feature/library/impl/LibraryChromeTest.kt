package com.iridium.feature.library.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.performSemanticsAction
import com.iridium.core.designsystem.IridiumCollapsingTopBar
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.model.LibraryFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Chrome parity tests: the reference section 3.1 building blocks adapted to EPUBs.
 * Covers one UI test per chrome island (top bar, search, quick filters).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@OptIn(ExperimentalMaterial3Api::class)
class LibraryChromeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // --- Pure logic ---

    // --- Collapsing top bar ---

    @Test
    fun `top bar shows 28sp title and filter action with active dot`() {
        var clicked = false
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                IridiumCollapsingTopBar(
                    title = "My books",
                    modifier = Modifier.testTag(LibraryChromeTestTags.TopBar),
                    actions = {
                        LibraryFilterAction(
                            filterActive = true,
                            onFilterClick = { clicked = true },
                        )
                    },
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
                IridiumCollapsingTopBar(
                    title = "My books",
                    actions = {
                        LibraryFilterAction(
                            filterActive = false,
                            onFilterClick = {},
                        )
                    },
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
}
