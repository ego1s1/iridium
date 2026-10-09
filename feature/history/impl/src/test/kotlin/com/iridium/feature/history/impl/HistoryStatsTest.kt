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

class HistoryStatsTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = TestBooksRepository()
    private lateinit var viewModel: HistoryStatsViewModel

    @Before
    fun setup() {
        viewModel = HistoryStatsViewModel(repository)
    }

    @Test
    fun `untouched books stay out of stats`() {
        val snapshot = listOf(TestData.book(id = "1", progress = 0f))
            .toHistoryStats(HistoryStatsRange.WEEK)
        assertEquals(0, snapshot.totals.booksStarted)
        assertEquals(0, snapshot.totals.booksFinished)
        assertEquals(HistoryStreak(0, 0), snapshot.streak)
        assertTrue(snapshot.topBooks.isEmpty())
    }

    @Test
    fun `totals count started finished and estimates`() {
        val books = listOf(
            TestData.book(id = "half", progress = 0.5f),
            TestData.book(id = "done", progress = 1f),
        )
        val totals = books.toHistoryTotals()
        assertEquals(2, totals.booksStarted)
        assertEquals(1, totals.booksFinished)
        // No spine counts in fixtures: fallback segments (300 per book).
        assertEquals(150 + 300, totals.segmentsRead)
        // 1.5 complete reads at 360 min each.
        assertEquals(1.5 * 360 * 60_000L, totals.estimatedReadingMs.toDouble(), 1.0)
    }

    @Test
    fun `buckets match the selected range length`() {
        val books = listOf(TestData.book(id = "1", progress = 0.4f))
        assertEquals(7, books.toHistoryStats(HistoryStatsRange.WEEK).buckets.size)
        assertEquals(30, books.toHistoryStats(HistoryStatsRange.MONTH).buckets.size)
        assertEquals(365, books.toHistoryStats(HistoryStatsRange.YEAR).buckets.size)
    }

    @Test
    fun `books land in their read-day bucket`() = runTest {
        val now = System.currentTimeMillis()
        // One hour before today's midnight is always calendar-yesterday,
        // unlike now-26h which crosses two midnights near 00:xx.
        val yesterday = dayStartMillis(now) - 60L * 60 * 1000
        repository.setBooks(
            listOf(
                TestData.book(id = "today", progress = 0.5f, updatedAt = now),
                TestData.book(id = "yesterday", progress = 0.5f, updatedAt = yesterday),
            ),
        )
        val snapshot = viewModel.uiState.first { it.totals.booksStarted == 2 }
        val active = snapshot.buckets.filter { it.booksActive > 0 }
        assertEquals(2, active.size)
        assertEquals(2, snapshot.buckets.sumOf { it.booksActive })
    }

    @Test
    fun `consecutive read days form a streak`() {
        val now = System.currentTimeMillis()
        val yesterday = dayStartMillis(now) - 60L * 60 * 1000
        val books = listOf(
            TestData.book(id = "a", progress = 0.5f, updatedAt = yesterday),
            TestData.book(id = "b", progress = 0.5f, updatedAt = now),
        )
        assertEquals(HistoryStreak(current = 2, longest = 2), historyStreak(books, now))
    }

    @Test
    fun `streak keeps longest across a gap`() {
        val today = dayStartMillis(System.currentTimeMillis())
        val day = 86_400_000L
        val books = listOf(
            TestData.book(id = "a", progress = 0.2f, updatedAt = today - 5 * day),
            TestData.book(id = "b", progress = 0.2f, updatedAt = today - 4 * day),
            TestData.book(id = "c", progress = 0.2f, updatedAt = today - 3 * day),
            TestData.book(id = "d", progress = 0.2f, updatedAt = today),
        )
        assertEquals(HistoryStreak(current = 1, longest = 3), historyStreak(books, today + 1_000L))
    }

    @Test
    fun `top books rank by progress then recency`() {
        val now = System.currentTimeMillis()
        val books = listOf(
            TestData.book(id = "older", progress = 0.9f, updatedAt = now - 60_000),
            TestData.book(id = "newer", progress = 0.9f, updatedAt = now),
            TestData.book(id = "half", progress = 0.4f, updatedAt = now),
        )
        val top = historyTopBooks(books)
        assertEquals(listOf("newer", "older", "half"), top.map { it.book.id })
        assertEquals(90, top.first().percentRead)
    }

    @Test
    fun `viewmodel emits snapshot and resizes buckets on range select`() = runTest {
        repository.setBooks(
            listOf(TestData.book(id = "1", progress = 0.4f, updatedAt = System.currentTimeMillis())),
        )
        val week = viewModel.uiState.first { it.totals.booksStarted == 1 }
        assertEquals(HistoryStatsRange.WEEK, week.range)
        assertEquals(7, week.buckets.size)

        viewModel.onRangeSelect(HistoryStatsRange.MONTH)
        val month = viewModel.uiState.first { it.range == HistoryStatsRange.MONTH }
        assertEquals(30, month.buckets.size)
        assertEquals(1, month.buckets.sumOf { it.booksActive })

        viewModel.onRangeSelect(HistoryStatsRange.YEAR)
        val year = viewModel.uiState.first { it.range == HistoryStatsRange.YEAR }
        assertEquals(365, year.buckets.size)
    }
}
