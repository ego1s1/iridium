package com.iridium.core.data

import android.content.Context
import androidx.room.withTransaction
import com.iridium.core.database.BookDao
import com.iridium.core.database.BookEntity
import com.iridium.core.database.BookmarkDao
import com.iridium.core.database.ChapterTextDao
import com.iridium.core.database.HighlightDao
import com.iridium.core.database.IridiumDatabase
import com.iridium.core.model.Book
import com.iridium.core.model.BookError
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.TocEntry
import com.iridium.core.model.applyQuery
import com.iridium.epub.EpubBackend
import com.iridium.epub.EpubSource
import com.iridium.epub.InspectedEpub
import com.iridium.epub.nativecore.NativeEpub
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Outcome of one linked-tree scan. */
data class IndexReport(val total: Int, val failed: Int)

/** FTS overscan factor: fetch extra rows so round-robin fairness has a pool. */
private const val SEARCH_FANOUT = 4

/** A full-text match inside a chapter. */
data class ContentHit(
    val bookId: String,
    val bookTitle: String,
    val href: String,
    val chapterTitle: String,
    val snippet: String,
)

interface BooksRepository {
    fun observeLibrary(query: LibraryQuery): Flow<List<Book>>
    fun observeBook(id: String): Flow<Book?>
    fun observeToc(bookId: String): Flow<List<TocEntry>>
    fun observeHighlights(bookId: String): Flow<List<Highlight>>
    fun observeBookmarks(bookId: String): Flow<List<Bookmark>>

    /**
     * Scans all of shared storage in place and indexes every EPUB found.
     * Rows whose files are gone are pruned (never on a failed walk).
     */
    suspend fun indexFilesystem(
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): IndexReport

    suspend fun updateProgress(id: String, progress: Float, locator: String?)
    suspend fun setBookmarked(id: String, bookmarked: Boolean)
    suspend fun upsertHighlight(highlight: Highlight)
    suspend fun deleteHighlight(id: String)
    suspend fun deleteAllHighlights(bookId: String)
    suspend fun upsertBookmark(bookmark: Bookmark)
    suspend fun deleteBookmark(id: String)
    suspend fun deleteAllBookmarks(bookId: String)

    /**
     * Extracts and indexes a book's chapter text for full-text search.
     * Returns the number of chapters indexed (0 when unsupported/unreadable).
     */
    suspend fun indexBookContent(bookId: String): Int

    /** True when a book already has chapter text in the search index. */
    suspend fun isContentIndexed(bookId: String): Boolean

    /** Full-text search across every indexed chapter. */
    suspend fun searchContent(query: String, limit: Int = 40): List<ContentHit>

    /** Unlinks a book: removes the row, thumbnail and search index. */
    suspend fun removeBook(id: String)

    /**
     * Imports one EPUB shared from another app: copies the bytes into
     * app-private storage, inspects/indexes them like a scan would, and
     * returns the new book id. Throws [java.io.IOException] when the stream
     * is missing, too large, or not a readable EPUB.
     */
    suspend fun importEpub(displayName: String, openStream: () -> java.io.InputStream?): String
}

/**
 * Offline-first, link-only repository (Mori's storage model): Room is the
 * source of truth and books are addressed by their SAF document URI — bytes
 * are read in place, never copied into the app. Only cover thumbnails and
 * reading state live on-device.
 */
@Singleton
internal class OfflineFirstBooksRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: IridiumDatabase,
    private val bookDao: BookDao,
    private val highlightDao: HighlightDao,
    private val bookmarkDao: BookmarkDao,
    private val chapterTextDao: ChapterTextDao,
    private val backend: EpubBackend,
    private val covers: EpubCoverGenerator,
    private val treeLister: LinkedTreeLister,
    private val sourceFactory: EpubSourceFactory,
    private val chapterIndexer: ChapterIndexer,
) : BooksRepository {

    /**
     * Library rows, mapped/filtered/sorted off the main thread. Room re-emits
     * on every write, so the transform rides [Dispatchers.Default] and
     * [distinctUntilChanged] drops equal lists before they can recompose.
     */
    override fun observeLibrary(query: LibraryQuery): Flow<List<Book>> =
        bookDao.observeAll()
            .map { entities -> entities.map { it.toModel() }.applyQuery(query) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    override fun observeBook(id: String): Flow<Book?> =
        bookDao.observeById(id)
            .map { it?.toModel() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    override fun observeToc(bookId: String): Flow<List<TocEntry>> =
        bookDao.observeById(bookId).map { it?.tocEntries().orEmpty() }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    override fun observeHighlights(bookId: String): Flow<List<Highlight>> =
        highlightDao.observeForBook(bookId).map { list -> list.map { it.toModel() } }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    override fun observeBookmarks(bookId: String): Flow<List<Bookmark>> =
        bookmarkDao.observeForBook(bookId).map { list -> list.map { it.toModel() } }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    override suspend fun indexFilesystem(
        onProgress: (done: Int, total: Int) -> Unit,
    ): IndexReport = withContext(Dispatchers.IO) {
        val (docs, walkFailed) = treeLister.listBooks()
        // Batch: one table fetch, one upsert, one emission.
        val knownById = bookDao.getAll().associateBy { it.id }
        val rows = mutableListOf<BookEntity>()
        var failed = 0
        docs.forEachIndexed { index, doc ->
            val uri = doc.uri.toString()
            try {
                val known = knownById[uri]
                val row = if (
                    known != null &&
                    known.error == null &&
                    known.sourceModified == doc.modified &&
                    known.coverPath?.let { File(it).isFile } == true
                ) {
                    // Fast path: healthy unchanged file with a live thumbnail.
                    // Error rows always re-inspect so transient failures retry.
                    known
                } else {
                    indexDocument(doc, known)
                }
                rows += row
                if (row.error != null) failed += 1
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed += 1
            }
            onProgress(index + 1, docs.size)
        }
        // Re-read before writing: a reader progress write may have landed
        // after the snapshot above; last-writer-wins with stale rows would
        // eat it.
        val fresh = bookDao.getAll().associateBy { it.id }
        val merged = rows.map { row ->
            val live = fresh[row.id]
            val snap = knownById[row.id]
            if (live != null && (live.updatedAt > (snap?.updatedAt ?: 0L))) {
                row.copy(
                    progress = live.progress,
                    lastLocator = live.lastLocator,
                    updatedAt = live.updatedAt,
                    bookmarked = live.bookmarked,
                )
            } else {
                row
            }
        }
        // One transaction: crash/interleave can never leave book rows
        // without FTS or FTS without books. Cover files delete after
        // commit (file I/O must stay out of the DB transaction).
        val prunedLinked: List<BookEntity> = database.withTransaction {
            if (merged.isNotEmpty()) bookDao.upsertAll(merged)
            pruneLocked(
                foundIdsOf = merged,
                knownById = knownById,
                walkFailed = walkFailed,
            )
        }
        prunedLinked.forEach { deleteCover(it.coverPath) }
        IndexReport(total = docs.size, failed = failed)
    }

    override suspend fun updateProgress(id: String, progress: Float, locator: String?) {
        bookDao.updateProgress(id, progress.coerceIn(0f, 1f), locator, System.currentTimeMillis())
    }

    override suspend fun setBookmarked(id: String, bookmarked: Boolean) {
        bookDao.updateBookmark(id, bookmarked, System.currentTimeMillis())
    }

    override suspend fun upsertHighlight(highlight: Highlight) {
        highlightDao.upsert(highlight.toEntity())
    }

    override suspend fun deleteHighlight(id: String) {
        highlightDao.deleteById(id)
    }

    override suspend fun deleteAllHighlights(bookId: String) {
        highlightDao.deleteForBook(bookId)
    }

    override suspend fun upsertBookmark(bookmark: Bookmark) {
        bookmarkDao.upsert(bookmark.toEntity())
    }

    override suspend fun deleteBookmark(id: String) {
        bookmarkDao.deleteById(id)
    }

    override suspend fun deleteAllBookmarks(bookId: String) {
        bookmarkDao.deleteForBook(bookId)
    }

    /**
     * Prunes rows deleted from the tree out from under us — never on a
     * failed walk, which would read as an empty folder and wipe rows the
     * user still owns. Anything not found is gone, whatever scheme its row
     * used (legacy rows included). FTS follows only linked books: legacy
     * file rows survive in the table (the DAO deletes content:// rows
     * only), so wiping their index would orphan live rows.
     *
     * Must run inside [IridiumDatabase.withTransaction]; returns the pruned
     * linked rows so cover files delete after commit.
     */
    private suspend fun pruneLocked(
        foundIdsOf: List<BookEntity>,
        knownById: Map<String, BookEntity>,
        walkFailed: Boolean,
    ): List<BookEntity> {
        if (walkFailed) return emptyList()
        val foundIds = foundIdsOf.map { it.id }.toSet()
        val pruned = knownById.values.filter { it.id !in foundIds }
        val prunedLinked = pruned.filter { isLinkedSourcePath(it.id) }
        if (foundIdsOf.isEmpty() && pruned.isNotEmpty()) {
            bookDao.deleteAllLinked()
        } else if (pruned.isNotEmpty()) {
            bookDao.deleteMissingLinked(foundIds.toList())
        }
        if (prunedLinked.isNotEmpty()) {
            chapterTextDao.deleteForBooks(prunedLinked.map { it.id })
        }
        return prunedLinked
    }

    override suspend fun indexBookContent(bookId: String): Int = withContext(Dispatchers.IO) {
        val row = bookDao.getById(bookId) ?: return@withContext 0
        val toc = row.tocEntries()
        val chapters = runCatching {
            chapterIndexer.extract(bookId, row.sourcePath, row.sourceDisplayName, toc)
        }.getOrDefault(emptyList())

        // Never wipe a good index for a failed extract (unmounted file,
        // transient parse error): the atomic replace below only runs when
        // there is something to replace with.
        if (chapters.isEmpty()) return@withContext 0
        chapterTextDao.replaceForBook(bookId, chapters)
        chapters.size
    }

    override suspend fun isContentIndexed(bookId: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { chapterTextDao.countForBook(bookId) > 0 }.getOrDefault(false)
        }

    override suspend fun searchContent(query: String, limit: Int): List<ContentHit> =
        withContext(Dispatchers.IO) {
            val expression = FtsQuery.build(query) ?: return@withContext emptyList()
            val rows = runCatching { chapterTextDao.search(expression, limit * SEARCH_FANOUT) }
                .getOrDefault(emptyList())
            if (rows.isEmpty()) return@withContext emptyList()

            val titles = bookDao.getAll().associate { it.id to it.title }
            // Round-robin across books: one hit-heavy book must not starve
            // the rest out of the global limit.
            val byBook = rows.mapNotNull { row ->
                ContentHit(
                    bookId = row.bookId,
                    bookTitle = titles[row.bookId] ?: return@mapNotNull null,
                    href = row.href,
                    chapterTitle = row.title,
                    snippet = FtsQuery.snippet(row.body, query),
                )
            }.groupBy { it.bookId }
            val rounds = ArrayDeque(byBook.values.map { it.toMutableList() })
            val out = ArrayList<ContentHit>(limit.coerceAtLeast(0))
            while (out.size < limit && rounds.isNotEmpty()) {
                val queue = rounds.removeFirst()
                // removeAt(0), not removeFirst(): the latter binds to the
                // JDK 21 SequencedCollection member when compiling against a
                // recent android.jar, then throws NoSuchMethodError on every
                // runtime older than API 35.
                out += queue.removeAt(0)
                if (queue.isNotEmpty()) rounds.addLast(queue)
            }
            out
        }

    override suspend fun removeBook(id: String) = withContext(Dispatchers.IO) {
        // Unlink only: the user's original file must survive removal.
        // Atomic: a crash can never leave annotations/FTS without their book.
        val row = bookDao.getById(id)
        if (row != null) {
            database.withTransaction {
                highlightDao.deleteForBook(id)
                bookmarkDao.deleteForBook(id)
                chapterTextDao.deleteForBook(id)
                bookDao.deleteById(id)
            }
            deleteCover(row.coverPath)
            // Imported copies are app-owned: dropping the row must not orphan
            // the bytes. Linked originals (content://) are never touched.
            deleteImportedFile(row.sourcePath)
        }
        Unit
    }

    /**
     * Imports one shared EPUB into app-private storage (`filesDir/books`).
     * The copy is streamed with a size cap; inspection/indexing reuse the
     * scan path, so imported books behave exactly like scanned ones. Prune
     * queries only touch `content://` rows, so imports survive rescans.
     */
    override suspend fun importEpub(displayName: String, openStream: () -> java.io.InputStream?): String =
        withContext(Dispatchers.IO) {
            val name = sanitizeImportDisplayName(displayName)
            val id = UUID.randomUUID().toString()
            val booksDir = File(context.filesDir, IMPORT_DIR).apply { mkdirs() }
            val dest = File(booksDir, "$id.epub")
            val stream = openStream() ?: throw IOException("Unable to read $name")
            try {
                stream.use { input ->
                    dest.outputStream().use { output ->
                        copyCapped(input, output, MAX_IMPORT_BYTES)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                dest.delete()
                throw e
            }
            val now = System.currentTimeMillis()
            try {
                val inspected = sourceFactory.open(dest.absolutePath, name)
                    .use { source -> backend.inspect(source, name.substringBeforeLast('.')) }
                if (inspected.spineCount == 0 && inspected.chapters.isEmpty()) {
                    throw IOException("Not a readable EPUB: $name")
                }
                val coverPath = covers.generate(inspected.coverBytes, "import-$id")
                bookDao.upsert(
                    BookEntity(
                        id = id,
                        title = inspected.title,
                        author = inspected.author,
                        format = com.iridium.core.model.BookFormat.EPUB.name,
                        spineCount = inspected.spineCount,
                        sourcePath = dest.absolutePath,
                        coverPath = coverPath,
                        progress = 0f,
                        lastLocator = null,
                        sourceDisplayName = name,
                        sourceModified = dest.lastModified(),
                        tocJson = encodeToc(inspected.chapters.map { TocEntry(it.href, it.title) }),
                        error = null,
                        createdAt = now,
                        updatedAt = now,
                        bookmarked = false,
                    ),
                )
                // Best-effort FTS like the scan's follow-up pass.
                runCatching { indexBookContent(id) }
                id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dest.delete()
                throw (e as? IOException ?: IOException("Could not import $name", e))
            }
        }

    /** Deletes an app-owned import copy; never touches linked originals. */
    private fun deleteImportedFile(sourcePath: String) {
        val booksDir = File(context.filesDir, IMPORT_DIR).absolutePath
        val file = File(sourcePath)
        if (file.absolutePath.startsWith(booksDir + File.separator)) {
            runCatching { file.delete() }
        }
    }

    /**
     * Inspects one linked document and builds its row WITHOUT writing, so the
     * caller batches every row into a single upsert. The URI is the stable id,
     * so rescans refresh rows instead of duplicating them, and progress /
     * bookmarks carry over via [existing].
     */
    private suspend fun indexDocument(doc: LinkedDocument, existing: BookEntity?): BookEntity {
        val uri = doc.uri.toString()
        val now = System.currentTimeMillis()
        val coverId = linkedCoverId(uri)
        return try {
            val inspected = inspectDocument(doc)
            val coverPath = covers.generate(inspected.coverBytes, coverId)
                ?: existing?.coverPath?.takeIf { File(it).isFile }
            BookEntity(
                id = uri,
                title = inspected.title,
                author = inspected.author,
                format = com.iridium.core.model.BookFormat.EPUB.name,
                spineCount = inspected.spineCount,
                sourcePath = uri,
                coverPath = coverPath,
                progress = existing?.progress ?: 0f,
                lastLocator = existing?.lastLocator,
                sourceDisplayName = doc.name,
                sourceModified = doc.modified,
                tocJson = encodeToc(inspected.chapters.map { TocEntry(it.href, it.title) }),
                error = if (inspected.spineCount == 0 && inspected.chapters.isEmpty()) {
                    BookError.CORRUPT.name
                } else {
                    null
                },
                createdAt = existing?.createdAt ?: now,
                updatedAt = existing?.updatedAt ?: now,
                bookmarked = existing?.bookmarked ?: false,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Typed failure, not silence: keep the row so the user sees the
            // book, flagged for the detail screen's retry/remove path.
            BookEntity(
                id = uri,
                title = existing?.title ?: doc.name.substringBeforeLast('.'),
                author = existing?.author,
                format = com.iridium.core.model.BookFormat.EPUB.name,
                spineCount = 0,
                sourcePath = uri,
                coverPath = existing?.coverPath,
                progress = existing?.progress ?: 0f,
                lastLocator = existing?.lastLocator,
                sourceDisplayName = doc.name,
                sourceModified = doc.modified,
                tocJson = existing?.tocJson,
                error = BookError.CORRUPT.name,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                bookmarked = existing?.bookmarked ?: false,
            )
        }
    }

    /**
     * Reads metadata/cover/TOC for one document.
     *
     * Prefers the native core when the provider exposes a real file descriptor
     * and the native scan actually understood the book; otherwise falls back to
     * the JVM engine, which handles ZIP64 and every archive the native core
     * declines. Both paths are never-throw.
     */
    private fun inspectDocument(doc: LinkedDocument): InspectedEpub {
        val fallback = doc.name.substringBeforeLast('.')
        if (NativeEpub.isAvailable) {
            sourceFactory.withFileDescriptor(doc.uri.toString()) { fd ->
                NativeEpub.inspectFd(fd, fallback)
            }?.let { return it }
        }
        return sourceFactory.open(doc.uri.toString(), doc.name)
            .use { source -> backend.inspect(source, fallback) }
    }

    private fun deleteCover(coverPath: String?) {
        if (coverPath != null) runCatching { File(coverPath).delete() }
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBooksRepository(
        impl: OfflineFirstBooksRepository,
    ): BooksRepository

    @Binds
    abstract fun bindBookOpener(
        impl: ReadiumOpener,
    ): BookOpener
}

@Module
@InstallIn(SingletonComponent::class)
internal object EpubModule {

    @Provides
    @Singleton
    fun provideEpubEngine(): com.iridium.epub.engine.IridiumEpubEngine =
        com.iridium.epub.engine.IridiumEpubEngine()

    @Provides
    @Singleton
    fun provideEpubBackend(
        engine: com.iridium.epub.engine.IridiumEpubEngine,
    ): EpubBackend = engine

    @Provides
    @Singleton
    fun provideLinkedTreeLister(lister: SafLinkedTreeLister): LinkedTreeLister = lister
}
