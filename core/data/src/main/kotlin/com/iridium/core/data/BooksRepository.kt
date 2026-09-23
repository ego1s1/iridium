package com.iridium.core.data

import android.content.Context
import android.net.Uri
import com.iridium.core.database.BookDao
import com.iridium.core.database.BookEntity
import com.iridium.core.database.BookmarkDao
import com.iridium.core.database.ChapterTextDao
import com.iridium.core.database.HighlightDao
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

    /** Links a user folder in place and indexes every EPUB under it. */
    suspend fun indexLinkedTree(
        treeUri: Uri,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): IndexReport

    suspend fun updateProgress(id: String, progress: Float, locator: String?)
    suspend fun setBookmarked(id: String, bookmarked: Boolean)
    suspend fun upsertHighlight(highlight: Highlight)
    suspend fun deleteHighlight(id: String)
    suspend fun upsertBookmark(bookmark: Bookmark)
    suspend fun deleteBookmark(id: String)

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
}

/**
 * Offline-first, link-only repository (Mori's storage model): Room is the
 * source of truth and books are addressed by their SAF document URI — bytes
 * are read in place, never copied into the app. Only cover thumbnails and
 * reading state live on-device.
 */
@Singleton
internal class OfflineFirstBooksRepository @Inject constructor(
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
        bookDao.observeById(bookId).map { it?.tocEntries().orEmpty() }.flowOn(Dispatchers.Default)

    override fun observeHighlights(bookId: String): Flow<List<Highlight>> =
        highlightDao.observeForBook(bookId).map { list -> list.map { it.toModel() } }

    override fun observeBookmarks(bookId: String): Flow<List<Bookmark>> =
        bookmarkDao.observeForBook(bookId).map { list -> list.map { it.toModel() } }

    override suspend fun indexLinkedTree(
        treeUri: Uri,
        onProgress: (done: Int, total: Int) -> Unit,
    ): IndexReport = withContext(Dispatchers.IO) {
        val (docs, walkFailed) = treeLister.listBooks(treeUri)
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
                    known.sourceModified == doc.modified &&
                    known.coverPath?.let { File(it).isFile } == true
                ) {
                    // Fast path: unchanged file with a live thumbnail.
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
        if (rows.isNotEmpty()) bookDao.upsertAll(rows)
        // Prune rows deleted from the tree out from under us — never on a
        // failed walk, which would read as an empty folder and wipe rows the
        // user still owns. Pruned covers go with their rows.
        if (!walkFailed) {
            val foundIds = rows.map { it.id }.toSet()
            val pruned = knownById.values.filter { isLinkedSourcePath(it.sourcePath) && it.id !in foundIds }
            if (rows.isEmpty() && pruned.isNotEmpty()) {
                bookDao.deleteAllLinked()
            } else if (pruned.isNotEmpty()) {
                bookDao.deleteMissingLinked(foundIds.toList())
            }
            pruned.forEach { deleteCover(it.coverPath) }
        }
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

    override suspend fun upsertBookmark(bookmark: Bookmark) {
        bookmarkDao.upsert(bookmark.toEntity())
    }

    override suspend fun deleteBookmark(id: String) {
        bookmarkDao.deleteById(id)
    }

    override suspend fun indexBookContent(bookId: String): Int = withContext(Dispatchers.IO) {
        val row = bookDao.getById(bookId) ?: return@withContext 0
        val toc = row.tocEntries()
        val chapters = runCatching {
            chapterIndexer.extract(row.sourcePath, row.sourceDisplayName, toc)
        }.getOrDefault(emptyList())

        // Replace atomically enough: a failure mid-insert leaves the previous
        // index in place rather than a half-built one.
        chapterTextDao.deleteForBook(bookId)
        if (chapters.isNotEmpty()) chapterTextDao.insertAll(chapters)
        chapters.size
    }

    override suspend fun isContentIndexed(bookId: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { chapterTextDao.countForBook(bookId) > 0 }.getOrDefault(false)
        }

    override suspend fun searchContent(query: String, limit: Int): List<ContentHit> =
        withContext(Dispatchers.IO) {
            val expression = FtsQuery.build(query) ?: return@withContext emptyList()
            val rows = runCatching { chapterTextDao.search(expression, limit) }
                .getOrDefault(emptyList())
            if (rows.isEmpty()) return@withContext emptyList()

            val titles = bookDao.getAll().associate { it.id to it.title }
            rows.mapNotNull { row ->
                ContentHit(
                    bookId = row.bookId,
                    bookTitle = titles[row.bookId] ?: return@mapNotNull null,
                    href = row.href,
                    chapterTitle = row.title,
                    snippet = FtsQuery.snippet(row.body, query),
                )
            }
        }

    override suspend fun removeBook(id: String) = withContext(Dispatchers.IO) {
        // Unlink only: the user's original file must survive removal.
        val row = bookDao.getById(id)
        if (row != null) {
            highlightDao.deleteForBook(id)
            bookmarkDao.deleteForBook(id)
            chapterTextDao.deleteForBook(id)
            bookDao.deleteById(id)
            deleteCover(row.coverPath)
        }
        Unit
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
    fun provideLinkedTreeLister(
        @ApplicationContext context: Context,
    ): LinkedTreeLister = DocumentLinkedTreeLister(context)
}
