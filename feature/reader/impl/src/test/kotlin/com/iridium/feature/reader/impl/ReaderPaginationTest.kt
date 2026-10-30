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
 * Book-wide page totals: exact for chapters the navigator has laid out,
 * estimated from the observed pages-per-position ratio for the rest, and
 * stable once the counts are restored for the same typography.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderPaginationTest {

    private val store = ReaderSessionStore()
    private val tracker = ReaderPositionTracker(store, FakeClock())

    @Test
    fun `without a laid-out chapter the counter stays empty`() {
        // The position table alone cannot produce pages; showing its size
        // would display a number in the wrong unit.
        tracker.replaceAll(List(692) { locator("ch1.xhtml", it / 692.0) })
        store.onLocator(locator("ch1.xhtml", 0.5))
        assertEquals(0 to 0, tracker.indexAndCount())
    }

    @Test
    fun `restored counts give the total before the first turn`() {
        tracker.applyLayout("layout-a")
        tracker.replaceAll(
            listOf(
                locator("ch1.xhtml", 0.0),
                locator("ch1.xhtml", 0.5),
                locator("ch2.xhtml", 0.5),
            ),
        )
        tracker.restorePersisted("layout-a", mapOf("ch1.xhtml" to 4))

        // ch1: 4 rendered pages over 2 positions -> ratio 2, so ch2's single
        // position is estimated at 2 pages. Total 6, opening at page 1.
        store.onLocator(locator("ch1.xhtml", 0.0))
        assertEquals(0 to 6, tracker.indexAndCount())
    }

    @Test
    fun `unread chapters are estimated from the measured ratio`() {
        tracker.replaceAll(
            listOf(
                locator("ch1.xhtml", 0.0),
                locator("ch1.xhtml", 0.5),
                locator("ch2.xhtml", 0.3),
                locator("ch2.xhtml", 0.6),
                locator("ch2.xhtml", 0.9),
            ),
        )
        tracker.onPageChanged("ch1.xhtml", 0, totalPages = 4)

        // 4 pages over 2 positions -> 2 pages per position; ch2 holds 3
        // positions, so 6 pages. Nothing is written for ch2.
        assertEquals(4 + 6, tracker.indexAndCount().second)
    }

    @Test
    fun `a single-position chapter does not invent a total`() {
        // A cover page renders one page from one position, which would read as
        // "1 page per position" and undercount the whole book several times
        // over. Observed on device: 1 / 692 until real text was laid out.
        tracker.replaceAll(
            listOf(locator("cover.xhtml", 0.0)) +
                List(5) { locator("ch1.xhtml", it / 5.0) },
        )
        tracker.onPageChanged("cover.xhtml", pageIndex = 0, totalPages = 1)

        assertEquals(0 to 0, tracker.indexAndCount())
    }

    @Test
    fun `a fully measured book shows its exact total`() {
        tracker.replaceAll(
            listOf(
                locator("cover.xhtml", 0.0),
                locator("ch1.xhtml", 0.5),
            ),
        )
        tracker.onPageChanged("cover.xhtml", pageIndex = 0, totalPages = 1)
        // Every chapter known: no estimate is needed, so the small sample no
        // longer matters.
        tracker.onPageChanged("ch1.xhtml", pageIndex = 0, totalPages = 9)

        // The cover is page 0, so the first page of ch1 is index 1.
        assertEquals(1 to 10, tracker.indexAndCount())
    }

    @Test
    fun `a chapter is counted once even when reported repeatedly`() {
        tracker.replaceAll(List(4) { locator("ch1.xhtml", it / 4.0) })
        repeat(5) { tracker.onPageChanged("ch1.xhtml", 0, totalPages = 7) }

        assertEquals(0 to 7, tracker.indexAndCount())
    }

    @Test
    fun `a new typography drops the learned counts`() {
        tracker.applyLayout("small")
        tracker.replaceAll(List(2) { locator("ch1.xhtml", it / 2.0) })
        tracker.onPageChanged("ch1.xhtml", 0, totalPages = 6)
        assertEquals(6, tracker.indexAndCount().second)

        // Bigger font: the same text renders into more pages, so the old
        // counts must not survive.
        tracker.applyLayout("large")
        assertEquals(0 to 0, tracker.indexAndCount())
        assertEquals(emptyMap<String, Int>(), tracker.snapshotChapterPages())
    }

    @Test
    fun `restored counts for a stale layout are ignored`() {
        tracker.applyLayout("current")
        tracker.replaceAll(List(2) { locator("ch1.xhtml", it / 2.0) })

        tracker.restorePersisted("previous", mapOf("ch1.xhtml" to 9))

        assertEquals(0 to 0, tracker.indexAndCount())
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
