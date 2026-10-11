package com.iridium.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryBackupCodecTest {

    @Test
    fun `round-trips a full backup`() {
        val backup = LibraryBackup(
            createdAt = 1234L,
            books = listOf(
                BackupBook(
                    sourcePath = "content://tree/book.epub",
                    title = "The Odyssey",
                    author = "Homer",
                    progress = 0.42f,
                    lastLocator = """{"href":"x.html"}""",
                    bookmarked = true,
                    highlights = listOf(
                        BackupHighlight("h1", "x.html", "s", "e", "Wine-dark sea", "YELLOW", "note", 5L),
                    ),
                    bookmarks = listOf(BackupBookmark("b1", "locator", "p. 3", 7L)),
                ),
            ),
        )
        val decoded = LibraryBackupCodec.decode(LibraryBackupCodec.encode(backup))

        assertEquals(1, decoded.books.size)
        val book = decoded.books.first()
        assertEquals("content://tree/book.epub", book.sourcePath)
        assertEquals(0.42f, book.progress, 0.001f)
        assertTrue(book.bookmarked)
        assertEquals("Homer", book.author)
        assertEquals("h1", book.highlights.first().id)
        assertEquals("p. 3", book.bookmarks.first().label)
        assertEquals("""{"href":"x.html"}""", book.lastLocator)
    }

    @Test
    fun `empty library round-trips cleanly`() {
        val backup = LibraryBackup()
        assertEquals(backup, LibraryBackupCodec.decode(LibraryBackupCodec.encode(backup)))
    }
}
