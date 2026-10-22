package com.iridium.core.data

import com.iridium.core.database.BookmarkDao
import com.iridium.core.database.HighlightDao
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import javax.inject.Inject

/** Highlight + bookmark CRUD. Thin DAO pass-through; Room owns the threading. */
internal class AnnotationRepository @Inject constructor(
    private val highlightDao: HighlightDao,
    private val bookmarkDao: BookmarkDao,
) {

    suspend fun upsertHighlight(highlight: Highlight) {
        highlightDao.upsert(highlight.toEntity())
    }

    suspend fun deleteHighlight(id: String) {
        highlightDao.deleteById(id)
    }

    suspend fun deleteAllHighlights(bookId: String) {
        highlightDao.deleteForBook(bookId)
    }

    suspend fun upsertBookmark(bookmark: Bookmark) {
        bookmarkDao.upsert(bookmark.toEntity())
    }

    suspend fun deleteBookmark(id: String) {
        bookmarkDao.deleteById(id)
    }

    suspend fun deleteAllBookmarks(bookId: String) {
        bookmarkDao.deleteForBook(bookId)
    }
}
