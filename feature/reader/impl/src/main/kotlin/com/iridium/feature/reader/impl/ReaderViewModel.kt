package com.iridium.feature.reader.impl

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iridium.core.data.BooksRepository
import com.iridium.core.data.OpenResult
import com.iridium.core.data.ReadiumOpener
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.Book
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.HighlightColor
import com.iridium.core.model.TocEntry
import com.iridium.feature.reader.api.ReaderRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import java.util.UUID

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BooksRepository,
    private val preferences: IridiumPreferencesDataSource,
    private val opener: ReadiumOpener,
    private val store: ReaderSessionStore,
) : ViewModel() {

    private val route: ReaderRoute = savedStateHandle.toRoute()
    private val bookId: String = route.bookId

    private val chromeVisible = MutableStateFlow(true)
    private val settingsOpen = MutableStateFlow(false)
    private val tocOpen = MutableStateFlow(false)
    private val highlightsOpen = MutableStateFlow(false)
    private val focusedHighlightId = MutableStateFlow<String?>(null)
    private val navigatorAttached = MutableStateFlow(false)
    private val openFailed = MutableStateFlow(false)
    private var chromeJob: Job? = null
    /** Publication positions for the slider/position text (no fixed pages). */
    private var positionsCache: List<Locator> = emptyList()

    private val messageChannel = Channel<ReaderMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    /**
     * True once the publication is published and the navigator host may be
     * attached. Opening is asynchronous, so the UI waits on this instead of
     * composing the host during a window where there is no factory yet.
     */
    val sessionReady: StateFlow<Boolean> = store.sessionReady

    private val book: Flow<Book?> = repository.observeBook(bookId)
    private val toc: Flow<List<TocEntry>> = repository.observeToc(bookId)
    private val highlights: Flow<List<Highlight>> = repository.observeHighlights(bookId)

    /** Debounced locator → persisted progress (never per-frame writes). */
    private val progression: StateFlow<Float> = store.latestLocator
        .debounce(LOCATOR_SAVE_DEBOUNCE_MS)
        .map { it?.locations?.totalProgression?.toFloat() ?: 0f }
        .distinctUntilChanged { a, b -> kotlin.math.abs(a - b) < 0.0005f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val uiState: StateFlow<ReaderUiState> = combine(
        book, toc, highlights, preferences.readerPreferences, chromeVisible, settingsOpen, tocOpen,
        highlightsOpen, focusedHighlightId, navigatorAttached, openFailed, progression,
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val b = args[0] as Book?
        val t = args[1] as List<TocEntry>
        val h = args[2] as List<Highlight>
        val p = args[3] as com.iridium.core.model.ReaderPreferences
        val chrome = args[4] as Boolean
        val settings = args[5] as Boolean
        val tocOpenV = args[6] as Boolean
        val hlOpen = args[7] as Boolean
        val focused = args[8] as String?
        val attached = args[9] as Boolean
        val failed = args[10] as Boolean
        val prog = args[11] as Float
        when {
            b == null && failed -> ReaderUiState.OpenFailed
            b == null -> ReaderUiState.Gone
            failed -> ReaderUiState.OpenFailed
            else -> ReaderUiState.Ready(
                book = b,
                toc = t,
                highlights = h,
                prefs = p,
                chromeVisible = chrome,
                settingsOpen = settings,
                tocOpen = tocOpenV,
                highlightsOpen = hlOpen,
                progression = (b.progress.takeIf { prog == 0f } ?: prog),
                positionText = positionText(),
                focusedHighlightId = focused,
                navigatorAttached = attached,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState.Loading)

    init {
        viewModelScope.launch { openPublication() }
        viewModelScope.launch {
            store.events.collect { handleSessionEvent(it) }
        }
        viewModelScope.launch {
            // Persist progress on every debounced locator (skip the initial 0).
            var first = true
            progression.collect { prog ->
                if (first) {
                    first = false
                    return@collect
                }
                val locator = store.latestLocator.value ?: return@collect
                repository.updateProgress(bookId, prog, locator.toJSON().toString())
            }
        }
        viewModelScope.launch {
            // Re-apply decorations whenever the highlight list changes.
            highlights.collect { applyDecorations(it) }
        }
        scheduleChromeHide()
    }

    // UI actions

    fun onAction(action: ReaderAction) {
        when (action) {
            ReaderAction.Back -> viewModelScope.launch { messageChannel.send(ReaderMessage.Pop) }
            ReaderAction.ContentTapped -> toggleChrome()
            ReaderAction.OpenSettings -> {
                settingsOpen.value = true
                chromeVisible.value = true
                chromeJob?.cancel()
            }
            ReaderAction.CloseSettings -> {
                settingsOpen.value = false
                scheduleChromeHide()
            }
            ReaderAction.OpenToc -> {
                tocOpen.value = true
                chromeJob?.cancel()
            }
            ReaderAction.CloseToc -> {
                tocOpen.value = false
                scheduleChromeHide()
            }
            ReaderAction.OpenHighlights -> {
                highlightsOpen.value = true
                focusedHighlightId.value = null
                chromeJob?.cancel()
            }
            ReaderAction.CloseHighlights -> {
                highlightsOpen.value = false
                focusedHighlightId.value = null
                scheduleChromeHide()
            }
            is ReaderAction.SeekTo -> seekTo(action.progression)
            is ReaderAction.GoTocEntry -> goTocEntry(action.entry)
            is ReaderAction.GoForward -> store.navigator?.goForward(action.animated)
            is ReaderAction.GoBackward -> store.navigator?.goBackward(action.animated)
            is ReaderAction.AddHighlight -> addHighlight(action.color, action.note)
            is ReaderAction.DeleteHighlight -> deleteHighlight(action.id)
            is ReaderAction.SetFlow -> updateReaderPrefs { it.copy(flow = action.flow) }
            is ReaderAction.SetFontScale -> updateReaderPrefs {
                it.copy(fontScale = action.scale.coerceIn(0.5f, 3f))
            }
            is ReaderAction.SetTextAlign -> updateReaderPrefs { it.copy(textAlign = action.align) }
            is ReaderAction.SetTheme -> updateReaderPrefs { it.copy(theme = action.theme) }
            is ReaderAction.SetBrightness -> updateReaderPrefs {
                it.copy(brightness = action.brightness.coerceIn(-1f, 1f))
            }
        }
    }

    // Session wiring

    private suspend fun openPublication() {
        // Mark the open before any suspension: until publish() runs there is
        // no navigator factory, and the UI must not attach the host fragment.
        store.beginOpen()

        val book = repository.observeBook(bookId).first()
        if (book == null) {
            store.failOpen()
            return // Gone state via UiState combine.
        }
        val prefs = preferences.readerPreferences.first()
        val mapped = EpubPreferencesMapper.map(prefs)

        when (val result = opener.open(book.sourcePath)) {
            is OpenResult.Opened -> {
                // An explicit href (from a search hit) wins over saved progress.
                val initialLocator = route.href
                    ?.let { locatorForHref(result.publication, it) }
                    ?: book.lastLocator?.let { raw ->
                        runCatching { Locator.fromJSON(JSONObject(raw)) }.getOrNull()
                    }
                val factory = EpubNavigatorFactory(result.publication)
                store.publish(bookId, result.publication, factory, initialLocator, mapped)
                positionsCache = runCatching { result.publication.positions() }
                    .getOrDefault(emptyList())
                indexContentIfNeeded()
            }
            OpenResult.FileMissing, OpenResult.ParseFailed -> {
                store.failOpen()
                openFailed.value = true
            }
        }
    }

    /**
     * Resolves a search hit's archive path to a locator. Indexed hrefs are
     * archive paths (`OEBPS/ch1.xhtml`) while Readium links are manifest
     * relative, so matching falls back to the file name.
     */
    private fun locatorForHref(publication: Publication, href: String): Locator? {
        val target = href.substringAfterLast('/').substringBefore('#')
        if (target.isEmpty()) return null
        val candidates = publication.readingOrder + publication.tableOfContents
        val link = candidates.firstOrNull {
            it.href.toString().substringAfterLast('/').substringBefore('#') == target
        } ?: return null
        return runCatching { publication.locatorFromLink(link) }.getOrNull()
    }

    /** Indexes chapter text once per book so content search has data. */
    private fun indexContentIfNeeded() {
        viewModelScope.launch {
            runCatching {
                if (!repository.isContentIndexed(bookId)) {
                    repository.indexBookContent(bookId)
                }
            }
        }
    }

    private suspend fun handleSessionEvent(event: ReaderSessionEvent) {
        when (event) {
            ReaderSessionEvent.SessionLost -> messageChannel.send(ReaderMessage.Pop)
            ReaderSessionEvent.ContentTapped -> toggleChrome()
            ReaderSessionEvent.NavigatorAttached -> {
                navigatorAttached.value = true
                // Fresh navigator: submit current prefs + decorations.
                val prefs = EpubPreferencesMapper.map(preferences.readerPreferences.first())
                runCatching { store.navigator?.submitPreferences(prefs) }
                applyDecorations(highlights.first())
            }
            is ReaderSessionEvent.DecorationTapped -> {
                focusedHighlightId.value = event.decorationId
                highlightsOpen.value = true
                chromeVisible.value = false
                chromeJob?.cancel()
            }
            is ReaderSessionEvent.ExternalLink ->
                messageChannel.send(ReaderMessage.OpenUrl(event.url))
            ReaderSessionEvent.ResourceFailed ->
                messageChannel.send(ReaderMessage.Text("Couldn't load part of this book"))
        }
    }

    private fun toggleChrome() {
        chromeVisible.update { !it }
        if (chromeVisible.value) scheduleChromeHide() else chromeJob?.cancel()
    }

    private fun scheduleChromeHide() {
        chromeJob?.cancel()
        chromeJob = viewModelScope.launch {
            delay(CHROME_AUTO_HIDE_MS)
            if (!settingsOpen.value && !tocOpen.value && !highlightsOpen.value) {
                chromeVisible.value = false
            }
        }
    }

    // Navigation + annotations

    private fun seekTo(progression: Float) {
        val nav = store.navigator ?: return
        val positions = positionsCache
        if (positions.isEmpty()) return
        val index = (progression * (positions.size - 1)).toInt().coerceIn(0, positions.size - 1)
        nav.go(positions[index])
    }

    private fun goTocEntry(entry: TocEntry) {
        val publication = store.publication ?: return
        val nav = store.navigator ?: return
        tocOpen.value = false
        scheduleChromeHide()
        val link = publication.tableOfContents.firstOrNull {
            it.href.toString().substringBefore('#') == entry.href.substringBefore('#') ||
                entry.href.substringBefore('#').endsWith(it.href.toString().substringBefore('#'))
        } ?: return
        val locator = runCatching { publication.locatorFromLink(link) }.getOrNull() ?: return
        nav.go(locator)
    }

    private fun addHighlight(color: HighlightColor, note: String?) {
        viewModelScope.launch {
            val nav = store.navigator
            val selection = nav?.let {
                runCatching { it.currentSelection() }.getOrNull()
            }
            if (selection == null) {
                messageChannel.send(ReaderMessage.SelectTextFirst)
                return@launch
            }
            val locatorJson = selection.locator.toJSON().toString()
            val highlight = Highlight(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                href = selection.locator.href.toString(),
                startLocator = locatorJson,
                endLocator = locatorJson,
                selectedText = "",
                color = color,
                note = note?.ifBlank { null },
                createdAt = System.currentTimeMillis(),
            )
            repository.upsertHighlight(highlight)
            nav.clearSelection()
            messageChannel.send(ReaderMessage.HighlightSaved)
        }
    }

    private fun deleteHighlight(id: String) {
        viewModelScope.launch {
            repository.deleteHighlight(id)
            if (focusedHighlightId.value == id) focusedHighlightId.value = null
        }
    }

    private suspend fun applyDecorations(highlights: List<Highlight>) {
        val nav = store.navigator ?: return
        val decorations = highlights.mapNotNull { h ->
            val locator = runCatching { Locator.fromJSON(JSONObject(h.startLocator)) }.getOrNull()
                ?: return@mapNotNull null
            Decoration(
                id = h.id,
                locator = locator,
                style = Decoration.Style.Highlight(tint = highlightTint(h.color)),
            )
        }
        runCatching { nav.applyDecorations(decorations, DECORATION_GROUP) }
    }

    private fun positionText(): String? {
        val positions = positionsCache
        if (positions.isEmpty()) return null
        val current = store.latestLocator.value ?: return "1 / ${positions.size}"
        val index = positions.indexOfFirst { it.href == current.href }
        // Fallback to nearest by total progression when hrefs diverge.
        val pos = if (index >= 0) {
            index + 1
        } else {
            ((current.locations.totalProgression ?: 0.0) * positions.size).toInt()
                .coerceIn(1, positions.size)
        }
        return "$pos / ${positions.size}"
    }

    private fun updateReaderPrefs(transform: (com.iridium.core.model.ReaderPreferences) -> com.iridium.core.model.ReaderPreferences) {
        viewModelScope.launch {
            preferences.updateReaderPreferences(transform)
            val updated = EpubPreferencesMapper.map(preferences.readerPreferences.first())
            runCatching { store.navigator?.submitPreferences(updated) }
        }
    }

    override fun onCleared() {
        if (store.bookId == bookId) {
            // Flush the latest locator synchronously before closing.
            store.latestLocator.value?.let { locator ->
                val prog = locator.locations.totalProgression?.toFloat() ?: 0f
                // Fire-and-forget is unsafe in onCleared; Room write races
                // process death, but the debounced collector already saved
                // everything older than 500ms — this covers only the tail.
                viewModelScope.launch {
                    runCatching {
                        repository.updateProgress(bookId, prog, locator.toJSON().toString())
                    }
                }
            }
            store.clear()
        }
        super.onCleared()
    }

    private companion object {
        const val CHROME_AUTO_HIDE_MS = 3000L
        const val LOCATOR_SAVE_DEBOUNCE_MS = 500L
        const val DECORATION_GROUP = "highlights"
    }
}

internal fun highlightTint(color: com.iridium.core.model.HighlightColor): Int = when (color) {
    com.iridium.core.model.HighlightColor.YELLOW -> 0xFFFFFF00.toInt()
    com.iridium.core.model.HighlightColor.GREEN -> 0xFF90EE90.toInt()
    com.iridium.core.model.HighlightColor.TEAL -> 0xFF40E0D0.toInt()
    com.iridium.core.model.HighlightColor.BLUE -> 0xFFADD8E6.toInt()
    com.iridium.core.model.HighlightColor.RED -> 0xFFFF7F7F.toInt()
    com.iridium.core.model.HighlightColor.PURPLE -> 0xFFDDA0DD.toInt()
}
