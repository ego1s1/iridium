package com.iridium.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import com.iridium.core.fakes.TestBooksRepository
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
class DetailViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = TestBooksRepository()
    private lateinit var viewModel: DetailViewModel

    @Before
    fun setup() {
        viewModel = DetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf("bookId" to "book-1")),
            repository = repository,
        )
    }

    @Test
    fun `emits the book once the database has it`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "book-1", title = "Treasure Island")))
        val state = viewModel.uiState.first { it is DetailUiState.Success } as DetailUiState.Success
        assertEquals("book-1", state.book.id)
        assertEquals("Treasure Island", state.book.title)
    }

    @Test
    fun `reports Gone when the book is not in the library`() = runTest {
        repository.setBooks(emptyList())
        assertEquals(DetailUiState.Gone, viewModel.uiState.first())
    }

    @Test
    fun `toggle bookmark writes through and reflects the change`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "book-1")))
        viewModel.uiState.first { it is DetailUiState.Success }

        viewModel.onAction(DetailAction.ToggleBookmark(true))

        val state = viewModel.uiState.first {
            it is DetailUiState.Success && it.book.bookmarked
        } as DetailUiState.Success
        assertTrue(state.book.bookmarked)
    }

    @Test
    fun `removing asks for confirmation first`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "book-1")))
        viewModel.uiState.first { it is DetailUiState.Success }

        viewModel.onAction(DetailAction.AskRemove)
        assertTrue((viewModel.uiState.first() as DetailUiState.Success).confirmRemove)

        viewModel.onAction(DetailAction.DismissRemove)
        assertFalse((viewModel.uiState.first() as DetailUiState.Success).confirmRemove)
    }

    @Test
    fun `confirming removal unlinks the book`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "book-1")))
        viewModel.uiState.first { it is DetailUiState.Success }

        viewModel.onAction(DetailAction.ConfirmRemove)

        assertEquals(DetailUiState.Gone, viewModel.uiState.first())
    }

    @Test
    fun `deleting an annotation removes only that item`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "book-1")))
        repository.setHighlights(
            listOf(
                com.iridium.core.model.Highlight(
                    id = "h1", bookId = "book-1", href = "ch1.xhtml",
                    startLocator = "{}", endLocator = "{}", selectedText = "one",
                ),
                com.iridium.core.model.Highlight(
                    id = "h2", bookId = "book-1", href = "ch2.xhtml",
                    startLocator = "{}", endLocator = "{}", selectedText = "two",
                ),
            ),
        )
        viewModel.uiState.first { it is DetailUiState.Success && it.highlights.size == 2 }

        viewModel.onAction(DetailAction.DeleteHighlight("h1"))

        val state = viewModel.uiState.first {
            it is DetailUiState.Success && it.highlights.size == 1
        } as DetailUiState.Success
        assertEquals("h2", state.highlights.first().id)
    }
}
