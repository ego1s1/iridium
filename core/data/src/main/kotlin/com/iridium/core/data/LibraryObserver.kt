package com.iridium.core.data

import com.iridium.core.common.DefaultDispatcher
import com.iridium.core.database.BookDao
import com.iridium.core.database.BookmarkDao
import com.iridium.core.database.HighlightDao
import com.iridium.core.model.Book
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.TocEntry
import com.iridium.core.model.applyQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Read-only library observation. Room re-emits on every write, so the
 * transforms ride [defaultDispatcher] and [distinctUntilChanged] drops equal
 * lists before they can recompose.
 */
internal class LibraryObserver @Inject constructor(
    private val bookDao: BookDao,
    private val highlightDao: HighlightDao,
    private val bookmarkDao: BookmarkDao,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    fun observeLibrary(query: LibraryQuery): Flow<List<Book>> =
        bookDao.observeAll()
            .map { entities -> entities.map { it.toModel() }.applyQuery(query) }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)

    fun observeBook(id: String): Flow<Book?> =
        bookDao.observeById(id)
            .map { it?.toModel() }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)

    fun observeToc(bookId: String): Flow<List<TocEntry>> =
        bookDao.observeById(bookId).map { it?.tocEntries().orEmpty() }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)

    fun observeHighlights(bookId: String): Flow<List<Highlight>> =
        highlightDao.observeForBook(bookId).map { list -> list.map { it.toModel() } }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)

    fun observeBookmarks(bookId: String): Flow<List<Bookmark>> =
        bookmarkDao.observeForBook(bookId).map { list -> list.map { it.toModel() } }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)
}
