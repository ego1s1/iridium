package com.iridium.feature.reader.impl

import com.iridium.core.common.AppClock
import com.iridium.core.common.safeCatching
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.readium.r2.shared.publication.Locator

/**
 * Positions table + scrubber math for the reader. Owns the cached positions,
 * the recombine trigger version, the programmatic-nav grace clock, and
 * slider seeks; the ViewModel only forwards events.
 */
internal class ReaderPositionTracker(
    private val store: ReaderSessionStore,
    private val clock: AppClock,
) {

    private companion object {
        /**
         * Pages in the current chapter as rendered right now. A paged
         * reflowable document is a row of page-wide columns, so the document
         * width over the viewport width is the page count — and it changes
         * with every typography change.
         */
        const val PAGE_COUNT_SCRIPT =
            "(function(){var e=document.scrollingElement||document.documentElement;" +
                "var w=window.innerWidth;if(!e||!w||w<=0){return 0;}" +
                "return Math.max(1,Math.round(e.scrollWidth/w));})()"
    }

    /** Last programmatic jump (open/seek/TOC): locator ticks inside the grace window are the jump's own echo, not user page turns. */
    private var lastProgrammaticNavMs: Long = 0L

    /** Plain var, not a flow: every read and write happens on the Main thread
     * (viewModelScope + combine), so no synchronization is needed; the version
     * flow below drives recomposition. */
    private var positionsCache: List<Locator> = emptyList()
    private val _positionsVersion = MutableStateFlow(0)
    val positionsVersion: StateFlow<Int> = _positionsVersion.asStateFlow()

    /**
     * Optimistic page target while the navigator's locator echo is in flight.
     * The toolkit only reports a new locator once its own scroll settles, so
     * the counter would otherwise lag hundreds of ms behind the tap. The
     * display shows this target — never raw + delta — so an echo that has
     * already advanced the raw index can never be double-counted or make the
     * number jump forward and snap back.
     */
    private var pendingIndex: Int? = null
    private var pendingForward: Boolean = true

    /**
     * Rendered pages per position, measured from the live WebView. The
     * positions table is font-independent (it chunks the text), so using it
     * directly made the page counter static: it never reacted to a font-size
     * or margin change, and one page turn moved it by a fraction. Scaling by
     * the measured ratio makes both the current page and the total track the
     * real pagination.
     */
    private var pagesPerPosition: Float = 0f

    /** Per-chapter rendered-page samples: href -> pages. Averaging chapters
     *  keeps the total stable instead of re-sampling a different chapter. */
    private val renderedByChapter = mutableMapOf<String, Int>()

    /** True once a measurement has been attempted (correct-or-nothing total). */
    private var paginationAttempted: Boolean = false

    /** Sticky chapter title: locators between TOC anchors keep the last one. */
    private var lastChapterTitle: String? = null

    fun replaceAll(positions: List<Locator>) {
        positionsCache = positions
        _positionsVersion.value += 1
    }

    /** Typography changed: the rendered samples no longer describe the layout. */
    fun invalidatePagination() {
        renderedByChapter.clear()
        pagesPerPosition = 0f
        _positionsVersion.value += 1
    }

    /**
     * Records the pagination measured for [renderedPages] in a chapter that
     * holds [chapterPositions] table entries. Recomputed on attach and after
     * every reflow, so the counter always reflects the current typography.
     */
    fun updatePagination(renderedPages: Int, chapterPositions: Int, href: String? = null) {
        paginationAttempted = true
        if (renderedPages <= 0 || chapterPositions <= 0) return
        if (href != null) renderedByChapter[href] = renderedPages
        val sampledPositions = renderedByChapter.keys.sumOf { key ->
            positionsCache.count { it.href.toString() == key }
        }
        val sampledPages = renderedByChapter.values.sum()
        val ratio = if (sampledPositions > 0 && sampledPages > 0) {
            sampledPages.toFloat() / sampledPositions
        } else {
            renderedPages.toFloat() / chapterPositions
        }
        if (ratio.isFinite() && ratio > 0f && ratio != pagesPerPosition) {
            pagesPerPosition = ratio
            _positionsVersion.value += 1
        }
    }

    /** True once a rendered measurement exists (counter is meaningful). */
    val hasPagination: Boolean get() = pagesPerPosition > 0f

    /** Total rendered pages for the whole book at the current typography. */
    private fun totalPages(): Int {
        val base = positionsCache.size
        if (base == 0) return 0
        if (pagesPerPosition <= 0f) {
            // Before the first measurement, show nothing rather than the
            // font-independent table size: that value is wrong and used to
            // flash before the real total arrived.
            return if (paginationAttempted) base else 0
        }
        return (base * pagesPerPosition).roundToInt().coerceAtLeast(1)
    }

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

    fun positionText(): String? {
        val (index, count) = indexAndCount()
        if (count == 0) return null
        return "${index + 1} / $count"
    }

    /**
     * 0-based rendered page index + rendered page total for the counter.
     *
     * Both values come from one place so they can never disagree: the total is
     * the measured pagination, and the index is the locator's continuous
     * progression across it. A tap publishes an absolute [pendingIndex]
     * immediately; until the navigator's own index catches up, that value is
     * shown verbatim. Deliberately *not* "raw + delta": an echo that arrives
     * before the delta is cleared used to add to an index that had already
     * moved, which is what made the number jump by two or three and snap back.
     */
    fun indexAndCount(): Pair<Int, Int> {
        val (rawIndex, total) = rawIndexAndCount()
        if (total <= 0) return 0 to 0
        val pending = pendingIndex ?: return rawIndex to total
        val caughtUp = if (pendingForward) rawIndex >= pending else rawIndex <= pending
        if (caughtUp) {
            // The navigator's own position reached the optimistic target.
            android.util.Log.d("PageFlow", "caughtUp raw=$rawIndex pending=$pending total=$total")
            pendingIndex = null
            return rawIndex to total
        }
        return pending.coerceIn(0, total - 1) to total
    }

    /** Authoritative index from the live locator, ignoring any pending turn. */
    private fun rawIndexAndCount(): Pair<Int, Int> {
        val positions = positionsCache
        if (positions.isEmpty()) return 0 to 0
        val current = store.latestLocator.value ?: return 0 to totalPages()
        val total = totalPages()
        if (total <= 0) return 0 to 0
        // totalProgression is continuous across pages; hrefs repeat within a
        // chapter, so href-first matching freezes the counter mid-chapter.
        val progression = current.locations.totalProgression?.takeIf { it.isFinite() }
        if (progression != null) {
            return (progression * (total - 1)).roundToInt().coerceIn(0, total - 1) to total
        }
        val index = positions.indexOfFirst { it.href == current.href }
        return (if (index >= 0) index else 0) to total
    }

    /**
     * Registers a tap that will turn the page. The counter advances now, one
     * step from whatever is currently displayed, so repeated taps accumulate
     * predictably instead of compounding with a late echo.
     */
    fun onTurnRequested(direction: Int) {
        val (rawIndex, total) = rawIndexAndCount()
        if (total <= 0) return
        val base = pendingIndex ?: rawIndex
        pendingIndex = (base + direction).coerceIn(0, total - 1)
        android.util.Log.d("PageFlow", "turnRequested dir=$direction raw=$rawIndex pending=$pendingIndex total=$total")
        pendingForward = direction > 0
        _positionsVersion.value += 1
    }

    /** The navigator refused the step (book end): undo the optimistic move. */
    fun onTurnRefused(direction: Int) {
        val pending = pendingIndex ?: return
        val target = pending - direction
        val (rawIndex, _) = rawIndexAndCount()
        pendingIndex = if (target == rawIndex) null else target
        _positionsVersion.value += 1
    }

    /** Drops the optimistic target once a programmatic jump re-bases position. */
    fun clearPendingTurn() {
        if (pendingIndex == null) return
        pendingIndex = null
        _positionsVersion.value += 1
    }

    /** Reads the live WebView's page count for the current chapter. */
    suspend fun measurePagination() {
        val nav = store.navigator ?: return
        val current = store.latestLocator.value ?: return
        val raw = safeCatching { nav.evaluateJavascript(PAGE_COUNT_SCRIPT) }.getOrNull() ?: return
        val rendered = raw.trim().trim('"').toDoubleOrNull()?.roundToInt() ?: return
        val chapterPositions = positionsCache.count { it.href == current.href }
        updatePagination(rendered, chapterPositions, href = current.href.toString())
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
}
