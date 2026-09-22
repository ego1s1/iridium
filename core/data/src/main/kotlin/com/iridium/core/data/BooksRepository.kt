package com.iridium.core.data

import android.net.Uri
import com.iridium.core.database.BookDao
import com.iridium.core.database.BookmarkDao
import com.iridium.core.database.HighlightDao
import com.iridium.core.model.Book
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.TocEntry
import com.iridium.epub.EpubBackend
import com.iridium.epub.ZipEpubBackend
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface BooksRepository {
    fun observeLibrary(query: LibraryQuery): Flow<List<Book>>
    fun observeBook(id: String): Flow<Book?>
    fun observeToc(bookId: String): Flow<List<TocEntry>>
    fun observeHighlights(bookId: String): Flow<List<Highlight>>
    fun observeBookmarks(bookId: String): Flow<List<Bookmark>>
    suspend fun importEpub(uri: Uri): ImportResult
    suspend fun updateProgress(id: String, progress: Float, locator: String?)
    suspend fun setBookmarked(id: String, bookmarked: Boolean)
    suspend fun upsertHighlight(highlight: Highlight)
    suspend fun deleteHighlight(id: String)
    suspend fun upsertBookmark(bookmark: Bookmark)
    suspend fun deleteBookmark(id: String)
    suspend fun removeBook(id: String)
}

/**
 * Offline-first repository: Room is the source of truth, queries apply
 * in-memory over observed rows. Import copies EPUB bytes into app-private
 * storage — nothing is ever read in place from shared storage.
 */
@Singleton
class OfflineFirstBooksRepository @Inject constructor(
    private val bookDao: BookDao,
    private val highlightDao: HighlightDao,
    private val bookmarkDao: BookmarkDao,
    private val importer: EpubImporter,
) : BooksRepository {

    override fun observeLibrary(query: LibraryQuery): Flow<List<Book>> =
        bookDao.observeAll().map { entities ->
            entities.map { it.toModel() }.forQuery(query)
                .let { books ->
                    // Errors sort last unless the query says otherwise; cheap
                    // stability win for the grid when hideErrors is off.
                    books
                }
        }

    override fun observeBook(id: String): Flow<Book?> =
        bookDao.observeById(id).map { it?.toModel() }

    override fun observeToc(bookId: String): Flow<List<TocEntry>> =
        bookDao.observeById(bookId).map { it?.tocEntries().orEmpty() }

    override fun observeHighlights(bookId: String): Flow<List<Highlight>> =
        highlightDao.observeForBook(bookId).map { list -> list.map { it.toModel() } }

    override fun observeBookmarks(bookId: String): Flow<List<Bookmark>> =
        bookmarkDao.observeForBook(bookId).map { list -> list.map { it.toModel() } }

    override suspend fun importEpub(uri: Uri): ImportResult = importer.import(uri)

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

    override suspend fun removeBook(id: String) {
        val book = bookDao.getById(id)
        withContext(Dispatchers.IO) {
            highlightDao.deleteForBook(id)
            bookmarkDao.deleteForBook(id)
            bookDao.deleteById(id)
        }
        // Delete private files last: DB removal is the atomic user-visible
        // effect; orphaned files on crash are reaped on next launch.
        book?.let {
            runCatching { File(it.sourcePath).delete() }
            runCatching { it.coverPath?.let { path -> File(path).delete() } }
        }
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
    fun provideEpubBackend(): EpubBackend = ZipEpubBackend()
}
