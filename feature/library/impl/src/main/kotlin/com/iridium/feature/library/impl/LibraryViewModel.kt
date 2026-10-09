package com.iridium.feature.library.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iridium.core.data.BooksRepository
import com.iridium.core.data.ContentHit
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.Book
import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.continueShelf
import com.iridium.core.model.resumeTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
    private val menuBook = MutableStateFlow<Book?>(null)
    private val menuDeleteConfirm = MutableStateFlow(false)

    private val refreshing = MutableStateFlow(false)
    private val filterOpen = MutableStateFlow(false)

    /**
     * Serializes scans: a folder pick is never dropped behind a running scan,
     * and rapid rescan taps queue instead of overlapping index writes. The
     * counter keeps the progress bar up across queued runs.
     */
    private val scanMutex = Mutex()

    private val scanPending = AtomicInteger(0)
    private val scanQueued = AtomicBoolean(false)
    private val indexProgress = MutableStateFlow<IndexProgress?>(null)
    private val contentHits = MutableStateFlow<List<ContentHit>>(emptyList())

    /**
     * Database subscription query: the text field echoes instantly through
     * [query], but the grid re-queries at most once per typing pause. Empty
     * text passes through with no delay.
     */
    private val dbQuery: Flow<LibraryQuery> = combine(
        preferences.libraryDisplay,
        searchText.debounce { text -> if (text.isEmpty()) 0L else SEARCH_DEBOUNCE_MS },
        LibraryDisplay::toQuery,
    )

    /** One-shot messages (scan failures). A channel, not state. */
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
        combine(refreshing, filterOpen, ::Chrome),
        indexProgress,
        contentHits,
        preferences.libraryDisplay,
        combine(menuBook, menuDeleteConfirm, ::MenuChrome),
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val books = args[0] as List<Book>
        val query = args[1] as LibraryQuery
        val chrome = args[2] as Chrome
        val progress = args[3] as IndexProgress?
        val hits = args[4] as List<ContentHit>
        val display = args[5] as com.iridium.core.model.LibraryDisplay
        val menu = args[6] as MenuChrome
        LibraryUiState(
            books = books,
            query = query,
            refreshing = chrome.refreshing,
            filterOpen = chrome.filterOpen,
            continueReading = books.continueShelf(),
            indexProgress = progress,
            contentHits = hits,
            display = display,
            menuBook = menu.book,
            menuDeleteConfirm = menu.deleteConfirm,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(
            books = emptyList(),
            query = LibraryQuery(),
            refreshing = false,
            filterOpen = false,
            continueReading = emptyList(),
        ),
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

    /** SAF folders linked for scans; empty means the grant UI shows. */
    val linkedFolders: StateFlow<Set<String>> = preferences.linkedFolders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet(),
        )

    init {
        // Rescan on launch: the library reads every EPUB on shared storage
        // in place, so a launch pass picks up files added, moved or removed
        // outside the app.
        viewModelScope.launch { scan() }
        // Content search rides the same typed text but its own debounce: FTS
        // is cheap per query, yet we still avoid a query per keystroke.
        viewModelScope.launch {
            searchText
                .debounce { text -> if (text.length < MIN_CONTENT_QUERY) 0L else CONTENT_DEBOUNCE_MS }
                .distinctUntilChanged()
                .collectLatest { text ->
                    contentHits.value = if (text.length < MIN_CONTENT_QUERY) {
                        emptyList()
                    } else {
                        runCatching { repository.searchContent(text) }.getOrDefault(emptyList())
                    }
                }
        }
    }

    fun onAction(action: LibraryAction) {
        when (action) {
            is LibraryAction.SearchTextChanged -> {
                searchText.value = action.text
                savedStateHandle[KEY_QUERY_TEXT] = action.text
            }
            is LibraryAction.SortSelected -> updateDisplay { it.copy(sortOrder = action.sort) }
            is LibraryAction.FilterSelected -> updateDisplay { it.copy(filter = action.filter) }
            is LibraryAction.ToggleHideErrors -> updateDisplay { it.copy(hideErrors = action.hide) }
            is LibraryAction.DisplayModeSelected -> updateDisplay { it.copy(displayMode = action.mode) }
            is LibraryAction.GridColumnsSelected -> updateDisplay {
                it.copy(gridColumns = action.columns.coerceIn(0, 6))
            }
            LibraryAction.OpenFilter -> filterOpen.value = true
            LibraryAction.CloseFilter -> filterOpen.value = false
            LibraryAction.Rescan -> scan()
            is LibraryAction.AddLinkedFolder -> viewModelScope.launch {
                preferences.addLinkedFolder(action.uri)
            }
            is LibraryAction.RemoveBook -> remove(action.bookId)
            is LibraryAction.OpenMenu -> {
                menuBook.value = books.value.firstOrNull { it.id == action.bookId }
                menuDeleteConfirm.value = false
            }
            LibraryAction.CloseMenu -> {
                menuBook.value = null
                menuDeleteConfirm.value = false
            }
            LibraryAction.ToggleMenuBookmark -> {
                val id = menuBook.value?.id ?: return
                val current = books.value.firstOrNull { it.id == id }?.bookmarked ?: return
                viewModelScope.launch { repository.setBookmarked(id, !current) }
            }
            LibraryAction.OpenMenuDelete -> menuDeleteConfirm.value = true
            LibraryAction.ConfirmMenuDelete -> {
                val id = menuBook.value?.id ?: return
                menuDeleteConfirm.value = false
                menuBook.value = null
                remove(id)
            }
        }
    }


    /** Ephemeral chrome state kept out of the query/data flows. */
    private data class Chrome(
        val refreshing: Boolean,
        val filterOpen: Boolean,
    )

    /** Ephemeral menu chrome kept out of the query/data flows. */
    private data class MenuChrome(
        val book: Book?,
        val deleteConfirm: Boolean,
    )

    private fun updateDisplay(transform: (LibraryDisplay) -> LibraryDisplay) {
        viewModelScope.launch { preferences.updateLibraryDisplay(transform) }
    }

    private fun remove(bookId: String) {
        viewModelScope.launch { repository.removeBook(bookId) }
    }

    /**
     * Filesystem scan: re-indexes every EPUB on shared storage in place —
     * nothing is ever copied. A scan during a running scan folds into at
     * most one follow-up run.
     */
    private fun scan() {
        viewModelScope.launch {
            // No linked folders, no scan: running the repository with an
            // empty folder set would read as an empty device and prune the
            // whole library.
            if (preferences.linkedFolders.first().isEmpty()) return@launch
            if (scanMutex.isLocked) {
                scanQueued.set(true)
                return@launch
            }
            scanPending.incrementAndGet()
            refreshing.value = true
            try {
                do {
                    scanQueued.set(false)
                    scanMutex.withLock {
                        try {
                            val report = repository.indexFilesystem { done, total ->
                                indexProgress.value = IndexProgress(done, total)
                            }
                            if (report.failed > 0) {
                                messageChannel.send(LibraryMessage.IndexFailed(report.failed))
                            }
                        } catch (_: Exception) {
                            messageChannel.send(LibraryMessage.ScanFailed)
                        }
                    }
                } while (scanQueued.getAndSet(false))
            } finally {
                if (scanPending.decrementAndGet() == 0) {
                    refreshing.value = false
                    indexProgress.value = null
                }
            }
        }
    }


    private companion object {
        const val KEY_QUERY_TEXT = "iridium_query_text"
        const val SEARCH_DEBOUNCE_MS = 250L
        const val CONTENT_DEBOUNCE_MS = 300L

        /** Below this, content search would match almost everything. */
        const val MIN_CONTENT_QUERY = 2
    }
}
