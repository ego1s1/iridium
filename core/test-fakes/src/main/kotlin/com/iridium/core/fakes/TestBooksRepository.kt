package com.iridium.core.fakes

import android.net.Uri
import com.iridium.core.data.BooksRepository
import com.iridium.core.data.ContentHit
import com.iridium.core.data.IndexReport
import com.iridium.core.model.Book
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.TocEntry
import com.iridium.core.model.applyQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [BooksRepository] for tests. Exposes hooks to push data and
 * inspect writes without a database or content provider.
 */
class TestBooksRepository : BooksRepository {

    private val booksFlow = MutableStateFlow<List<Book>>(emptyList())
    private val tocFlow = MutableStateFlow<Map<String, List<TocEntry>>>(emptyMap())
    private val highlightsFlow = MutableStateFlow<List<Highlight>>(emptyList())
    private val bookmarksFlow = MutableStateFlow<List<Bookmark>>(emptyList())

    var indexResult: IndexReport = IndexReport(total = 0, failed = 0)
    var lastLinkedTree: Uri? = null
    private val progressWrites = mutableListOf<Pair<String, Float>>()

    // Test hooks.

    fun setBooks(books: List<Book>) {
        booksFlow.value = books
    }

    fun setToc(bookId: String, entries: List<TocEntry>) {
        tocFlow.update { it + (bookId to entries) }
    }

    fun setHighlights(highlights: List<Highlight>) {
        highlightsFlow.value = highlights
    }

    fun progressWritesFor(id: String): List<Float> =
        progressWrites.filter { it.first == id }.map { it.second }

    // Interface implementation.

    override fun observeLibrary(query: LibraryQuery): Flow<List<Book>> =
        booksFlow.map { it.applyQuery(query) }

    override fun observeBook(id: String): Flow<Book?> =
        booksFlow.map { list -> list.firstOrNull { it.id == id } }

    override fun observeToc(bookId: String): Flow<List<TocEntry>> =
        tocFlow.map { it[bookId].orEmpty() }

    override fun observeHighlights(bookId: String): Flow<List<Highlight>> =
        highlightsFlow.map { list -> list.filter { it.bookId == bookId } }

    override fun observeBookmarks(bookId: String): Flow<List<Bookmark>> =
        bookmarksFlow.map { list -> list.filter { it.bookId == bookId } }

    override suspend fun indexLinkedTree(
        treeUri: Uri,
        onProgress: (done: Int, total: Int) -> Unit,
    ): IndexReport {
        lastLinkedTree = treeUri
        return indexResult
    }

    override suspend fun updateProgress(id: String, progress: Float, locator: String?) {
        progressWrites += id to progress
        booksFlow.update { list ->
            list.map { if (it.id == id) it.copy(progress = progress, lastLocator = locator) else it }
        }
    }

    override suspend fun setBookmarked(id: String, bookmarked: Boolean) {
        booksFlow.update { list ->
            list.map { if (it.id == id) it.copy(bookmarked = bookmarked) else it }
        }
    }

    override suspend fun upsertHighlight(highlight: Highlight) {
        highlightsFlow.update { it + highlight }
    }

    override suspend fun deleteHighlight(id: String) {
        highlightsFlow.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun upsertBookmark(bookmark: Bookmark) {
        bookmarksFlow.update { it + bookmark }
    }

    override suspend fun deleteBookmark(id: String) {
        bookmarksFlow.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun removeBook(id: String) {
        booksFlow.update { list -> list.filterNot { it.id == id } }
    }

    // Full-text search hooks.

    var indexResultCount: Int = 0
    var searchHits: List<ContentHit> = emptyList()
    var lastSearchQuery: String? = null
    val indexedBooks = mutableListOf<String>()

    override suspend fun indexBookContent(bookId: String): Int {
        indexedBooks += bookId
        return indexResultCount
    }

    override suspend fun isContentIndexed(bookId: String): Boolean = bookId in indexedBooks

    override suspend fun searchContent(query: String, limit: Int): List<ContentHit> {
        lastSearchQuery = query
        return searchHits.take(limit)
    }
}
