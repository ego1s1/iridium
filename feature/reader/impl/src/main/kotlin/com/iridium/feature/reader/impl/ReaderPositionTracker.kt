package com.iridium.feature.reader.impl

import com.iridium.core.common.AppClock
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.shared.publication.Locator

/**
 * Page counting for the reader.
 *
 * The numbers are real rendered pages, not a scaled position table:
 *
 * - The navigator reports `pageIndex` / `totalPages` for the chapter it has
 *   laid out (`onPageChanged`). That is exact, free and arrives on every page
 *   change, so it is the source of truth for the current chapter.
 * - Counts are cached per chapter and restored per typography fingerprint, so
 *   the book-wide total is stable across sessions instead of being re-derived
 *   from whichever chapter the book happened to open on.
 * - Chapters that have never been laid out are estimated from the observed
 *   pages-per-position ratio. As the reader moves through the book the
 *   estimate is replaced by exact counts.
 *
 * The position table is still what the scrubber seeks with; it is no longer
 * used to invent a page number.
 */
internal class ReaderPositionTracker(
    private val store: ReaderSessionStore,
    private val clock: AppClock,
) {

    /** Last programmatic jump (open/seek/TOC): locator ticks inside the grace window are its own echo. */
    private var lastProgrammaticNavMs: Long = 0L

    /** Plain vars, not flows: every read and write happens on the Main thread
     * (viewModelScope + combine); [positionsVersion] drives recomposition. */
    private var positionsCache: List<Locator> = emptyList()
    private var chapterOrder: List<String> = emptyList()
    private var positionsPerChapter: Map<String, Int> = emptyMap()
    private val _positionsVersion = MutableStateFlow(0)
    val positionsVersion: StateFlow<Int> = _positionsVersion.asStateFlow()

    /** Typography fingerprint the learned counts belong to. */
    private var layoutKey: String = ""

    /** Rendered pages per chapter, as reported by the navigator. */
    private val chapterPages = mutableMapOf<String, Int>()

    /** Chapter and page the navigator last reported (exact, not estimated). */
    private var currentHref: String? = null
    private var currentPageIndex: Int = 0

    /** Derived per chapter and cumulative totals; rebuilt when inputs change. */
    private var pagesByChapter: Map<String, Int> = emptyMap()
    private var pagesBefore: Map<String, Int> = emptyMap()
    private var totalPages: Int = 0

    /**
     * Optimistic index for the page a tap just requested. Same unit as the
     * real index, so it is replaced by the navigator's own number without any
     * reconciliation heuristic: no drift and no snap-back.
     */
    private var pendingIndex: Int? = null

    /** Sticky chapter title: locators between TOC anchors keep the last one. */
    private var lastChapterTitle: String? = null

    val hasChapterPages: Boolean get() = chapterPages.isNotEmpty()

    val currentLayoutKey: String get() = layoutKey

    fun snapshotChapterPages(): Map<String, Int> = chapterPages.toMap()

    fun replaceAll(positions: List<Locator>) {
        positionsCache = positions
        chapterOrder = positions.map { it.href.toString() }.distinct()
        positionsPerChapter = chapterOrder.associateWith { href ->
            positions.count { it.href.toString() == href }
        }
        recompute()
        _positionsVersion.value += 1
    }

    /**
     * A different typography is a different pagination: drop the learned
     * counts and start from the layout cache for that fingerprint.
     */
    fun applyLayout(key: String) {
        if (key == layoutKey) return
        layoutKey = key
        chapterPages.clear()
        currentHref = null
        currentPageIndex = 0
        pendingIndex = null
        lastChapterTitle = null
        recompute()
        _positionsVersion.value += 1
    }

    /** Restores counts persisted for [key] on an earlier session. */
    fun restorePersisted(key: String, pages: Map<String, Int>) {
        if (key != layoutKey || pages.isEmpty()) return
        var changed = false
        pages.forEach { (href, count) ->
            if (count > 0 && chapterPages[href] != count) {
                chapterPages[href] = count
                changed = true
            }
        }
        if (changed) {
            recompute()
            _positionsVersion.value += 1
        }
    }

    /** Exact chapter pagination, straight from the navigator. */
    fun onPageChanged(href: String, pageIndex: Int, totalPages: Int) {
        val pagesChanged = totalPages > 0 && chapterPages[href] != totalPages
        val moveChanged = href != currentHref || pageIndex != currentPageIndex
        val hadPending = pendingIndex != null
        if (pagesChanged) chapterPages[href] = totalPages
        currentHref = href
        currentPageIndex = pageIndex.coerceAtLeast(0)
        // The navigator's answer supersedes any optimistic tap.
        pendingIndex = null
        if (pagesChanged) recompute()
        if (pagesChanged || moveChanged || hadPending) _positionsVersion.value += 1
    }

    /** True once a rendered measurement exists (the counter is meaningful). */
    val hasPagination: Boolean get() = totalPages > 0

    /**
     * Current chapter title for [toc], resolved from the live locator. Sticky
     * between anchors so the label never blanks out mid-chapter.
     */
    fun chapterTitle(toc: List<com.iridium.core.model.TocEntry>): String? {
        val href = store.latestLocator.value?.href?.toString()
        if (href != null) {
            val file = href.substringBefore('#').substringAfterLast('/')
            // Last match wins: subsections in the same file resolve deep.
            val match = toc.lastOrNull {
                it.href.substringBefore('#').substringAfterLast('/') == file
            }
            if (match != null && match.title.isNotBlank()) lastChapterTitle = match.title
        }
        return lastChapterTitle
    }

    fun markProgrammaticNav() {
        lastProgrammaticNavMs = clock.uptimeNow()
    }

    fun withinNavGrace(windowMs: Long): Boolean =
        clock.uptimeNow() - lastProgrammaticNavMs <= windowMs

    /**
     * 0-based page index and book-wide page total for the counter.
     *
     * Both are rendered pages. The index is the exact page within the current
     * chapter plus the pages of every chapter before it; a tap publishes the
     * next index immediately and the navigator's own value replaces it.
     */
    fun indexAndCount(): Pair<Int, Int> {
        if (totalPages <= 0) return 0 to 0
        val locator = store.latestLocator.value
        // The locator is authoritative for where the user is; the navigator's
        // own report covers the instant before the first locator tick.
        val href = locator?.href?.toString() ?: currentHref
        val absolute = if (href != null && pagesByChapter.containsKey(href)) {
            val before = pagesBefore[href] ?: 0
            val within = pagesWithin(href, pagesByChapter.getValue(href), locator)
            (before + within).coerceIn(0, totalPages - 1)
        } else {
            0
        }
        return (pendingIndex ?: absolute).coerceIn(0, totalPages - 1) to totalPages
    }

    /**
     * Registers a tap that will turn the page. The counter advances now, one
     * step from whatever is currently displayed, so repeated taps accumulate
     * predictably instead of compounding with a late echo.
     */
    fun onTurnRequested(direction: Int) {
        if (totalPages <= 0) return
        val (rawIndex, _) = indexAndCount()
        val base = pendingIndex ?: rawIndex
        pendingIndex = (base + direction).coerceIn(0, totalPages - 1)
        _positionsVersion.value += 1
    }

    /** The navigator refused the step (book end): the true page never moved. */
    fun onTurnRefused() {
        if (pendingIndex == null) return
        pendingIndex = null
        _positionsVersion.value += 1
    }

    /** Slider seek: jumps the navigator to the position table entry. */
    fun seekTo(progression: Float) {
        val nav = store.navigator ?: return
        val positions = positionsCache
        if (positions.isEmpty()) return
        markProgrammaticNav()
        val index = (progression.coerceIn(0f, 1f) * (positions.size - 1)).roundToInt()
            .coerceIn(0, positions.size - 1)
        nav.go(positions[index])
        // A seek rebases position: the optimistic page no longer applies.
        if (pendingIndex != null) {
            pendingIndex = null
            _positionsVersion.value += 1
        }
    }

    /**
     * Performs one page step. The slide itself is drawn by the UI (it needs a
     * snapshot of the outgoing page); this only advances the navigator, whose
     * paged layout snaps scroll offsets to page boundaries and therefore
     * cannot be animated from inside the document.
     */
    fun completeTurn(forward: Boolean): Boolean {
        val nav = store.navigator ?: return false
        markProgrammaticNav()
        return if (forward) nav.goForward(false) else nav.goBackward(false)
    }

    // Page arithmetic

    private fun pagesWithin(href: String, pages: Int, locator: Locator?): Int {
        // The navigator's own page index is exact; it is only stale for the
        // instant between a locator tick and its page-change event.
        if (href == currentHref) return currentPageIndex.coerceIn(0, pages - 1)
        // Never laid out in this session: interpolate the in-chapter
        // progression, which is what the saved locator carries on open.
        val progression = locator?.locations?.progression?.takeIf { it.isFinite() }
        return if (progression != null) {
            (progression * pages).toInt().coerceIn(0, pages - 1)
        } else {
            0
        }
    }

    /**
     * Rebuilds the per-chapter totals: exact where the navigator has laid the
     * chapter out, estimated from the observed pages-per-position ratio where
     * it has not. Nothing is shown until at least one chapter is known, so the
     * counter never flashes a number in the wrong unit.
     */
    @Suppress("ReturnCount") // Two early exits: no table yet, nothing measured yet.
    private fun recompute() {
        if (chapterOrder.isEmpty() || chapterPages.isEmpty()) {
            pagesByChapter = emptyMap()
            pagesBefore = emptyMap()
            totalPages = 0
            return
        }
        val knownPositions = chapterPages.keys.sumOf { positionsPerChapter[it] ?: 0 }
        // A single-position chapter (a cover, a title page) renders one page
        // and would set the ratio to 1 page per position, which undercounts a
        // real text chapter by several times. The samples have to cover
        // enough text to mean anything; until one does, and until every
        // chapter is known, there is no honest total to show.
        val everyChapterMeasured = chapterOrder.all { chapterPages.containsKey(it) }
        if (!everyChapterMeasured && knownPositions < MIN_RATIO_POSITIONS) {
            pagesByChapter = emptyMap()
            pagesBefore = emptyMap()
            totalPages = 0
            return
        }
        val knownPages = chapterPages.values.sum()
        val ratio = if (knownPositions > 0) knownPages.toFloat() / knownPositions else 0f
        val pages = chapterOrder.associateWith { href ->
            chapterPages[href] ?: estimatePages(href, ratio)
        }
        val before = HashMap<String, Int>(pages.size)
        var running = 0
        chapterOrder.forEach { href ->
            before[href] = running
            running += pages.getValue(href)
        }
        pagesByChapter = pages
        pagesBefore = before
        totalPages = running
    }

    private companion object {
        /**
         * Positions a sample must cover before its pages-per-position ratio is
         * used to estimate unread chapters. Two is enough to stop a cover page
         * from defining the whole book.
         */
        const val MIN_RATIO_POSITIONS = 2
    }

    private fun estimatePages(href: String, ratio: Float): Int {
        val positions = positionsPerChapter[href] ?: 0
        if (positions <= 0) return 0
        // Not enough data for a ratio yet: one position is one block of text,
        // which is the closest thing to a page this table offers.
        if (ratio <= 0f) return positions
        return (positions * ratio).roundToInt().coerceAtLeast(1)
    }
}
