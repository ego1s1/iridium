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
 * Page index resolution. The navigator reports the exact page it just laid
 * out; anything that derives a page number from the position table alone is a
 * bug (the table is font-independent and chunks text, not layouts).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderPositionTrackerTest {

    private val store = ReaderSessionStore()
    private val tracker = ReaderPositionTracker(store, FakeClock())

    @Test
    fun `one page step advances the counter by exactly one`() {
        tracker.replaceAll(List(3) { locator(href = "ch1.xhtml", progression = it / 3.0) })
        tracker.onPageChanged("ch1.xhtml", pageIndex = 0, totalPages = 3)
        store.onLocator(locator("ch1.xhtml", 0.0))
        assertEquals(0 to 3, tracker.indexAndCount())

        tracker.onPageChanged("ch1.xhtml", pageIndex = 1, totalPages = 3)
        assertEquals(1 to 3, tracker.indexAndCount())

        tracker.onPageChanged("ch1.xhtml", pageIndex = 2, totalPages = 3)
        assertEquals(2 to 3, tracker.indexAndCount())
    }

    @Test
    fun `a tap advances immediately and the navigator value replaces it`() {
        tracker.replaceAll(List(5) { locator(href = "ch1.xhtml", progression = it / 5.0) })
        tracker.onPageChanged("ch1.xhtml", pageIndex = 0, totalPages = 5)

        tracker.onTurnRequested(1)
        assertEquals(1 to 5, tracker.indexAndCount())

        // The navigator confirms the same page: no jump, no snap-back.
        tracker.onPageChanged("ch1.xhtml", pageIndex = 1, totalPages = 5)
        assertEquals(1 to 5, tracker.indexAndCount())
    }

    @Test
    fun `a refused turn reverts to the real page`() {
        tracker.replaceAll(List(3) { locator(href = "ch1.xhtml", progression = it / 3.0) })
        tracker.onPageChanged("ch1.xhtml", pageIndex = 1, totalPages = 3)

        tracker.onTurnRequested(1)
        assertEquals(2 to 3, tracker.indexAndCount())

        // End of book: the navigator never moved.
        tracker.onTurnRefused()
        assertEquals(1 to 3, tracker.indexAndCount())
    }

    @Test
    fun `opening mid-chapter places the page from its progression`() {
        tracker.applyLayout("layout-a")
        tracker.replaceAll(List(3) { locator(href = "ch1.xhtml", progression = it / 3.0) })
        tracker.restorePersisted("layout-a", mapOf("ch1.xhtml" to 10))

        // No page event yet (the navigator has not reported): the saved
        // locator's in-chapter progression is the best available answer.
        store.onLocator(locator("ch1.xhtml", 0.5))
        assertEquals(5 to 10, tracker.indexAndCount())
    }

    @Test
    fun `a chapter without progression resolves to its first page`() {
        tracker.applyLayout("layout-a")
        tracker.replaceAll(
            listOf(
                locator("ch1.xhtml", null),
                locator("ch1.xhtml", null),
                locator("ch2.xhtml", 0.5),
                locator("ch2.xhtml", 0.9),
            ),
        )
        tracker.restorePersisted("layout-a", mapOf("ch1.xhtml" to 5))

        store.onLocator(locator("ch2.xhtml", null))
        // ch1's 5 pages land before ch2; ch2 starts at its first page.
        assertEquals(5 to 10, tracker.indexAndCount())
    }

    @Test
    fun `an empty table resolves to zero`() {
        store.onLocator(locator(href = "ch1.xhtml", progression = 0.5))
        assertEquals(0 to 0, tracker.indexAndCount())
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
            progression = progression,
            position = null,
            totalProgression = progression,
        ),
        text = Locator.Text(),
    )
}
