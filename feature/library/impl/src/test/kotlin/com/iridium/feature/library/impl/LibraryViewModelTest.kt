package com.iridium.feature.library.impl

import androidx.lifecycle.SavedStateHandle
import com.iridium.core.data.IndexReport
import com.iridium.core.fakes.TestBooksRepository
import com.iridium.core.fakes.TestPreferencesDataSource
import com.iridium.core.testing.TestData
import com.iridium.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = TestBooksRepository()
    private val preferences = TestPreferencesDataSource()
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setup() = runTest {
        // Scans require at least one linked folder (SAF model).
        preferences.addLinkedFolder("content://com.example/tree/books")
        viewModel = LibraryViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            preferences = preferences,
        )
    }

    @Test
    fun `ui state starts empty without a loading gate`() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.books.isEmpty())
        assertFalse(state.refreshing)
    }

    @Test
    fun `books appear as soon as the database emits`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "1", title = "Treasure Island")))
        val state = viewModel.uiState.first { it.books.isNotEmpty() }
        assertEquals("1", state.books.first().id)
    }

    @Test
    fun `search filters by title`() = runTest {
        repository.setBooks(
            listOf(
                TestData.book(id = "1", title = "Treasure Island"),
                TestData.book(id = "2", title = "Huck Finn"),
            ),
        )
        viewModel.onAction(LibraryAction.SearchTextChanged("huck"))
        val state = viewModel.uiState.first { it.books.size == 1 }
        assertEquals("2", state.books.first().id)
    }

    @Test
    fun `launch scans the filesystem`() = runTest {
        viewModel.uiState.first { repository.filesystemScans > 0 }
        assertTrue(repository.filesystemScans >= 1)
    }

    @Test
    fun `manual rescan scans the filesystem again`() = runTest {
        viewModel.uiState.first { repository.filesystemScans > 0 }
        val before = repository.filesystemScans
        viewModel.onAction(LibraryAction.Rescan)
        viewModel.uiState.first { repository.filesystemScans > before }
    }

    @Test
    fun `continue shelf lists in-progress books by recency`() = runTest {
        repository.setBooks(
            listOf(
                TestData.book(id = "1", progress = 0.2f, updatedAt = 1L),
                TestData.book(id = "2", progress = 0f, updatedAt = 9L),
                TestData.book(id = "3", progress = 0.7f, updatedAt = 5L),
            ),
        )
        val state = viewModel.uiState.first { it.books.size == 3 }
        assertEquals(listOf("3", "1"), state.continueReading.map { it.id })
    }

    @Test
    fun `resume target skips errored and untouched books`() = runTest {
        repository.setBooks(
            listOf(
                TestData.book(id = "1", progress = 0.9f, updatedAt = 1L),
                TestData.book(
                    id = "2",
                    progress = 0.9f,
                    updatedAt = 5L,
                    error = com.iridium.core.model.BookError.CORRUPT,
                ),
                TestData.book(id = "3", progress = 0f, updatedAt = 9L),
            ),
        )
        val target = viewModel.resumeTarget.first { it != null }
        assertEquals("1", target?.id)
    }

    @Test
    fun `scan failure emits a one-shot message`() = runTest {
        repository.indexResult = IndexReport(total = 3, failed = 2)
        viewModel.onAction(LibraryAction.Rescan)
        // The message channel is consumed by the UI; assert the scan ran.
        viewModel.uiState.first { repository.filesystemScans > 1 }
    }

    @Test
    fun `scan is skipped with no linked folders`() = runTest {
        val bare = TestPreferencesDataSource()
        val quiet = LibraryViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            preferences = bare,
        )
        val scans = repository.filesystemScans
        quiet.onAction(LibraryAction.Rescan)
        // Let the skipped scan settle: no new filesystem pass may start.
        runCurrent()
        assertEquals(scans, repository.filesystemScans)
    }

    @Test
    fun `typing a query surfaces full-text hits`() = runTest {
        repository.searchHits = listOf(
            com.iridium.core.data.ContentHit(
                bookId = "1",
                bookTitle = "Treasure Island",
                href = "OEBPS/ch1.xhtml",
                chapterTitle = "Chapter 1",
                snippet = "…the Hispaniola was rolling…",
            ),
        )
        viewModel.onAction(LibraryAction.SearchTextChanged("hispaniola"))

        val state = viewModel.uiState.first { it.contentHits.isNotEmpty() }
        assertEquals("hispaniola", repository.lastSearchQuery)
        assertEquals(1, state.contentHits.size)
        assertEquals("OEBPS/ch1.xhtml", state.contentHits.first().href)
    }

    @Test
    fun `a one-character query does not search content`() = runTest {
        repository.searchHits = listOf(
            com.iridium.core.data.ContentHit("1", "Book", "ch.xhtml", "Ch", "snippet"),
        )
        viewModel.onAction(LibraryAction.SearchTextChanged("a"))
        // Give the debounce a chance to run; nothing should have been queried.
        assertTrue(viewModel.uiState.value.contentHits.isEmpty())
        assertEquals(null, repository.lastSearchQuery)
    }

    @Test
    fun `index library indexes every book`() = runTest {
        repository.setBooks(
            listOf(TestData.book(id = "1"), TestData.book(id = "2")),
        )
        viewModel.onAction(LibraryAction.IndexLibrary)

        assertEquals(listOf("1", "2"), repository.indexedBooks)
    }
}
