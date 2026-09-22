package com.iridium.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iridium.core.data.BooksRepository
import com.iridium.feature.detail.api.DetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BooksRepository,
) : ViewModel() {

    private val route: DetailRoute = savedStateHandle.toRoute()
    private val confirmRemove = MutableStateFlow(false)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeBook(route.bookId),
        repository.observeToc(route.bookId),
        repository.observeHighlights(route.bookId),
        repository.observeBookmarks(route.bookId),
        confirmRemove,
        ::toUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState.Loading,
    )

    private fun toUiState(
        book: com.iridium.core.model.Book?,
        toc: List<com.iridium.core.model.TocEntry>,
        highlights: List<com.iridium.core.model.Highlight>,
        bookmarks: List<com.iridium.core.model.Bookmark>,
        confirm: Boolean,
    ): DetailUiState {
        if (book == null) return DetailUiState.Gone
        return DetailUiState.Success(book, toc, highlights, bookmarks, confirm)
    }

    fun onAction(action: DetailAction) {
        when (action) {
            is DetailAction.ToggleBookmark -> setBookmarked(action.bookmarked)
            DetailAction.AskRemove -> confirmRemove.value = true
            DetailAction.DismissRemove -> confirmRemove.value = false
            DetailAction.ConfirmRemove -> remove()
            is DetailAction.DeleteHighlight -> deleteHighlight(action.id)
            is DetailAction.DeleteBookmark -> deleteBookmark(action.id)
        }
    }

    private fun setBookmarked(bookmarked: Boolean) {
        viewModelScope.launch {
            repository.setBookmarked(route.bookId, bookmarked)
        }
    }

    private fun remove() {
        viewModelScope.launch {
            repository.removeBook(route.bookId)
        }
    }

    private fun deleteHighlight(id: String) {
        viewModelScope.launch {
            repository.deleteHighlight(id)
        }
    }

    private fun deleteBookmark(id: String) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
        }
    }
}
