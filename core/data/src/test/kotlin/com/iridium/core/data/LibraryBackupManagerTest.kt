package com.iridium.core.data

import com.iridium.core.fakes.TestBooksRepository
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.HighlightColor
import com.iridium.core.testing.TestData
import com.iridium.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LibraryBackupManagerTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = TestBooksRepository()
    private val manager = LibraryBackupManager(repository)

    @Test
    fun `snapshot captures metadata`() = runTest {
        repository.setBooks(
            listOf(TestData.book(id = "1", title = "Odyssey", progress = 0.3f)),
        )
        repository.setHighlights(
            listOf(
                Highlight(
                    id = "h1", bookId = "1", href = "x.html",
                    startLocator = "s", endLocator = "e",
                    selectedText = "wine-dark", color = HighlightColor.BLUE,
                ),
            ),
        )
        repository.setBookmarks(listOf(Bookmark(id = "b1", bookId = "1", locator = "l", label = "p.3")))
        val json = manager.snapshot()
        assertTrue(json.contains("Odyssey"))
        assertTrue(json.contains("wine-dark"))
        assertTrue(json.contains("BLUE"))
        assertTrue(json.contains("p.3"))
    }

    @Test
    fun `restore reapplies highlights and progress`() = runTest {
        repository.setBooks(
            listOf(TestData.book(id = "1", title = "Odyssey", progress = 0.3f)),
        )
        repository.setHighlights(
            listOf(
                Highlight(
                    id = "h1", bookId = "1", href = "x.html",
                    startLocator = "s", endLocator = "e",
                    selectedText = "wine-dark", color = HighlightColor.GREEN,
                ),
            ),
        )
        val json = manager.snapshot()

        // Simulate data loss: clear metadata after the snapshot.
        repository.setHighlights(emptyList())
        repository.setBooks(listOf(TestData.book(id = "1", title = "Odyssey", progress = 0f)))

        val report = manager.restore(json)
        assertEquals(1, report.restored)
        assertEquals(0, report.skipped)
        assertEquals(1, repository.observeHighlights("1").first().size)
        assertEquals(HighlightColor.GREEN, repository.observeHighlights("1").first().first().color)
    }

    @Test
    fun `restore skips books no longer present`() = runTest {
        val json = "{\"version\":1,\"createdAt\":0,\"books\":[" +
            "{\"sourcePath\":\"content://docs/gone.epub\",\"title\":\"Gone\",\"progress\":0.5,\"bookmarked\":false," +
            "\"highlights\":[],\"bookmarks\":[]}]}"
        val report = manager.restore(json)
        assertEquals(0, report.restored)
        assertEquals(1, report.skipped)
    }

    @Test(expected = java.io.IOException::class)
    fun `restore throws IOException on corrupt json`() = runTest {
        manager.restore("{not-json")
    }
}
