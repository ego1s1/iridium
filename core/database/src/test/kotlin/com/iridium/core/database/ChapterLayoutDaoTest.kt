package com.iridium.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChapterLayoutDaoTest {

    private lateinit var database: IridiumDatabase
    private lateinit var dao: ChapterLayoutDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            IridiumDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.chapterLayoutDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `upsert then get returns the row`() = runTest {
        dao.upsert(row("book-1", "layout-a", pages = "{\"ch1.xhtml\":4}"))

        val loaded = dao.get("book-1", "layout-a")

        assertNotNull(loaded)
        assertEquals("{\"ch1.xhtml\":4}", loaded?.pagesJson)
        assertNull(dao.get("book-1", "layout-b"))
    }

    @Test
    fun `upsert overwrites the same book and layout`() = runTest {
        dao.upsert(row("book-1", "layout-a", updatedAt = 1L))
        dao.upsert(row("book-1", "layout-a", pages = "{\"ch1.xhtml\":9}", updatedAt = 2L))

        assertEquals("{\"ch1.xhtml\":9}", dao.get("book-1", "layout-a")?.pagesJson)
    }

    @Test
    fun `prune keeps only the most recent layouts for a book`() = runTest {
        dao.upsert(row("book-1", "a", updatedAt = 1L))
        dao.upsert(row("book-1", "b", updatedAt = 2L))
        dao.upsert(row("book-1", "c", updatedAt = 3L))
        dao.upsert(row("book-1", "d", updatedAt = 4L))

        dao.prune("book-1", keep = 2)

        assertNull(dao.get("book-1", "a"))
        assertNull(dao.get("book-1", "b"))
        assertNotNull(dao.get("book-1", "c"))
        assertNotNull(dao.get("book-1", "d"))
    }

    @Test
    fun `prune never touches another book`() = runTest {
        dao.upsert(row("book-1", "a", updatedAt = 1L))
        dao.upsert(row("book-2", "a", updatedAt = 1L))

        dao.prune("book-1", keep = 1)

        assertNotNull(dao.get("book-2", "a"))
    }

    private fun row(
        bookId: String,
        layoutKey: String,
        pages: String = "{}",
        updatedAt: Long = 1L,
    ) = ChapterLayoutEntity(
        bookId = bookId,
        layoutKey = layoutKey,
        pagesJson = pages,
        updatedAt = updatedAt,
    )
}
