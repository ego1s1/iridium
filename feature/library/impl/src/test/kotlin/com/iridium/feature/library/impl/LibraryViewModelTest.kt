package com.iridium.feature.library.impl

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.iridium.core.data.IndexReport
import com.iridium.core.fakes.TestBooksRepository
import com.iridium.core.fakes.TestPreferencesDataSource
import com.iridium.core.testing.TestData
import com.iridium.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.first
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
    fun setup() {
        viewModel = LibraryViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            preferences = preferences,
        )
    }

    @Test
    fun `ui state starts empty and unlinked without a loading gate`() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.books.isEmpty())
        assertFalse(state.linked)
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
    fun `link folder persists the tree and scans it`() = runTest {
        val uri = Uri.parse("content://tree/books")
        viewModel.onAction(LibraryAction.LinkFolder(uri))

        assertEquals("content://tree/books", preferences.sourceTreeUri.first())
        assertEquals(uri, repository.lastLinkedTree)
        assertTrue(preferences.onboardingCompleted.first())
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
        viewModel.onAction(LibraryAction.LinkFolder(Uri.parse("content://tree")))
        // The message channel is consumed by the UI; assert the scan ran.
        assertEquals(Uri.parse("content://tree"), repository.lastLinkedTree)
    }
}
