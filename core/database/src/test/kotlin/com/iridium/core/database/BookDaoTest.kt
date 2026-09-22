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
    fun `deleteMissingLinked prunes only linked rows`() = runTest {
        dao.upsert(linkedBook("content://docs/a.epub"))
        dao.upsert(linkedBook("content://docs/b.epub"))
        dao.upsert(linkedBook("/data/local/c.epub"))

        dao.deleteMissingLinked(listOf("content://docs/a.epub"))

        val remaining = dao.getIds().toSet()
        assertTrue("content://docs/a.epub" in remaining)
        assertTrue("content://docs/b.epub" !in remaining)
        assertTrue("/data/local/c.epub" in remaining)
    }

    @Test
    fun `deleteAllLinked leaves non-linked rows`() = runTest {
        dao.upsert(linkedBook("content://docs/a.epub"))
        dao.upsert(linkedBook("/data/local/c.epub"))

        dao.deleteAllLinked()

        assertEquals(listOf("/data/local/c.epub"), dao.getIds())
    }

    @Test
    fun `updateProgress persists progress and locator`() = runTest {
        dao.upsert(linkedBook("content://docs/a.epub"))
        dao.updateProgress("content://docs/a.epub", 0.42f, "{\"href\":\"ch1\"}", 99L)

        val row = dao.getById("content://docs/a.epub")
        assertEquals(0.42f, row?.progress)
        assertEquals("{\"href\":\"ch1\"}", row?.lastLocator)
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
