package com.iridium.feature.reader.impl

import com.iridium.core.fakes.FakeClock
import com.iridium.core.model.TocEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Rendered pagination: the page counter must follow the live layout, not the
 * font-independent position table. The table only supplies the scale; the
 * measured pages-per-position ratio turns it into real pages, so a typography
 * change moves the total and a page turn moves the index by exactly one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderPaginationTest {

    private val store = ReaderSessionStore()
    private val tracker = ReaderPositionTracker(store, FakeClock())

    @Test
    fun `measurement scales the total with the rendered layout`() {
        // 10 table entries; the chapter renders 5 pages over 2 entries, so the
        // book runs at 2.5 pages per entry -> 25 pages.
        tracker.replaceAll(
            List(10) { locator(if (it < 2) "ch1.xhtml" else "ch2.xhtml", it / 10.0) },
        )
        tracker.updatePagination(renderedPages = 5, chapterPositions = 2)

        store.onLocator(locator("ch1.xhtml", 0.0))
        assertEquals(0 to 25, tracker.indexAndCount())
    }

    @Test
    fun `without a measurement the counter stays empty instead of guessing`() {
        // Launch must never flash the font-independent table size: until the
        // first measurement there is no correct total to show.
        tracker.replaceAll(List(692) { locator("ch1.xhtml", it / 692.0) })
        store.onLocator(locator("ch1.xhtml", 0.5))
        assertEquals(0 to 0, tracker.indexAndCount())
        assertEquals(null, tracker.positionText())
    }

    @Test
    fun `a failed measurement falls back to the table size`() {
        tracker.replaceAll(List(10) { locator("ch1.xhtml", it / 10.0) })
        tracker.updatePagination(renderedPages = 0, chapterPositions = 0)
        store.onLocator(locator("ch1.xhtml", 0.0))
        assertEquals(0 to 10, tracker.indexAndCount())
    }

    @Test
    fun `one page step advances the counter by exactly one`() {
        tracker.replaceAll(List(4) { locator("ch1.xhtml", it / 4.0) })
        tracker.updatePagination(renderedPages = 4, chapterPositions = 4)

        // 4 entries rendering 4 pages = 1 page per entry, 4 pages total.
        store.onLocator(locator("ch1.xhtml", 0.0))
        assertEquals(0 to 4, tracker.indexAndCount())

        store.onLocator(locator("ch1.xhtml", 0.25))
        assertEquals(1 to 4, tracker.indexAndCount())

        store.onLocator(locator("ch1.xhtml", 0.5))
        assertEquals(2 to 4, tracker.indexAndCount())

        store.onLocator(locator("ch1.xhtml", 1.0))
        assertEquals(3 to 4, tracker.indexAndCount())
    }

    @Test
    fun `a larger font raises the total`() {
        tracker.replaceAll(List(20) { locator("ch1.xhtml", it / 20.0) })
        tracker.updatePagination(renderedPages = 2, chapterPositions = 4)
        store.onLocator(locator("ch1.xhtml", 0.0))
        val small = tracker.indexAndCount().second

        // Same text, more pages: the chapter now holds twice as many.
        tracker.updatePagination(renderedPages = 4, chapterPositions = 4)
        val large = tracker.indexAndCount().second

        assertEquals(10, small)
        assertEquals(20, large)
    }

    @Test
    fun `chapter title resolves from the toc and stays while between anchors`() {
        val toc = listOf(
            TocEntry(href = "Text/ch1.xhtml", title = "Foreword"),
            TocEntry(href = "Text/ch2.xhtml", title = "Chapter Two"),
            TocEntry(href = "Text/ch2.xhtml#part2", title = "Part Two"),
        )
        store.onLocator(locator("Text/ch2.xhtml", 0.4))
        assertEquals("Part Two", tracker.chapterTitle(toc))

        // An href the TOC does not list keeps the last known chapter.
        store.onLocator(locator("Text/ch2.xhtml?split=1", 0.5))
        assertEquals("Part Two", tracker.chapterTitle(toc))
    }

    @Test
    fun `chapter title is null before any locator`() {
        assertEquals(null, tracker.chapterTitle(listOf(TocEntry("Text/ch1.xhtml", "One"))))
    }

    private fun locator(href: String, progression: Double?): Locator = Locator(
        href = Url(href)!!,
        mediaType = MediaType.XHTML,
        title = null,
        locations = Locator.Locations(
            fragments = emptyList(),
            progression = null,
            position = null,
            totalProgression = progression,
        ),
        text = Locator.Text(),
    )
}
