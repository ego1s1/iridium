package com.iridium.feature.history.impl

import com.iridium.core.fakes.TestBooksRepository
import com.iridium.core.testing.TestData
import com.iridium.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = TestBooksRepository()
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setup() {
        viewModel = HistoryViewModel(repository)
    }

    @Test
    fun `untouched books are not history`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "1", progress = 0f)))
        assertTrue(viewModel.uiState.first().groups.isEmpty())
    }

    @Test
    fun `books read today are grouped under Today`() = runTest {
        val now = System.currentTimeMillis()
        repository.setBooks(
            listOf(
                TestData.book(id = "1", progress = 0.4f, updatedAt = now),
                TestData.book(id = "2", progress = 0.9f, updatedAt = now - 60_000),
            ),
        )
        val state = viewModel.uiState.first { it.groups.isNotEmpty() }
        assertEquals("Today", state.groups.first().label)
        assertEquals(listOf("1", "2"), state.groups.first().books.map { it.id })
    }

    @Test
    fun `books read yesterday are separated from today`() = runTest {
        val now = System.currentTimeMillis()
        val yesterday = now - 26L * 60 * 60 * 1000
        repository.setBooks(
            listOf(
                TestData.book(id = "today", progress = 0.5f, updatedAt = now),
                TestData.book(id = "yesterday", progress = 0.5f, updatedAt = yesterday),
            ),
        )
        val state = viewModel.uiState.first { it.groups.size == 2 }
        assertEquals(listOf("Today", "Yesterday"), state.groups.map { it.label })
        assertEquals(listOf("today"), state.groups[0].books.map { it.id })
        assertEquals(listOf("yesterday"), state.groups[1].books.map { it.id })
    }

    @Test
    fun `finished books still count as history`() = runTest {
        repository.setBooks(
            listOf(TestData.book(id = "1", progress = 1f, updatedAt = System.currentTimeMillis())),
        )
        val state = viewModel.uiState.first { it.groups.isNotEmpty() }
        assertEquals(listOf("1"), state.groups.first().books.map { it.id })
    }
}
