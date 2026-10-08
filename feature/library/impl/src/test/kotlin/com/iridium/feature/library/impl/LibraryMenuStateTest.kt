package com.iridium.feature.library.impl

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.iridium.core.fakes.TestBooksRepository
import com.iridium.core.fakes.TestPreferencesDataSource
import com.iridium.core.testing.TestData
import com.iridium.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryMenuStateTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = TestBooksRepository()
    private val preferences = TestPreferencesDataSource()
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setup() {
        viewModel = LibraryViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            preferences = preferences,
        )
    }

    private suspend fun ReceiveTurbine<LibraryUiState>.awaitStateMatching(
        predicate: (LibraryUiState) -> Boolean,
    ): LibraryUiState {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }

    @Test
    fun `open menu sets menuBook and clears delete confirm`() = runTest {
        repository.setBooks(
            listOf(
                TestData.book(id = "1"),
                TestData.book(id = "2"),
            ),
        )

        viewModel.uiState.test {
            awaitStateMatching { it.books.size == 2 }

            viewModel.onAction(LibraryAction.OpenMenu("1"))
            val opened = awaitStateMatching { it.menuBook?.id == "1" }
            assertFalse(opened.menuDeleteConfirm)

            viewModel.onAction(LibraryAction.OpenMenuDelete)
            val confirming = awaitStateMatching { it.menuDeleteConfirm }
            assertEquals("1", confirming.menuBook?.id)

            // Opening another book's menu resets the confirm flag.
            viewModel.onAction(LibraryAction.OpenMenu("2"))
            val switched = awaitStateMatching { it.menuBook?.id == "2" && !it.menuDeleteConfirm }
            assertEquals("2", switched.menuBook?.id)
            assertFalse(switched.menuDeleteConfirm)
        }
    }

    @Test
    fun `close menu clears menuBook and delete confirm`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "1")))

        viewModel.uiState.test {
            awaitStateMatching { it.books.size == 1 }

            viewModel.onAction(LibraryAction.OpenMenu("1"))
            awaitStateMatching { it.menuBook?.id == "1" }

            viewModel.onAction(LibraryAction.OpenMenuDelete)
            awaitStateMatching { it.menuDeleteConfirm }

            viewModel.onAction(LibraryAction.CloseMenu)
            val closed = awaitStateMatching { it.menuBook?.id == null && !it.menuDeleteConfirm }
            assertNull(closed.menuBook?.id)
            assertFalse(closed.menuDeleteConfirm)
        }
    }

    @Test
    fun `toggle menu bookmark sets bookmarked through the repository`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "1", bookmarked = false)))

        viewModel.uiState.test {
            awaitStateMatching { it.books.size == 1 }

            viewModel.onAction(LibraryAction.OpenMenu("1"))
            awaitStateMatching { it.menuBook?.id == "1" }

            viewModel.onAction(LibraryAction.ToggleMenuBookmark)
            val updated = awaitStateMatching {
                it.books.firstOrNull { book -> book.id == "1" }?.bookmarked == true
            }
            assertTrue(updated.books.first { book -> book.id == "1" }.bookmarked)
            // The menu stays open across the toggle.
            assertEquals("1", updated.menuBook?.id)
        }

        assertEquals(true, repository.observeBook("1").first()?.bookmarked)
    }

    @Test
    fun `toggle menu bookmark clears an existing bookmark`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "1", bookmarked = true)))

        viewModel.uiState.test {
            awaitStateMatching { it.books.size == 1 }

            viewModel.onAction(LibraryAction.OpenMenu("1"))
            awaitStateMatching { it.menuBook?.id == "1" }

            viewModel.onAction(LibraryAction.ToggleMenuBookmark)
            val updated = awaitStateMatching {
                it.books.firstOrNull { book -> book.id == "1" }?.bookmarked == false
            }
            assertFalse(updated.books.first { book -> book.id == "1" }.bookmarked)
            assertEquals("1", updated.menuBook?.id)
        }

        assertEquals(false, repository.observeBook("1").first()?.bookmarked)
    }

    @Test
    fun `open menu delete only shows the confirm flag`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "1")))

        viewModel.uiState.test {
            awaitStateMatching { it.books.size == 1 }

            viewModel.onAction(LibraryAction.OpenMenu("1"))
            awaitStateMatching { it.menuBook?.id == "1" }

            viewModel.onAction(LibraryAction.OpenMenuDelete)
            val confirming = awaitStateMatching { it.menuDeleteConfirm }
            assertTrue(confirming.menuDeleteConfirm)
            assertEquals("1", confirming.menuBook?.id)
            // The book is not removed until confirmed.
            assertEquals(listOf("1"), confirming.books.map { it.id })
        }

        assertEquals("1", repository.observeBook("1").first()?.id)
    }

    @Test
    fun `confirm menu delete removes the book and closes the menu`() = runTest {
        repository.setBooks(
            listOf(
                TestData.book(id = "1"),
                TestData.book(id = "2"),
            ),
        )

        viewModel.uiState.test {
            awaitStateMatching { it.books.size == 2 }

            viewModel.onAction(LibraryAction.OpenMenu("1"))
            awaitStateMatching { it.menuBook?.id == "1" }

            viewModel.onAction(LibraryAction.OpenMenuDelete)
            awaitStateMatching { it.menuDeleteConfirm }

            viewModel.onAction(LibraryAction.ConfirmMenuDelete)
            val done = awaitStateMatching {
                it.menuBook?.id == null && !it.menuDeleteConfirm &&
                    it.books.none { book -> book.id == "1" }
            }
            assertNull(done.menuBook?.id)
            assertFalse(done.menuDeleteConfirm)
            assertEquals(listOf("2"), done.books.map { it.id })
        }

        assertNull(repository.observeBook("1").first())
        assertEquals("2", repository.observeBook("2").first()?.id)
    }

    @Test
    fun `toggle menu bookmark without an open menu is a no-op`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "1", bookmarked = false)))

        viewModel.onAction(LibraryAction.ToggleMenuBookmark)

        assertEquals(false, repository.observeBook("1").first()?.bookmarked)

        viewModel.uiState.test {
            val state = awaitStateMatching { it.books.size == 1 }
            assertNull(state.menuBook?.id)
            assertFalse(state.menuDeleteConfirm)
            assertFalse(state.books.first { it.id == "1" }.bookmarked)
        }
    }

    @Test
    fun `bookmark toggle does not affect other books`() = runTest {
        repository.setBooks(
            listOf(
                TestData.book(id = "1", bookmarked = false),
                TestData.book(id = "2", bookmarked = false),
            ),
        )

        viewModel.uiState.test {
            awaitStateMatching { it.books.size == 2 }

            viewModel.onAction(LibraryAction.OpenMenu("1"))
            awaitStateMatching { it.menuBook?.id == "1" }

            viewModel.onAction(LibraryAction.ToggleMenuBookmark)
            val updated = awaitStateMatching {
                it.books.firstOrNull { book -> book.id == "1" }?.bookmarked == true
            }
            assertTrue(updated.books.first { it.id == "1" }.bookmarked)
            assertFalse(updated.books.first { it.id == "2" }.bookmarked)
        }

        assertEquals(true, repository.observeBook("1").first()?.bookmarked)
        assertEquals(false, repository.observeBook("2").first()?.bookmarked)
    }

    @Test
    fun `confirm delete removes only the open book`() = runTest {
        repository.setBooks(
            listOf(
                TestData.book(id = "1"),
                TestData.book(id = "2"),
            ),
        )

        viewModel.uiState.test {
            awaitStateMatching { it.books.size == 2 }

            viewModel.onAction(LibraryAction.OpenMenu("2"))
            awaitStateMatching { it.menuBook?.id == "2" }

            viewModel.onAction(LibraryAction.OpenMenuDelete)
            awaitStateMatching { it.menuDeleteConfirm }

            viewModel.onAction(LibraryAction.ConfirmMenuDelete)
            val done = awaitStateMatching {
                it.menuBook?.id == null && it.books.map { book -> book.id } == listOf("1")
            }
            assertEquals(listOf("1"), done.books.map { it.id })
            assertFalse(done.menuDeleteConfirm)
        }

        assertEquals("1", repository.observeBook("1").first()?.id)
        assertNull(repository.observeBook("2").first())
    }
}
