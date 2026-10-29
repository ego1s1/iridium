package com.iridium.core.data

import com.iridium.core.common.safeCatching
import com.iridium.core.model.Book
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.TocEntry
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

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

    /**
     * Scans all of shared storage in place and indexes every EPUB found.
     * Rows whose files are gone are pruned (never on a failed walk).
     */
    suspend fun indexFilesystem(
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): IndexReport

    suspend fun updateProgress(id: String, progress: Float, locator: String?)
    suspend fun setBookmarked(id: String, bookmarked: Boolean)
    suspend fun toggleBookmarked(id: String)
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
 * Offline-first, link-only repository: Room is the source of truth and books
 * are addressed by their SAF document URI — bytes are read in place, never
 * copied into the app. Only cover thumbnails and reading state live on-device.
 */
@Singleton
// 14 params: 12 collaborators + 2 injected dispatchers. The 1.2 split
// (LibraryObserver / ScanUseCase / SearchUseCase / ...) removes this;
// until then the suppression keeps the active LongParameterList rule green.
internal class OfflineFirstBooksRepository @Inject constructor(
    private val observer: LibraryObserver,
    private val search: ContentSearchUseCase,
    private val annotations: AnnotationRepository,
    private val management: BookManagementUseCase,
    private val scanner: LibraryScanUseCase,
) : BooksRepository {

    /** Library rows with the query applied; see [LibraryObserver]. */
    override fun observeLibrary(query: LibraryQuery): Flow<List<Book>> =
        observer.observeLibrary(query)

    override fun observeBook(id: String): Flow<Book?> =
        observer.observeBook(id)

    override fun observeToc(bookId: String): Flow<List<TocEntry>> =
        observer.observeToc(bookId)

    override fun observeHighlights(bookId: String): Flow<List<Highlight>> =
        observer.observeHighlights(bookId)

    override fun observeBookmarks(bookId: String): Flow<List<Bookmark>> =
        observer.observeBookmarks(bookId)

    override suspend fun indexFilesystem(
        onProgress: (done: Int, total: Int) -> Unit,
    ): IndexReport = scanner.indexFilesystem(onProgress)

    override suspend fun updateProgress(id: String, progress: Float, locator: String?) {
        management.updateProgress(id, progress, locator)
    }

    override suspend fun setBookmarked(id: String, bookmarked: Boolean) {
        management.setBookmarked(id, bookmarked)
    }

    override suspend fun toggleBookmarked(id: String) {
        management.toggleBookmarked(id)
    }

    override suspend fun upsertHighlight(highlight: Highlight) =
        annotations.upsertHighlight(highlight)

    override suspend fun deleteHighlight(id: String) =
        annotations.deleteHighlight(id)

    override suspend fun deleteAllHighlights(bookId: String) =
        annotations.deleteAllHighlights(bookId)

    override suspend fun upsertBookmark(bookmark: Bookmark) =
        annotations.upsertBookmark(bookmark)

    override suspend fun deleteBookmark(id: String) =
        annotations.deleteBookmark(id)

    override suspend fun deleteAllBookmarks(bookId: String) =
        annotations.deleteAllBookmarks(bookId)

    override suspend fun indexBookContent(bookId: String): Int =
        search.indexBookContent(bookId)

    override suspend fun isContentIndexed(bookId: String): Boolean =
        search.isContentIndexed(bookId)

    override suspend fun searchContent(query: String, limit: Int): List<ContentHit> =
        search.searchContent(query, limit)

    override suspend fun removeBook(id: String) =
        management.removeBook(id)

    override suspend fun importEpub(displayName: String, openStream: () -> java.io.InputStream?): String =
        scanner.importEpub(displayName, openStream).also { id ->
            // Best-effort FTS like the scan's follow-up pass.
            safeCatching { search.indexBookContent(id) }
        }

}
