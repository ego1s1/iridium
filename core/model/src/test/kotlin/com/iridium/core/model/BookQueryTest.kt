package com.iridium.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookQueryTest {

    private fun book(
        id: String,
        title: String,
        progress: Float = 0f,
        author: String? = null,
        updatedAt: Long = 0L,
        createdAt: Long = 0L,
        error: BookError? = null,
        bookmarked: Boolean = false,
    ) = Book(
        id = id,
        title = title,
        author = author,
        sourcePath = "/books/$id.epub",
        coverPath = null,
        progress = progress,
        sourceDisplayName = "$id.epub",
        createdAt = createdAt,
        updatedAt = updatedAt,
        error = error,
        bookmarked = bookmarked,
    )

    @Test
    fun `text matches title and author case-insensitively`() {
        val books = listOf(
            book("1", "Treasure Island", author = "Stevenson"),
            book("2", "Huck Finn", author = "Mark Twain"),
        )
        val result = books.applyQuery(LibraryQuery(text = "twain"))
        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test
    fun `filter in-progress and favorites`() {
        val books = listOf(
            book("1", "A", progress = 0.5f),
            book("2", "B", progress = 0f, bookmarked = true),
            book("3", "C", progress = 1f),
        )
        assertEquals(
            listOf("1"),
            books.applyQuery(LibraryQuery(filter = LibraryFilter.IN_PROGRESS)).map { it.id },
        )
        assertEquals(
            listOf("2"),
            books.applyQuery(LibraryQuery(filter = LibraryFilter.FAVORITES)).map { it.id },
        )
        assertEquals(
            listOf("3"),
            books.applyQuery(LibraryQuery(filter = LibraryFilter.FINISHED)).map { it.id },
        )
    }

    @Test
    fun `title sort is case-insensitive`() {
        val books = listOf(book("1", "banana"), book("2", "Apple"))
        val result = books.applyQuery(LibraryQuery(sortOrder = LibrarySortOrder.TITLE))
        assertEquals(listOf("2", "1"), result.map { it.id })
    }

    @Test
    fun `continue shelf is in-progress by recency`() {
        val books = listOf(
            book("1", "A", progress = 0.2f, updatedAt = 1L),
            book("2", "B", progress = 0f, updatedAt = 9L),
            book("3", "C", progress = 0.8f, updatedAt = 5L),
            book("4", "D", progress = 1f, updatedAt = 7L),
        )
        assertEquals(listOf("3", "1"), books.continueShelf().map { it.id })
    }

    @Test
    fun `resume target skips errors and untouched books`() {
        val books = listOf(
            book("1", "A", progress = 0.9f, updatedAt = 1L),
            book("2", "B", progress = 0.9f, updatedAt = 5L, error = BookError.CORRUPT),
            book("3", "C", progress = 0f, updatedAt = 9L),
        )
        assertEquals("1", books.resumeTarget()?.id)
        assertNull(emptyList<Book>().resumeTarget())
    }
}
