package com.iridium.feature.reader.impl

import com.iridium.core.fakes.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Position resolution: the counter must track continuous progress, not just
 * chapter starts. Regression test for the frozen counter (every page of a
 * long chapter sharing one href resolved to the chapter's first position).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderPositionTrackerTest {

    private val store = ReaderSessionStore()
    private val tracker = ReaderPositionTracker(store, FakeClock())

    @Test
    fun `same href with rising progression advances the index`() {
        tracker.replaceAll(List(10) { locator(href = "ch1.xhtml", total = it / 10.0) })
        tracker.updatePagination(renderedPages = 10, chapterPositions = 10)

        store.onLocator(locator(href = "ch1.xhtml", total = 0.05))
        assertEquals(0 to 10, tracker.indexAndCount())

        store.onLocator(locator(href = "ch1.xhtml", total = 0.55))
        assertEquals(5 to 10, tracker.indexAndCount())

        store.onLocator(locator(href = "ch1.xhtml", total = 0.99))
        assertEquals(9 to 10, tracker.indexAndCount())
    }

    @Test
    fun `null progression falls back to href match`() {
        tracker.replaceAll(
            listOf(
                locator(href = "ch1.xhtml", total = null),
                locator(href = "ch2.xhtml", total = null),
            ),
        )
        tracker.updatePagination(renderedPages = 2, chapterPositions = 2)

        store.onLocator(locator(href = "ch2.xhtml", total = null))
        assertEquals(1 to 2, tracker.indexAndCount())
    }

    @Test
    fun `null progression with unknown href resolves to start`() {
        tracker.replaceAll(listOf(locator(href = "ch1.xhtml", total = null)))
        tracker.updatePagination(renderedPages = 1, chapterPositions = 1)

        store.onLocator(locator(href = "gone.xhtml", total = null))
        assertEquals(0 to 1, tracker.indexAndCount())
    }

    @Test
    fun `empty table resolves to zero`() {
        store.onLocator(locator(href = "ch1.xhtml", total = 0.5))
        assertEquals(0 to 0, tracker.indexAndCount())
        assertEquals(null, tracker.positionText())
    }

    @Test
    fun `position text is one-based`() {
        tracker.replaceAll(List(10) { locator(href = "ch1.xhtml", total = it / 10.0) })
        tracker.updatePagination(renderedPages = 10, chapterPositions = 10)
        store.onLocator(locator(href = "ch1.xhtml", total = 0.25))
        assertEquals("3 / 10", tracker.positionText())
    }

    private fun locator(href: String, total: Double?): Locator = Locator(
        href = Url(href)!!,
        mediaType = MediaType.XHTML,
        title = null,
        locations = Locator.Locations(
            fragments = emptyList(),
            progression = null,
            position = null,
            totalProgression = total,
        ),
        text = Locator.Text(),
    )
}
