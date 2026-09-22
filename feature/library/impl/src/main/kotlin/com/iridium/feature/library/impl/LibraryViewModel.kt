package com.iridium.feature.library.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iridium.core.data.BooksRepository
import com.iridium.core.data.ImportResult
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.Book
import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.continueShelf
import com.iridium.core.model.resumeTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: BooksRepository,
    private val preferences: IridiumPreferencesDataSource,
) : ViewModel() {

    /**
     * Effective query: persisted display options (sort/filter/errors, survive
     * restarts) overlaid with ephemeral search text (restored across process
     * death via [SavedStateHandle], cleared on full restart).
     */
    private val searchText = MutableStateFlow(
        savedStateHandle.get<String>(KEY_QUERY_TEXT).orEmpty(),
    )
    private val query: StateFlow<LibraryQuery> = combine(
        preferences.libraryDisplay,
        searchText,
        LibraryDisplay::toQuery,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryQuery(),
    )
    private val refreshing = MutableStateFlow(false)
    private val filterOpen = MutableStateFlow(false)
    private val searchOpen = MutableStateFlow(false)

    /**
     * Database subscription query: the text field echoes instantly through
     * [query], but the grid re-queries at most once per typing pause instead
     * of once per keystroke. Empty text passes through with no delay.
     */
    private val dbQuery: Flow<LibraryQuery> = combine(
        preferences.libraryDisplay,
        searchText.debounce { text -> if (text.isEmpty()) 0L else SEARCH_DEBOUNCE_MS },
        LibraryDisplay::toQuery,
    )

    /** One-shot messages (import failures). A channel, not state. */
    private val messageChannel = Channel<LibraryMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    /** Shared list subscription feeding both the screen and resume candidate. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val books: StateFlow<List<Book>> = dbQuery
        .flatMapLatest { repository.observeLibrary(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val uiState: StateFlow<LibraryUiState> = combine(
        books,
        query,
        combine(refreshing, filterOpen, searchOpen, ::Chrome),
    ) { books, query, chrome ->
        LibraryUiState.Success(
            books = books,
            query = query,
            refreshing = chrome.refreshing,
            filterOpen = chrome.filterOpen,
            searchOpen = chrome.searchOpen,
            continueReading = books.continueShelf(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState.Loading,
    )

    /** Most recently touched book; backs a resume affordance in the shell. */
    val resumeTarget: StateFlow<Book?> = books
        .map { list -> list.resumeTarget() }
        .distinctUntilChanged { a, b ->
            a?.id == b?.id && a?.progress == b?.progress && a?.error == b?.error
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.SearchTextChanged -> {
                searchText.value = action.text
                savedStateHandle[KEY_QUERY_TEXT] = action.text
            }
            is LibraryAction.SortSelected -> updateDisplay { it.copy(sortOrder = action.sort) }
            is LibraryAction.FilterSelected -> updateDisplay { it.copy(filter = action.filter) }
            is LibraryAction.ToggleHideErrors -> updateDisplay { it.copy(hideErrors = action.hide) }
            LibraryAction.OpenFilter -> filterOpen.value = true
            LibraryAction.CloseFilter -> filterOpen.value = false
            LibraryAction.ToggleSearch -> searchOpen.update { !it }
            LibraryAction.Refresh -> pruneMissing()
            is LibraryAction.ImportSelected -> import(action.uri)
            is LibraryAction.RemoveBook -> remove(action.bookId)
        }
    }

    /** Ephemeral chrome state kept out of the query/data flows. */
    private data class Chrome(
        val refreshing: Boolean,
        val filterOpen: Boolean,
        val searchOpen: Boolean,
    )

    private fun updateDisplay(transform: (LibraryDisplay) -> LibraryDisplay) {
        viewModelScope.launch {
            preferences.updateLibraryDisplay(transform)
        }
    }

    private fun import(uri: android.net.Uri) {
        viewModelScope.launch {
            refreshing.value = true
            try {
                when (repository.importEpub(uri)) {
                    is ImportResult.Imported,
                    is ImportResult.AlreadyInLibrary,
                    -> {
                        preferences.setOnboardingCompleted(true)
                    }
                    is ImportResult.Failed -> messageChannel.send(LibraryMessage.ImportFailed)
                }
            } finally {
                refreshing.value = false
            }
        }
    }

    private fun remove(bookId: String) {
        viewModelScope.launch {
            repository.removeBook(bookId)
        }
    }

    /**
     * Lightweight refresh: drops rows whose private files vanished (restores,
     * failed writes). No folder rescan — books are imported file-by-file.
     */
    private fun pruneMissing() {
        viewModelScope.launch {
            refreshing.value = true
            try {
                val current = books.value
                for (book in current) {
                    if (!java.io.File(book.sourcePath).exists()) {
                        repository.removeBook(book.id)
                    }
                }
            } finally {
                refreshing.value = false
            }
        }
    }

    private companion object {
        const val KEY_QUERY_TEXT = "iridium_query_text"
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
