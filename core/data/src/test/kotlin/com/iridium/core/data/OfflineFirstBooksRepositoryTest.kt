package com.iridium.core.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.iridium.core.database.BookEntity
import com.iridium.core.database.IridiumDatabase
import com.iridium.core.model.BookError
import com.iridium.core.model.Highlight
import com.iridium.core.model.LibraryQuery
import com.iridium.epub.engine.IridiumEpubEngine
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Repository-level integration: real Room, real EPUB bytes on disk, fake SAF
 * listing. Covers the linked-tree scan (including pruning) and the full-text
 * index lifecycle end to end.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineFirstBooksRepositoryTest {

    private lateinit var context: Context
    private lateinit var database: IridiumDatabase
    private lateinit var repository: OfflineFirstBooksRepository
    private lateinit var lister: FakeLister
    private lateinit var tempDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, IridiumDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        lister = FakeLister()
        tempDir = File(context.cacheDir, "repo-test").apply { mkdirs() }
        repository = OfflineFirstBooksRepository(
            bookDao = database.bookDao(),
            highlightDao = database.highlightDao(),
            bookmarkDao = database.bookmarkDao(),
            chapterTextDao = database.chapterTextDao(),
            backend = IridiumEpubEngine(),
            covers = EpubCoverGenerator(context),
            treeLister = lister,
            sourceFactory = EpubSourceFactory(context),
            chapterIndexer = ChapterIndexer(EpubSourceFactory(context), IridiumEpubEngine()),
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `scan indexes linked books with metadata and toc`() = runTest {
        val file = writeEpub("Treasure Island")
        lister.documents = listOf(LinkedDocument(Uri.fromFile(file), file.name, modified = 1L))

        val report = repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }

        assertEquals(1, report.total)
        assertEquals(0, report.failed)
        val books = repository.observeLibrary(LibraryQuery()).first()
        assertEquals(1, books.size)
        assertEquals("Treasure Island", books.first().title)
        assertEquals(2, books.first().spineCount)
    }

    @Test
    fun `rescan of an unchanged file reuses the row`() = runTest {
        val file = writeEpub("Book")
        val document = LinkedDocument(Uri.fromFile(file), file.name, modified = 5L)
        lister.documents = listOf(document)

        repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }
        val firstRun = repository.observeLibrary(LibraryQuery()).first().first()

        repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }
        val secondRun = repository.observeLibrary(LibraryQuery()).first().first()

        assertEquals(1, repository.observeLibrary(LibraryQuery()).first().size)
        assertEquals("ids stay stable across rescans", firstRun.id, secondRun.id)
        assertEquals(firstRun.createdAt, secondRun.createdAt)
    }

    @Test
    fun `books removed from the tree are pruned`() = runTest {
        // Pruning only ever targets linked rows (content:// documents); a
        // document the provider cannot open still yields a row to prune.
        lister.documents = listOf(linkedDocument("gone.epub"))
        repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }
        assertEquals(1, repository.observeLibrary(LibraryQuery()).first().size)

        // The document vanishes from the folder; the rescan must drop it.
        lister.documents = emptyList()
        repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }

        assertTrue(repository.observeLibrary(LibraryQuery()).first().isEmpty())
    }

    @Test
    fun `a failed walk never prunes`() = runTest {
        lister.documents = listOf(linkedDocument("kept.epub"))
        repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }

        // SAF listing failed: the result must not read as an empty folder.
        lister.walkFailed = true
        lister.documents = emptyList()
        repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }

        assertEquals(1, repository.observeLibrary(LibraryQuery()).first().size)
    }

    /** A linked document as a real scan produces it: a content:// URI. */
    private fun linkedDocument(name: String) =
        LinkedDocument(Uri.parse("content://docs/$name"), name, modified = 1L)

    @Test
    fun `unreadable files are indexed as typed error rows`() = runTest {
        val garbage = File(tempDir, "broken.epub").apply { writeBytes(ByteArray(1024) { 3 }) }
        lister.documents = listOf(LinkedDocument(Uri.fromFile(garbage), garbage.name, modified = 1L))

        val report = repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }

        assertEquals(1, report.total)
        assertEquals(1, report.failed)
        val book = repository.observeLibrary(LibraryQuery()).first().single()
        assertEquals(BookError.CORRUPT, book.error)
    }

    @Test
    fun `content indexing then search returns hits with book titles`() = runTest {
        val file = writeEpub("Treasure Island")
        lister.documents = listOf(LinkedDocument(Uri.fromFile(file), file.name, modified = 1L))
        repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }
        val book = repository.observeLibrary(LibraryQuery()).first().single()

        val indexed = repository.indexBookContent(book.id)
        assertEquals(2, indexed)
        assertTrue(repository.isContentIndexed(book.id))

        val hits = repository.searchContent("hispaniola")
        assertEquals(1, hits.size)
        assertEquals("Treasure Island", hits.first().bookTitle)
        assertTrue(hits.first().snippet.contains("Hispaniola"))
    }

    @Test
    fun `re-indexing replaces previous rows rather than duplicating`() = runTest {
        val file = writeEpub("Treasure Island")
        lister.documents = listOf(LinkedDocument(Uri.fromFile(file), file.name, modified = 1L))
        repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }
        val book = repository.observeLibrary(LibraryQuery()).first().single()

        repository.indexBookContent(book.id)
        repository.indexBookContent(book.id)

        assertEquals(2, database.chapterTextDao().countForBook(book.id))
    }

    @Test
    fun `search is empty for a query with nothing searchable`() = runTest {
        assertTrue(repository.searchContent("   ").isEmpty())
        assertTrue(repository.searchContent("***").isEmpty())
    }

    @Test
    fun `unlinking a book clears its annotations and index`() = runTest {
        val file = writeEpub("Treasure Island")
        lister.documents = listOf(LinkedDocument(Uri.fromFile(file), file.name, modified = 1L))
        repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }
        val book = repository.observeLibrary(LibraryQuery()).first().single()
        repository.indexBookContent(book.id)
        repository.upsertHighlight(
            Highlight(
                id = "h1", bookId = book.id, href = "ch1.xhtml",
                startLocator = "{}", endLocator = "{}", selectedText = "x",
            ),
        )
        assertTrue(repository.searchContent("hispaniola").isNotEmpty())

        repository.removeBook(book.id)

        assertTrue(repository.observeLibrary(LibraryQuery()).first().isEmpty())
        assertEquals(0, database.chapterTextDao().countForBook(book.id))
        assertTrue(database.highlightDao().observeForBook(book.id).first().isEmpty())
        assertTrue(repository.searchContent("hispaniola").isEmpty())
    }

    @Test
    fun `progress survives a rescan`() = runTest {
        val file = writeEpub("Book")
        lister.documents = listOf(LinkedDocument(Uri.fromFile(file), file.name, modified = 1L))
        repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }
        val book = repository.observeLibrary(LibraryQuery()).first().single()

        repository.updateProgress(book.id, 0.42f, "{\"href\":\"ch1.xhtml\"}")
        repository.indexLinkedTree(Uri.parse("content://tree")) { _, _ -> }

        val after = repository.observeLibrary(LibraryQuery()).first().single()
        assertEquals(0.42f, after.progress)
        assertFalse(after.isFinished)
    }

    /** SAF stand-in: the repository only ever asks for a document list. */
    private class FakeLister : LinkedTreeLister {
        var documents: List<LinkedDocument> = emptyList()
        var walkFailed: Boolean = false

        override suspend fun listBooks(treeUri: Uri): LinkedTreeListResult =
            LinkedTreeListResult(documents, walkFailed)

        override suspend fun resolve(documentUri: Uri): LinkedDocument? =
            documents.firstOrNull { it.uri == documentUri }
    }

    private fun writeEpub(title: String): File {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            fun put(name: String, data: String) {
                zip.putNextEntry(ZipEntry(name)); zip.write(data.toByteArray()); zip.closeEntry()
            }
            put("mimetype", "application/epub+zip")
            put(
                "META-INF/container.xml",
                """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""",
            )
            put(
                "OEBPS/content.opf",
                """<?xml version="1.0"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>$title</dc:title></metadata>
  <manifest>
    <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine><itemref idref="ch1"/><itemref idref="ch2"/></spine>
</package>""",
            )
            put(
                "OEBPS/ch1.xhtml",
                "<html><body><p>The Hispaniola was rolling scuppers under.</p></body></html>",
            )
            put(
                "OEBPS/ch2.xhtml",
                "<html><body><p>Wild stone spires stood above the grey woods.</p></body></html>",
            )
        }
        return File(tempDir, "$title-${System.nanoTime()}.epub")
            .apply { writeBytes(bos.toByteArray()) }
    }
}
