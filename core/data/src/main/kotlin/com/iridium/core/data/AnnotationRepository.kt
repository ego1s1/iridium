package com.iridium.core.data

import com.iridium.core.database.BookmarkDao
import com.iridium.core.database.HighlightDao
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import javax.inject.Inject

/**
 * Highlight + bookmark CRUD. The interface is the seam consumers and tests
 * depend on; the storage-backed implementation stays internal.
 */
interface AnnotationRepository {
    suspend fun upsertHighlight(highlight: Highlight)
    suspend fun deleteHighlight(id: String)
    suspend fun deleteAllHighlights(bookId: String)
    suspend fun upsertBookmark(bookmark: Bookmark)
    suspend fun deleteBookmark(id: String)
    suspend fun deleteAllBookmarks(bookId: String)
}

/** Room-backed [AnnotationRepository]. Thin DAO pass-through; Room owns the threading. */
internal class OfflineFirstAnnotationRepository @Inject constructor(
    private val highlightDao: HighlightDao,
    private val bookmarkDao: BookmarkDao,
) : AnnotationRepository {

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
}
