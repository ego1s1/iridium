package com.iridium.feature.library.impl

import android.net.Uri
import com.iridium.core.model.Book
import com.iridium.core.model.LibraryFilter
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.LibrarySortOrder

sealed interface LibraryUiState {
    data object Loading : LibraryUiState

    data class Success(
        val books: List<Book>,
        val query: LibraryQuery,
        val refreshing: Boolean,
        val filterOpen: Boolean,
        val searchOpen: Boolean,
        /** In-progress books by recency, backing the continue shelf. */
        val continueReading: List<Book>,
    ) : LibraryUiState {
        val isEmpty: Boolean get() = books.isEmpty()
    }
}

sealed interface LibraryAction {
    data class SearchTextChanged(val text: String) : LibraryAction
    data class SortSelected(val sort: LibrarySortOrder) : LibraryAction
    data class FilterSelected(val filter: LibraryFilter) : LibraryAction
    data class ToggleHideErrors(val hide: Boolean) : LibraryAction
    data object OpenFilter : LibraryAction
    data object CloseFilter : LibraryAction
    data object ToggleSearch : LibraryAction
    data object Refresh : LibraryAction

    /** An EPUB picked from the system picker; copied + parsed by the repo. */
    data class ImportSelected(val uri: Uri) : LibraryAction

    data class RemoveBook(val bookId: String) : LibraryAction
}

/** One-shot library messages; the UI maps each to localized copy. */
sealed interface LibraryMessage {
    data object ImportFailed : LibraryMessage
}
