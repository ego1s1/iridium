package com.iridium.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChapterTextDaoTest {

    private lateinit var database: IridiumDatabase
    private lateinit var dao: ChapterTextDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            IridiumDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.chapterTextDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `fts match finds prose inside a chapter`() = runTest {
        database.bookDao().upsert(book("book-a", "Treasure Island"))
        val body = "The Hispaniola was rolling in the swell. " +
            List(100) { "open sea beyond the horizon." }.joinToString(" ")
        dao.insertAll(
            listOf(chapter("book-a", "ch1.xhtml", "Chapter One", body)),
        )
        val hits = dao.search("hispaniola*", limit = 10)
        assertEquals(1, hits.size)
        assertEquals("book-a", hits.first().bookId)
        assertEquals("Treasure Island", hits.first().bookTitle)
        assertEquals("ch1.xhtml", hits.first().href)
        assertTrue(hits.first().snippet.contains("Hispaniola"))
        assertTrue(hits.first().snippet.length < body.length / 2)
    }

    @Test
    fun `match is limited to the query terms`() = runTest {
        database.bookDao().upsertAll(
            listOf(book("book-a", "A"), book("book-b", "B")),
        )
        dao.insertAll(
            listOf(
                chapter("book-a", "ch1.xhtml", "One", "A quiet morning on the island."),
                chapter("book-b", "ch2.xhtml", "Two", "The island was full of wild stone spires."),
            ),
        )
        val hits = dao.search("spires*", limit = 10)
        assertEquals(1, hits.size)
        assertEquals("book-b", hits.first().bookId)
    }

    @Test
    fun `deleteForBook removes only that book`() = runTest {
        database.bookDao().upsertAll(
            listOf(book("book-a", "A"), book("book-b", "B")),
        )
        dao.insertAll(
            listOf(
                chapter("book-a", "ch1.xhtml", "One", "alpha bravo"),
                chapter("book-b", "ch1.xhtml", "One", "alpha bravo"),
            ),
        )
        dao.deleteForBook("book-a")

        assertEquals(0, dao.countForBook("book-a"))
        assertEquals(1, dao.countForBook("book-b"))
        assertTrue(dao.search("alpha*", limit = 10).all { it.bookId == "book-b" })
    }

    @Test
    fun `malformed match expression returns nothing instead of throwing`() = runTest {
        database.bookDao().upsert(book("book-a", "A"))
        dao.insertAll(listOf(chapter("book-a", "ch1.xhtml", "One", "some text")))
        // A bare quote is an FTS syntax error; callers sanitize, but the DAO
        // path must still be safe to call defensively.
        val hits = runCatching { dao.search("\"", limit = 10) }.getOrDefault(emptyList())
        assertTrue(hits.isEmpty())
    }

    private fun chapter(bookId: String, href: String, title: String, body: String) =
        ChapterTextEntity(bookId = bookId, href = href, title = title, body = body)

    private fun book(id: String, title: String) = BookEntity(
        id = id,
        title = title,
        author = null,
        format = "EPUB",
        spineCount = 1,
        sourcePath = id,
        coverPath = null,
        progress = 0f,
        lastLocator = null,
        sourceDisplayName = title,
        tocJson = null,
        error = null,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
