package com.iridium.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Day-bucketed reading-time log backing true stats durations.
 *
 * Mori's stats bento reads `observeReadingStats` / `observeReadingSessions`
 * (one row per session with `durationMs` and `pagesTurned`). EPUBs have no
 * fixed pages, so Iridium adapts the semantics instead of cloning them:
 * - sessions collapse to per-day minute buckets (`minutesByDay`), which is
 *   all the history bento needs for the hero duration, the activity chart,
 *   and streaks;
 * - `durationMs` precision drops to whole minutes — a reading timer flushing
 *   once a minute is cheaper than per-second writes and honest at chart
 *   scale;
 * - the pages-turned analogue stays in [HistoryStatsSnapshot] (`segmentsRead`
 *   from fractional spine progress); this log never pretends to count pages.
 *
 * Durable implementation is a DataStore Preferences store keyed
 * [readingMinutesKey] (see [READING_MINUTES_KEY_PREFIX]); the binding below
 * is process-local until that impl lands.
 */
interface ReadingSessionStore {
    /** Minutes read per calendar day, keyed by local date. Empty when never read. */
    fun minutesByDay(): Flow<Map<LocalDate, Int>>

    /** Adds [minutes] to [date]'s bucket. Non-positive values are ignored. */
    suspend fun recordMinutes(date: LocalDate, minutes: Int)

    /** Lifetime total across all day buckets. Zero when never read. */
    fun totalMinutes(): Flow<Int>
}

/**
 * DataStore Preferences key prefix for the durable reading-time log. The
 * suffix is the ISO-8601 date (`LocalDate.toString`), so keys sort and read
 * as `reading_minutes_2026-10-07`. Canonical home of the format: both the
 * future DataStore impl and its key-parsing must round-trip through
 * [readingMinutesKey] / [readingMinutesDateOrNull].
 */
const val READING_MINUTES_KEY_PREFIX = "reading_minutes_"

/** Preferences key holding [date]'s minutes, e.g. `reading_minutes_2026-10-07`. */
fun readingMinutesKey(date: LocalDate): String = READING_MINUTES_KEY_PREFIX + date.toString()

/**
 * Inverse of [readingMinutesKey]: the date for a minutes key, or null when
 * [key] is a foreign preference or holds an unparsable date.
 */
fun readingMinutesDateOrNull(key: String): LocalDate? {
    if (!key.startsWith(READING_MINUTES_KEY_PREFIX)) return null
    return runCatching { LocalDate.parse(key.removePrefix(READING_MINUTES_KEY_PREFIX)) }.getOrNull()
}

/**
 * Process-local [ReadingSessionStore]: same additive day-bucket semantics as
 * the durable DataStore impl, kept in a mutex-guarded [MutableStateFlow] so
 * concurrent timer flushes cannot lose minutes. State dies with the process;
 * the DataStore impl replaces this binding without touching callers.
 */
@Singleton
class InMemoryReadingSessionStore @Inject constructor() : ReadingSessionStore {

    private val mutex = Mutex()
    private val minutesByDay = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())

    override fun minutesByDay(): Flow<Map<LocalDate, Int>> = minutesByDay

    override suspend fun recordMinutes(date: LocalDate, minutes: Int) {
        if (minutes <= 0) return
        mutex.withLock {
            minutesByDay.update { current -> current + (date to ((current[date] ?: 0) + minutes)) }
        }
    }

    override fun totalMinutes(): Flow<Int> =
        minutesByDay.map { days -> days.values.sum() }.distinctUntilChanged()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ReadingSessionsModule {

    @Binds
    @Singleton
    abstract fun bindReadingSessionStore(
        impl: InMemoryReadingSessionStore,
    ): ReadingSessionStore
}
