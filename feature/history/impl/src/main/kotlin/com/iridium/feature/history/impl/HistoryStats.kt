package com.iridium.feature.history.impl

import com.iridium.core.model.Book
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Reading-stats shaping over Iridium history data, mirroring Mori's Stats
 * bento (UI hierarchy §3.4) without Mori's stats tables.
 *
 * Iridium has no session log or page-turn counter — only [Book.progress]
 * (0f..1f), [Book.updatedAt], and [Book.spineCount]. Every metric below is
 * derived from those three fields, so the bento stays honest about what it
 * measures:
 * - hero reading time is *estimated* from fractional progress
 *   ([ESTIMATED_MINUTES_PER_BOOK] per complete read);
 * - "segments read" (the pages-turned analogue for reflowable EPUBs, which
 *   have no fixed pages) counts fractional spine progress, falling back to
 *   [FALLBACK_SEGMENTS_PER_BOOK] when a book reports no spine count;
 * - the activity chart buckets distinct read-days, not minutes.
 *
 * All shaping is pure and unit-tested; the composable stays declarative.
 */

/** Chart window, mirroring Mori's StatsRange (week / month / year). */
enum class HistoryStatsRange {
    WEEK,
    MONTH,
    YEAR,
}

/** Window length in days for a chart range. */
fun HistoryStatsRange.days(): Int = when (this) {
    HistoryStatsRange.WEEK -> 7
    HistoryStatsRange.MONTH -> 30
    HistoryStatsRange.YEAR -> 365
}

/** One chart bucket: read-day activity, oldest first on the chart axis. */
data class HistoryDayBucket(
    val dayStartMillis: Long,
    val booksActive: Int,
    val booksFinished: Int,
)

/** Bento totals derived from fractional EPUB progress. */
data class HistoryTotals(
    /** Books with any progress. */
    val booksStarted: Int,
    /** Books at 100% progress. */
    val booksFinished: Int,
    /** Estimated spine segments read (pages-turned analogue). */
    val segmentsRead: Int,
    /** Estimated reading time in milliseconds (hero card). */
    val estimatedReadingMs: Long,
)

/** Consecutive-day reading streak, in local days. */
data class HistoryStreak(
    val current: Int,
    val longest: Int,
)

/** One book's rank in the top-books list. */
data class HistoryTopBook(
    val book: Book,
    /** Whole-percent progress, for the row subtitle. */
    val percentRead: Int,
)

/** Full snapshot backing the bento grid. */
data class HistoryStatsSnapshot(
    val totals: HistoryTotals,
    /** Buckets for [range], oldest first. */
    val buckets: List<HistoryDayBucket>,
    val range: HistoryStatsRange,
    val streak: HistoryStreak,
    val topBooks: List<HistoryTopBook>,
)

/**
 * Builds the bento snapshot for [range] from history books. One clock read:
 * buckets and streak must agree even if midnight falls between two calls.
 */
fun List<Book>.toHistoryStats(
    range: HistoryStatsRange,
    nowMillis: Long = System.currentTimeMillis(),
): HistoryStatsSnapshot {
    val read = filter { it.progress > 0f }
    return HistoryStatsSnapshot(
        totals = toHistoryTotals(),
        buckets = read.historyDayBuckets(nowMillis, range.days()),
        range = range,
        streak = historyStreak(read, nowMillis),
        topBooks = historyTopBooks(read),
    )
}

/** Totals shaping: counts plus progress-derived estimates. */
fun List<Book>.toHistoryTotals(): HistoryTotals {
    val read = filter { it.progress > 0f }
    val progressSum = read.sumOf { it.progress.coerceIn(0f, 1f).toDouble() }
    return HistoryTotals(
        booksStarted = read.size,
        booksFinished = read.count { it.progress >= 1f },
        segmentsRead = read.sumOf { book ->
            val segments = if (book.spineCount > 0) book.spineCount else FALLBACK_SEGMENTS_PER_BOOK
            (book.progress.coerceIn(0f, 1f) * segments).roundToInt()
        },
        estimatedReadingMs = (progressSum * ESTIMATED_MINUTES_PER_BOOK * 60_000L).toLong(),
    )
}

/**
 * Per-day activity buckets for the last [days] days, oldest first.
 * A book lands in the bucket of its [Book.updatedAt] day; untouched books
 * never reach here (callers filter on progress first).
 */
fun List<Book>.historyDayBuckets(nowMillis: Long, days: Int): List<HistoryDayBucket> {
    val today = dayStartMillis(nowMillis)
    val starts = (days - 1 downTo 0).map { today - it * DAY_MS }
    val counts = groupingByDate()
    return starts.map { start ->
        val books = counts[start].orEmpty()
        HistoryDayBucket(
            dayStartMillis = start,
            booksActive = books.size,
            booksFinished = books.count { it.progress >= 1f },
        )
    }
}

/** Groups read books by their local read-day start. */
private fun List<Book>.groupingByDate(): Map<Long, List<Book>> =
    groupBy { dayStartMillis(it.updatedAt) }

/**
 * Current and longest consecutive-day streaks from the set of days with at
 * least one read book. The current streak counts back from today (or
 * yesterday, so a streak isn't shown broken until a full day is missed).
 */
fun historyStreak(books: List<Book>, nowMillis: Long): HistoryStreak {
    if (books.isEmpty()) return HistoryStreak(current = 0, longest = 0)
    val days = books.map { dayStartMillis(it.updatedAt) }.toSortedSet()
    var longest = 0
    var run = 0
    var previous: Long? = null
    for (day in days) {
        // Calendar-day succession, not 24h arithmetic: across a DST
        // transition consecutive midnights are 23 or 25h apart. Midnight +
        // 36h always lands inside the next calendar day, so its day-start
        // is the successor test.
        run = if (previous != null && day == dayStartMillis(previous + NEXT_DAY_OFFSET_MS)) {
            run + 1
        } else {
            1
        }
        longest = maxOf(longest, run)
        previous = day
    }
    val today = dayStartMillis(nowMillis)
    var current = 0
    // Stepping back uses −12h, not −36h: the previous midnight is 23–25h
    // back, so −12h lands inside it, while −36h would overshoot on a 23h
    // spring-forward day.
    var cursor = if (days.contains(today)) today else dayStartMillis(today - PREV_DAY_OFFSET_MS)
    while (days.contains(cursor)) {
        current++
        cursor = dayStartMillis(cursor - PREV_DAY_OFFSET_MS)
    }
    return HistoryStreak(current = current, longest = longest)
}

/**
 * Books ranked by progress, then recency — the most-read surface. Untouched
 * books never rank; errored rows stay visible so the user can retry/remove.
 */
fun historyTopBooks(books: List<Book>, limit: Int = TOP_BOOK_LIMIT): List<HistoryTopBook> {
    if (books.isEmpty()) return emptyList()
    return books
        .filter { it.progress > 0f }
        .sortedWith(compareByDescending<Book> { it.progress }.thenByDescending { it.updatedAt })
        .take(limit)
        .map { HistoryTopBook(book = it, percentRead = (it.progress.coerceIn(0f, 1f) * 100).roundToInt()) }
}

/** Local-midnight start of the day containing [timeMillis]. */
fun dayStartMillis(timeMillis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timeMillis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val NEXT_DAY_OFFSET_MS = 36L * 60L * 60L * 1000L
private const val PREV_DAY_OFFSET_MS = 12L * 60L * 60L * 1000L
private const val TOP_BOOK_LIMIT = 5

/** Assumed full-book read time behind the hero estimate (a ~6h novel). */
internal const val ESTIMATED_MINUTES_PER_BOOK = 360

/** Spine fallback when a book reports no spine count (a ~300-page novel). */
internal const val FALLBACK_SEGMENTS_PER_BOOK = 300
