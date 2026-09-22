package com.iridium.feature.detail.impl

import com.iridium.core.model.Book
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.TocEntry

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(
        val book: Book,
        val toc: List<TocEntry>,
        val highlights: List<Highlight>,
        val bookmarks: List<Bookmark>,
        val confirmRemove: Boolean = false,
    ) : DetailUiState
    data object Gone : DetailUiState
}

sealed interface DetailAction {
    data class ToggleBookmark(val bookmarked: Boolean) : DetailAction
    data object AskRemove : DetailAction
    data object DismissRemove : DetailAction
    data object ConfirmRemove : DetailAction
    data class DeleteHighlight(val id: String) : DetailAction
    data class DeleteBookmark(val id: String) : DetailAction
}
