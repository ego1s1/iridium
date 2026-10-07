package com.iridium.core.data

import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * In-memory day-bucket log: no DataStore on this module's classpath, so the
 * fake IS the contract under test — additive buckets, day isolation, empty
 * defaults, and the stable `reading_minutes_<ISO-date>` key format the
 * durable impl must reuse. No mocking libraries; plain JUnit4 + runTest like
 * [FtsQueryTest].
 */
class ReadingSessionsTest {

    private val store = InMemoryReadingSessionStore()

    @Test
    fun `empty store emits no days and zero total`() = runTest {
        assertTrue(store.minutesByDay().first().isEmpty())
        assertEquals(0, store.totalMinutes().first())
    }

    @Test
    fun `recorded minutes round-trip per day`() = runTest {
        val date = LocalDate.of(2026, 10, 7)

        store.recordMinutes(date, 12)

        assertEquals(mapOf(date to 12), store.minutesByDay().first())
        assertEquals(12, store.totalMinutes().first())
    }

    @Test
    fun `same-day records accumulate`() = runTest {
        val date = LocalDate.of(2026, 10, 7)

        store.recordMinutes(date, 3)
        store.recordMinutes(date, 2)

        assertEquals(5, store.minutesByDay().first().getValue(date))
        assertEquals(5, store.totalMinutes().first())
    }

    @Test
    fun `minutes bucket by day and total sums across days`() = runTest {
        store.recordMinutes(LocalDate.of(2026, 10, 5), 10)
        store.recordMinutes(LocalDate.of(2026, 10, 6), 20)
        store.recordMinutes(LocalDate.of(2026, 10, 7), 5)

        val byDay = store.minutesByDay().first()
        assertEquals(3, byDay.size)
        assertEquals(10, byDay.getValue(LocalDate.of(2026, 10, 5)))
        assertEquals(20, byDay.getValue(LocalDate.of(2026, 10, 6)))
        assertEquals(5, byDay.getValue(LocalDate.of(2026, 10, 7)))
        assertEquals(35, store.totalMinutes().first())
    }

    @Test
    fun `non-positive minutes are ignored`() = runTest {
        val date = LocalDate.of(2026, 10, 7)

        store.recordMinutes(date, 0)
        store.recordMinutes(date, -4)

        assertTrue(store.minutesByDay().first().isEmpty())
        assertEquals(0, store.totalMinutes().first())
    }

    @Test
    fun `key format is a stable ISO date`() {
        assertEquals("reading_minutes_2026-10-07", readingMinutesKey(LocalDate.of(2026, 10, 7)))
    }

    @Test
    fun `key parsing round-trips and rejects foreign keys`() {
        val date = LocalDate.of(2026, 10, 7)

        assertEquals(date, readingMinutesDateOrNull(readingMinutesKey(date)))
        assertNull(readingMinutesDateOrNull("onboarding_completed"))
        assertNull(readingMinutesDateOrNull("reading_minutes_not-a-date"))
    }
}
