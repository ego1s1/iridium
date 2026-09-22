package com.iridium.feature.library.impl

import android.net.Uri
import com.iridium.core.model.Book
import com.iridium.core.model.LibraryFilter
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.LibrarySortOrder

/**
 * No blocking Loading state: Room is local and emits its first list almost
 * immediately, so the grid renders straight from an empty list and fills in
 * as rows arrive. Scanning surfaces as a thin progress bar, never a spinner
 * that hides content the user already has.
 */
data class LibraryUiState(
    val books: List<Book>,
    val query: LibraryQuery,
    val refreshing: Boolean,
    val filterOpen: Boolean,
    val searchOpen: Boolean,
    /** True once a source folder has been linked. */
    val linked: Boolean,
    /** In-progress books by recency, backing the continue shelf. */
    val continueReading: List<Book>,
    /** Determinate scan progress (done/total); null when idle. */
    val indexProgress: IndexProgress? = null,
) {
    val isEmpty: Boolean get() = books.isEmpty()
}

/** Determinate scan progress forwarded from the index callback. */
data class IndexProgress(val done: Int, val total: Int)

sealed interface LibraryAction {
    data class SearchTextChanged(val text: String) : LibraryAction
    data class SortSelected(val sort: LibrarySortOrder) : LibraryAction
    data class FilterSelected(val filter: LibraryFilter) : LibraryAction
    data class ToggleHideErrors(val hide: Boolean) : LibraryAction
    data object OpenFilter : LibraryAction
    data object CloseFilter : LibraryAction
    data object ToggleSearch : LibraryAction
    data object Rescan : LibraryAction

    /** A SAF folder picked for linking; its EPUBs are indexed in place. */
    data class LinkFolder(val uri: Uri) : LibraryAction

    /** Unlinks a book (the user's original file is never touched). */
    data class RemoveBook(val bookId: String) : LibraryAction
}

/** One-shot library messages; the UI maps each to localized copy. */
sealed interface LibraryMessage {
    data class IndexFailed(val failed: Int) : LibraryMessage
    data object ScanFailed : LibraryMessage
}
