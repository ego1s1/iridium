package com.iridium.feature.history.impl

import com.iridium.core.model.BookError
import com.iridium.core.testing.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Second-wave history-to-stats parity: bento shaping edge cases the first
 * suite leaves out — clamping, spine fallback math, ranking limits, errored
 * rows, bucket ordering, streak grace rules, and out-of-window reads.
 * Pure shaping only, so plain JUnit (no dispatchers, no Robolectric).
 */
class HistoryStatsParityTest {

    private val dayMs = 86_400_000L

    @Test
    fun `empty library yields a zero snapshot with full window buckets`() {
        HistoryStatsRange.entries.forEach { range ->
            val snapshot = emptyList<com.iridium.core.model.Book>().toHistoryStats(range)
            assertEquals("range $range", range, snapshot.range)
            assertEquals("buckets $range", range.days(), snapshot.buckets.size)
            assertEquals(0, snapshot.totals.booksStarted)
            assertEquals(0, snapshot.totals.booksFinished)
            assertEquals(0, snapshot.totals.segmentsRead)
            assertEquals(0L, snapshot.totals.estimatedReadingMs)
            assertEquals(HistoryStreak(0, 0), snapshot.streak)
            assertTrue("topBooks $range", snapshot.topBooks.isEmpty())
            assertTrue(
                "all idle $range",
                snapshot.buckets.all { it.booksActive == 0 && it.booksFinished == 0 },
            )
        }
    }

    @Test
    fun `untouched books stay out of every metric`() {
        val books = listOf(
            TestData.book(id = "a", progress = 0f, updatedAt = System.currentTimeMillis()),
        )
        val snapshot = books.toHistoryStats(HistoryStatsRange.WEEK)
        assertEquals(0, snapshot.totals.booksStarted)
        assertEquals(0, snapshot.buckets.sumOf { it.booksActive })
        assertEquals(HistoryStreak(0, 0), historyStreak(books.filter { it.progress > 0f }, System.currentTimeMillis()))
        assertTrue(historyTopBooks(books).isEmpty())
    }

    @Test
    fun `negative progress is treated as untouched`() {
        val books = listOf(TestData.book(id = "neg", progress = -0.5f))
        val totals = books.toHistoryTotals()
        assertEquals(0, totals.booksStarted)
        assertEquals(0, totals.booksFinished)
        assertEquals(0, totals.segmentsRead)
        assertEquals(0L, totals.estimatedReadingMs)
        assertTrue(historyTopBooks(books).isEmpty())
    }

    @Test
    fun `over-full progress clamps estimates to one complete read`() {
        val books = listOf(TestData.book(id = "over", progress = 1.5f))
        val totals = books.toHistoryTotals()
        assertEquals(1, totals.booksStarted)
        assertEquals(1, totals.booksFinished)
        // Clamped to 1f: one fallback book of 300 segments.
        assertEquals(300, totals.segmentsRead)
        val top = historyTopBooks(books)
        assertEquals(1, top.size)
        assertEquals(100, top.first().percentRead)
    }

    @Test
    fun `segments use spine count when present and fallback otherwise`() {
        val books = listOf(
            TestData.book(id = "spine", progress = 0.5f).copy(spineCount = 200),
            TestData.book(id = "fallback", progress = 0.5f).copy(spineCount = 0),
        )
        val totals = books.toHistoryTotals()
        assertEquals(100 + 150, totals.segmentsRead)
    }

    @Test
    fun `estimated reading time scales linearly with progress sum`() {
        val quarter = listOf(TestData.book(id = "q", progress = 0.25f)).toHistoryTotals()
        // A quarter read at 360 min per book = 90 min.
        assertEquals(90L * 60_000L, quarter.estimatedReadingMs)
        val empty = emptyList<com.iridium.core.model.Book>().toHistoryTotals()
        assertEquals(0L, empty.estimatedReadingMs)
    }

    @Test
    fun `top books cap at five and exclude untouched rows`() {
        val now = System.currentTimeMillis()
        val books = (1..7).map { i ->
            TestData.book(id = "b$i", progress = 0.1f * i, updatedAt = now - i * 1_000L)
        } + TestData.book(id = "untouched", progress = 0f, updatedAt = now)
        val top = historyTopBooks(books)
        assertEquals(5, top.size)
        assertEquals(listOf("b7", "b6", "b5", "b4", "b3"), top.map { it.book.id })
        assertTrue(top.none { it.book.id == "untouched" })
    }

    @Test
    fun `errored rows with progress stay visible in top books`() {
        val books = listOf(
            TestData.book(id = "broken", progress = 0.7f, error = BookError.CORRUPT),
        )
        val top = historyTopBooks(books)
        assertEquals(1, top.size)
        assertEquals("broken", top.first().book.id)
        assertEquals(70, top.first().percentRead)
    }

    @Test
    fun `percent read rounds fractional progress to whole percent`() {
        val books = listOf(TestData.book(id = "third", progress = 1f / 3f))
        assertEquals(33, historyTopBooks(books).first().percentRead)
    }

    @Test
    fun `buckets run oldest first and count finished per read day`() {
        val now = System.currentTimeMillis()
        val books = listOf(
            TestData.book(id = "done", progress = 1f, updatedAt = now),
            TestData.book(id = "half", progress = 0.5f, updatedAt = now),
        )
        val buckets = books.historyDayBuckets(now, HistoryStatsRange.WEEK.days())
        assertEquals(7, buckets.size)
        buckets.zipWithNext { a, b ->
            assertTrue("oldest first", a.dayStartMillis < b.dayStartMillis)
        }
        val today = buckets.last()
        assertEquals(2, today.booksActive)
        assertEquals(1, today.booksFinished)
    }

    @Test
    fun `same-day reads share one bucket`() {
        val now = System.currentTimeMillis()
        val books = listOf(
            TestData.book(id = "a", progress = 0.3f, updatedAt = now),
            TestData.book(id = "b", progress = 0.6f, updatedAt = now - 60_000L),
        )
        val active = books.toHistoryStats(HistoryStatsRange.WEEK).buckets
            .filter { it.booksActive > 0 }
        assertEquals(1, active.size)
        assertEquals(2, active.first().booksActive)
    }

    @Test
    fun `stale reads count in totals but leave the week chart idle`() {
        val now = System.currentTimeMillis()
        val today = dayStartMillis(now)
        val books = listOf(
            TestData.book(id = "old", progress = 0.4f, updatedAt = today - 100 * dayMs),
        )
        val snapshot = books.toHistoryStats(HistoryStatsRange.WEEK, now)
        assertEquals(1, snapshot.totals.booksStarted)
        assertTrue(snapshot.buckets.all { it.booksActive == 0 })
    }

    @Test
    fun `single today read is a streak of one`() {
        val now = System.currentTimeMillis()
        val books = listOf(TestData.book(id = "a", progress = 0.3f, updatedAt = now))
        assertEquals(HistoryStreak(current = 1, longest = 1), historyStreak(books, now))
    }

    @Test
    fun `yesterday-only read keeps a one-day current streak`() {
        val now = System.currentTimeMillis()
        val today = dayStartMillis(now)
        val books = listOf(
            TestData.book(id = "a", progress = 0.3f, updatedAt = today - 12 * 60 * 60 * 1000),
        )
        assertEquals(HistoryStreak(current = 1, longest = 1), historyStreak(books, now))
    }

    @Test
    fun `two separate two-day runs keep longest two with current two`() {
        val now = System.currentTimeMillis()
        val today = dayStartMillis(now)
        fun day(offsetDays: Long) = TestData.book(
            id = "d$offsetDays",
            progress = 0.2f,
            updatedAt = today - offsetDays * dayMs + 3_600_000L,
        )
        val books = listOf(day(10), day(9), day(2), day(1))
        assertEquals(HistoryStreak(current = 2, longest = 2), historyStreak(books, now))
    }

    @Test
    fun `range lengths match the mori week month year windows`() {
        assertEquals(7, HistoryStatsRange.WEEK.days())
        assertEquals(30, HistoryStatsRange.MONTH.days())
        assertEquals(365, HistoryStatsRange.YEAR.days())
    }

    @Test
    fun `day start is idempotent and never after its input`() {
        val now = System.currentTimeMillis()
        val start = dayStartMillis(now)
        assertTrue(start <= now)
        assertEquals(start, dayStartMillis(start))
        assertEquals(start, dayStartMillis(start + 3_600_000L))
    }
}
