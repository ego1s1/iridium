package com.iridium.feature.library.impl

import com.iridium.core.data.ContentHit
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
    /** In-progress books by recency, backing the continue shelf. */
    val continueReading: List<Book>,
    /** Determinate scan progress (done/total); null when idle. */
    val indexProgress: IndexProgress? = null,
    /** Full-text matches for the current query, newest-first. */
    val contentHits: List<ContentHit> = emptyList(),
    /** True while chapter text is being indexed for search. */
    val indexing: Boolean = false,
    /** Persisted display options (card density, grid width). */
    val display: com.iridium.core.model.LibraryDisplay =
        com.iridium.core.model.LibraryDisplay(),
    /** Open quick-actions sheet's book (retained, not re-looked-up, so
     * filtering while the sheet is open cannot dismiss it); null when closed. */
    val menuBook: Book? = null,
    /** True while the menu's remove-confirm dialog is showing. */
    val menuDeleteConfirm: Boolean = false,
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
    data class DisplayModeSelected(val mode: com.iridium.core.model.LibraryDisplayMode) : LibraryAction
    data class GridColumnsSelected(val columns: Int) : LibraryAction
    data object Rescan : LibraryAction

    /** Links one SAF folder for library scans. */
    data class AddLinkedFolder(val uri: String) : LibraryAction

    /** Extracts chapter text for every book so content search has data. */
    data object IndexLibrary : LibraryAction

    /** Unlinks a book (the user's original file is never touched). */
    data class RemoveBook(val bookId: String) : LibraryAction

    /** Opens the long-press quick-actions sheet for a book. */
    data class OpenMenu(val bookId: String) : LibraryAction

    /** Closes the quick-actions sheet and any confirm dialog. */
    data object CloseMenu : LibraryAction

    /** Toggles the bookmark flag of the book in the open menu. */
    data object ToggleMenuBookmark : LibraryAction

    /** Shows the remove-confirm dialog inside the open menu. */
    data object OpenMenuDelete : LibraryAction

    /** Confirms removal of the book in the open menu. */
    data object ConfirmMenuDelete : LibraryAction
}

/** One-shot library messages; the UI maps each to localized copy. */
sealed interface LibraryMessage {
    data class IndexFailed(val failed: Int) : LibraryMessage
    data object ScanFailed : LibraryMessage
    data class IndexedForSearch(val chapters: Int) : LibraryMessage
}
