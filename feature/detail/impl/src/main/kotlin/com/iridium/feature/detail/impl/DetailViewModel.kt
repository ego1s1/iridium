package com.iridium.feature.detail.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iridium.core.data.BooksRepository
import com.iridium.core.model.TocEntry
import com.iridium.feature.detail.api.DetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
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
            // Parameterless: the ViewModel inverts the latest flow value, so
            // rapid double-taps cannot act on a stale composition snapshot.
            DetailAction.ToggleBookmark -> setBookmarkedToggled()
            DetailAction.AskRemove -> confirmRemove.value = true
            DetailAction.DismissRemove -> confirmRemove.value = false
            DetailAction.ConfirmRemove -> remove()
            is DetailAction.DeleteHighlight -> deleteHighlight(action.id)
            is DetailAction.DeleteBookmark -> deleteBookmark(action.id)
        }
    }

    private fun setBookmarkedToggled() {
        viewModelScope.launch {
            val current = repository.observeBook(route.bookId).first()?.bookmarked ?: return@launch
            repository.setBookmarked(route.bookId, !current)
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

/**
 * TOC indices whose chapter resource sits fully before the reader's current
 * position. Anchored on [Book.lastLocator]'s href (the only per-chapter signal
 * persisted to the detail layer) against TOC order, which follows the nav
 * document's reading order.
 *
 * Granularity is one spine resource: TOC rows sharing the current file stay
 * unmarked because intra-file progress is invisible without the reader's
 * positions table. Linear reading is assumed — a slider jump over unread
 * chapters overstates completion. A finished book marks every row.
 */
internal fun completedTocIndices(
    toc: List<TocEntry>,
    lastLocator: String?,
    isFinished: Boolean,
): Set<Int> {
    if (toc.isEmpty()) return emptySet()
    if (isFinished) return toc.indices.toSet()
    val current = locatorResourceKey(lastLocator) ?: return emptySet()
    val firstCurrent = toc.indexOfFirst { resourceKey(it.href) == current }
    if (firstCurrent < 0) return emptySet()
    return (0 until firstCurrent).toSet()
}

/** Resource href out of a persisted Readium locator JSON, or null when absent/unparseable. */
internal fun locatorResourceKey(rawLocator: String?): String? {
    if (rawLocator.isNullOrBlank()) return null
    val href = runCatching { JSONObject(rawLocator).optString("href").ifBlank { null } }
        .getOrNull() ?: return null
    return resourceKey(href)
}

/**
 * Filename-level href key. Stored TOC hrefs are archive paths
 * (`OEBPS/ch1.xhtml`) while Readium emits manifest-relative hrefs, so the
 * comparison mirrors the reader's filename fallback.
 */
internal fun resourceKey(href: String): String =
    href.substringAfterLast('/').substringBefore('#')
