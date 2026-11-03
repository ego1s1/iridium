package com.iridium.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
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
class BookDaoTest {

    private lateinit var database: IridiumDatabase
    private lateinit var dao: BookDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            IridiumDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.bookDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `upsert then observe returns the row`() = runTest {
        dao.upsert(linkedBook("content://docs/a.epub", title = "Treasure Island"))
        val books = dao.observeAll().first()
        assertEquals(1, books.size)
        assertEquals("Treasure Island", books.first().title)
    }

    @Test
    fun `library query filters and orders rows in the database`() = runTest {
        dao.upsertAll(
            listOf(
                linkedBook("unread", title = "Unread").copy(createdAt = 3L),
                linkedBook("started", title = "Started")
                    .copy(progress = 0.4f, createdAt = 2L, updatedAt = 9L),
                linkedBook("finished", title = "Finished")
                    .copy(progress = 1f, createdAt = 1L, updatedAt = 5L),
                linkedBook("favorite", title = "Favorite")
                    .copy(bookmarked = true, createdAt = 4L),
                linkedBook("error", title = "Error")
                    .copy(error = "CORRUPT", bookmarked = true, createdAt = 5L),
            ),
        )

        val inProgress = dao.observeForLibrary("IN_PROGRESS", false, "RECENTLY_ADDED")
            .first().map { it.id }
        val unread = dao.observeForLibrary("UNREAD", false, "RECENTLY_ADDED")
            .first().map { it.id }
        val finished = dao.observeForLibrary("FINISHED", false, "RECENTLY_ADDED")
            .first().map { it.id }
        val favorites = dao.observeForLibrary("FAVORITES", true, "TITLE")
            .first().map { it.id }
        val unfinishedFirst = dao.observeForLibrary("ALL", false, "UNFINISHED_FIRST")
            .first().map { it.id }
        val recentlyAdded = dao.observeForLibrary("ALL", false, "RECENTLY_ADDED")
            .first().map { it.id }

        assertEquals(listOf("started"), inProgress)
        assertEquals(listOf("unread"), unread)
        assertEquals(listOf("finished"), finished)
        assertEquals(listOf("favorite"), favorites)
        assertEquals(listOf("started", "error", "favorite", "unread", "finished"), unfinishedFirst)
        assertEquals(listOf("error", "favorite", "unread", "started", "finished"), recentlyAdded)
    }

    @Test
    fun `deleteByIds removes exactly the rows the scan found missing`() = runTest {
        dao.upsert(linkedBook("content://docs/a.epub"))
        dao.upsert(linkedBook("content://docs/b.epub"))
        dao.upsert(linkedBook("/data/local/c.epub"))

        dao.deleteByIds(listOf("content://docs/b.epub"))

        assertEquals(
            setOf("content://docs/a.epub", "/data/local/c.epub"),
            dao.getAll().map { it.id }.toSet(),
        )
    }

    @Test
    fun `deleteAllLinked leaves non-linked rows`() = runTest {
        dao.upsert(linkedBook("content://docs/a.epub"))
        dao.upsert(linkedBook("/data/local/c.epub"))

        dao.deleteAllLinked()

        assertEquals(listOf("/data/local/c.epub"), dao.getAll().map { it.id })
    }

    @Test
    fun `updateProgress persists progress and locator`() = runTest {
        dao.upsert(linkedBook("content://docs/a.epub"))
        dao.updateProgress("content://docs/a.epub", 0.42f, "{\"href\":\"ch1\"}", 99L)

        val row = dao.getById("content://docs/a.epub")
        assertEquals(0.42f, row?.progress)
        assertEquals("{\"href\":\"ch1\"}", row?.lastLocator)
    }

    @Test
    fun `getUpdatedSince returns only rows touched after the snapshot`() = runTest {
        dao.upsert(linkedBook("content://docs/old.epub").copy(updatedAt = 100L))
        dao.upsert(linkedBook("content://docs/new.epub").copy(updatedAt = 300L))

        val delta = dao.getUpdatedSince(sinceMs = 200L).map { it.id }.toSet()

        assertTrue("content://docs/new.epub" in delta)
        assertTrue("content://docs/old.epub" !in delta)
    }

    private fun linkedBook(sourcePath: String, title: String = "Book") = BookEntity(
        id = sourcePath,
        title = title,
        author = null,
        format = "EPUB",
        spineCount = 3,
        sourcePath = sourcePath,
        coverPath = null,
        progress = 0f,
        lastLocator = null,
        sourceDisplayName = sourcePath.substringAfterLast('/'),
        sourceModified = 1L,
        tocJson = null,
        error = null,
        createdAt = 1L,
        updatedAt = 1L,
        bookmarked = false,
    )
}
