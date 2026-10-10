package com.iridium.feature.reader.impl

import android.view.KeyEvent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iridium.core.data.BookOpener
import com.iridium.core.data.BooksRepository
import com.iridium.core.data.OpenResult
import com.iridium.core.datastore.ApplicationScope
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.Book
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.HighlightColor
import com.iridium.core.model.TocEntry
import com.iridium.feature.reader.api.ReaderRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val opener: BookOpener,
    private val dictionaryLookup: DictionaryLookup,
    private val store: ReaderSessionStore,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val route: ReaderRoute = savedStateHandle.toRoute()
    private val bookId: String = route.bookId

    private val chromeVisible = MutableStateFlow(true)
    private val settingsOpen = MutableStateFlow(false)
    private val themeSheetOpen = MutableStateFlow(false)
    private val tocOpen = MutableStateFlow(false)
    private val highlightsOpen = MutableStateFlow(false)
    private val focusedHighlightId = MutableStateFlow<String?>(null)
    private val navigatorAttached = MutableStateFlow(false)
    private val openFailed = MutableStateFlow(false)
    private val openFileMissing = MutableStateFlow(false)
    private var chromeJob: Job? = null
    private var dictionaryJob: Job? = null

    /**
     * Suppresses locator-driven chrome hides right after programmatic
     * navigation (open, seek, TOC jump): only user page turns hide chrome.
     */
    private var lastProgrammaticNavMs: Long = 0L

    /** Cached prefs for tap paths that must not suspend on DataStore IO. */
    private val prefsFlow: StateFlow<com.iridium.core.model.ReaderPreferences> =
        preferences.readerPreferences.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = com.iridium.core.model.ReaderPreferences(),
        )

    /** Publication positions for the slider/position text (no fixed pages). */
    private var positionsCache: List<Locator> = emptyList()

    /**
     * Bumped whenever [positionsCache] is (re)filled: the cache is a plain
     * var outside every combine key, so without this the scrubber/counter
     * would sit at zero until the next debounced progression tick.
     */
    private val positionsVersion = MutableStateFlow(0)

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
        themeSheetOpen, openFileMissing, positionsVersion,
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
        val themeSheet = args[12] as Boolean
        val fileMissing = args[13] as Boolean
        // positionsVersion (args[14]) is a recombine trigger only: reading it
        // here keeps the scrubber/counter in sync when positions land.
        when {
            b == null && failed -> ReaderUiState.OpenFailed(fileMissing)
            b == null -> ReaderUiState.Gone
            failed -> ReaderUiState.OpenFailed(fileMissing)
            else -> {
                val (positionIndex, positionCount) = positionIndexAndCount()
                ReaderUiState.Ready(
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
                    positionIndex = positionIndex,
                    positionCount = positionCount,
                    focusedHighlightId = focused,
                    navigatorAttached = attached,
                    themeSheetOpen = themeSheet,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState.Loading)

    init {
        viewModelScope.launch { openPublication() }
        viewModelScope.launch {
            store.events.collect { handleSessionEvent(it) }
        }
        viewModelScope.launch {
            // Persist progress on every debounced locator value.
            progression.collect { prog ->
                val locator = store.latestLocator.value ?: return@collect
                repository.updateProgress(bookId, prog, locator.toJSON().toString())
            }
        }
        viewModelScope.launch {
            // Re-apply decorations whenever the highlight list changes.
            highlights.collect { applyDecorations(it) }
        }
        viewModelScope.launch {
            // User page turns hide the chrome; programmatic jumps (open,
            // seek, TOC) and the initial publish must not.
            store.latestLocator.collect { locator ->
                if (locator != null &&
                    SystemClock.uptimeMillis() - lastProgrammaticNavMs > LOCATOR_HIDE_GRACE_MS
                ) {
                    chromeVisible.value = false
                    chromeJob?.cancel()
                }
            }
        }
        scheduleChromeHide()
    }

    // UI actions

    fun onAction(action: ReaderAction) {
        when (action) {
            ReaderAction.Back -> viewModelScope.launch { messageChannel.send(ReaderMessage.Pop) }
            ReaderAction.OpenSettings -> {
                settingsOpen.value = true
                chromeVisible.value = true
                chromeJob?.cancel()
            }
            ReaderAction.CloseSettings -> {
                settingsOpen.value = false
                scheduleChromeHide()
            }
            ReaderAction.OpenThemeSheet -> {
                themeSheetOpen.value = true
                chromeVisible.value = true
                chromeJob?.cancel()
            }
            ReaderAction.CloseThemeSheet -> {
                themeSheetOpen.value = false
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
            ReaderAction.DismissDictionary -> dismissDictionary()
            is ReaderAction.SeekTo -> {
                seekTo(action.progression)
                scheduleChromeHide()
            }
            is ReaderAction.GoTocEntry -> goTocEntry(action.entry)
            is ReaderAction.GoForward -> {
                store.navigator?.goForward(action.animated)
                scheduleChromeHide()
            }
            is ReaderAction.GoBackward -> {
                store.navigator?.goBackward(action.animated)
                scheduleChromeHide()
            }
            is ReaderAction.AddHighlight -> addHighlight(action.color, action.note)
            is ReaderAction.DeleteHighlight -> deleteHighlight(action.id)
            is ReaderAction.SetFlow,
            is ReaderAction.SetFontScale,
            is ReaderAction.SetLineHeight,
            is ReaderAction.SetTextAlign,
            is ReaderAction.SetTheme,
            is ReaderAction.SetBrightness,
            is ReaderAction.SetKeepScreenOn,
            is ReaderAction.SetShowPageCounter,
            is ReaderAction.SetVolumeKeys,
            is ReaderAction.SetVolumeKeysInverted,
            is ReaderAction.SetInvertTaps,
            is ReaderAction.SetNightLight,
            is ReaderAction.SetNightLightIntensity,
            -> onPrefsAction(action)
        }
    }

    /** Reader-preference writes, split out so onAction stays under the complexity gate. */
    private fun onPrefsAction(action: ReaderAction) {
        when (action) {
            is ReaderAction.SetFlow -> updateReaderPrefs { it.copy(flow = action.flow) }
            is ReaderAction.SetFontScale -> updateReaderPrefs {
                it.copy(fontScale = action.scale.coerceIn(0.5f, 3f))
            }
            is ReaderAction.SetLineHeight -> updateReaderPrefs {
                it.copy(lineHeight = action.lineHeight.coerceIn(1f, 2.5f))
            }
            is ReaderAction.SetTextAlign -> updateReaderPrefs { it.copy(textAlign = action.align) }
            is ReaderAction.SetTheme -> updateReaderPrefs { it.copy(theme = action.theme) }
            is ReaderAction.SetBrightness -> updateReaderPrefs {
                it.copy(brightness = action.brightness.coerceIn(-1f, 1f))
            }
            is ReaderAction.SetKeepScreenOn -> updateReaderPrefs {
                it.copy(keepScreenOn = action.enabled)
            }
            is ReaderAction.SetShowPageCounter -> updateReaderPrefs {
                it.copy(showPageCounter = action.enabled)
            }
            is ReaderAction.SetVolumeKeys -> updateReaderPrefs {
                it.copy(volumeKeys = action.enabled)
            }
            is ReaderAction.SetVolumeKeysInverted -> updateReaderPrefs {
                it.copy(volumeKeysInverted = action.inverted)
            }
            is ReaderAction.SetInvertTaps -> updateReaderPrefs {
                it.copy(invertTaps = action.inverted)
            }
            is ReaderAction.SetNightLight -> updateReaderPrefs {
                it.copy(nightLight = action.enabled)
            }
            is ReaderAction.SetNightLightIntensity -> updateReaderPrefs {
                it.copy(nightLightIntensity = action.intensity.coerceIn(0f, 1f))
            }
            // Statement position: anything else was routed here by mistake and
            // is intentionally ignored (callers only pass prefs actions).
            else -> Unit
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
                positionsVersion.value += 1
                indexContentIfNeeded()
            }
            OpenResult.FileMissing, OpenResult.ParseFailed -> {
                store.failOpen()
                openFailed.value = true
                openFileMissing.value = result == OpenResult.FileMissing
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
            // No SessionLost branch: nothing emits it, and session death
            // surfaces as Gone/OpenFailed, which own the exit UI. (The else
            // satisfies exhaustiveness; a future emitter must route through
            // those states, never a second Pop.)
            ReaderSessionEvent.ContentTapped -> {
                // A content tap with the dictionary open dismisses the popup
                // instead of toggling chrome; taps behind any sheet are the
                // modal's, not the page's.
                if (_dictionaryUi.value != null) {
                    dismissDictionary()
                } else if (!settingsOpen.value && !themeSheetOpen.value &&
                    !tocOpen.value && !highlightsOpen.value
                ) {
                    toggleChrome()
                }
            }
            is ReaderSessionEvent.ContentTappedAt ->
                handleZonedTap(event.fractionX, event.fractionY, event.dismissedPopup)
            ReaderSessionEvent.NavigatorAttached -> {
                navigatorAttached.value = true
                lastProgrammaticNavMs = SystemClock.uptimeMillis()
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
            is ReaderSessionEvent.SelectionChanged -> onSelectionChanged(event.text)
            else -> Unit
        }
    }

    private fun toggleChrome() {
        chromeVisible.update { !it }
        if (chromeVisible.value) scheduleChromeHide() else chromeJob?.cancel()
    }

    /**
     * Routes a positioned content tap through the fixed tap zones: outer
     * thirds turn positions, the center toggles chrome (optionally mirrored
     * by the invert-taps switch). Links keep working — Readium follows those
     * before listeners run. Taps landing while a sheet or the dictionary is
     * open do nothing: the modal owns the gesture.
     */
    private fun handleZonedTap(fractionX: Float, fractionY: Float, dismissedPopup: Boolean) {
        if (dismissedPopup) {
            dismissDictionary()
            return
        }
        if (_dictionaryUi.value != null ||
            settingsOpen.value || themeSheetOpen.value ||
            tocOpen.value || highlightsOpen.value
        ) {
            return
        }
        when (
            chromeZoneForTap(
                fractionX,
                fractionY,
                ChromeReadingDirection.LEFT_TO_RIGHT,
                prefsFlow.value.invertTaps,
            )
        ) {
            ChromeTapZone.PREV -> {
                store.navigator?.goBackward(true)
                scheduleChromeHide()
            }
            ChromeTapZone.NEXT -> {
                store.navigator?.goForward(true)
                scheduleChromeHide()
            }
            ChromeTapZone.MENU -> toggleChrome()
        }
    }

    private fun scheduleChromeHide() {
        chromeJob?.cancel()
        chromeJob = viewModelScope.launch {
            delay(CHROME_AUTO_HIDE_MS)
            if (!settingsOpen.value && !themeSheetOpen.value &&
                !tocOpen.value && !highlightsOpen.value
            ) {
                chromeVisible.value = false
            }
        }
    }

    /**
     * Volume-key paging gate (Mori parity): volume keys turn pages only while
     * paging is enabled, the chrome is hidden, and no sheet is open — so
     * volume always works normally everywhere else. Single source of truth.
     */
    val volumePagingActive: StateFlow<Boolean> = uiState
        .map { state -> (state as? ReaderUiState.Ready)?.let { ready -> isVolumePagingActive(ready) } ?: false }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private fun isVolumePagingActive(state: ReaderUiState.Ready): Boolean {
        return state.prefs.volumeKeys &&
            !state.chromeVisible &&
            !state.settingsOpen && !state.themeSheetOpen &&
            !state.tocOpen && !state.highlightsOpen
    }

    /**
     * Handles a hardware key event for volume-key paging. Consumes key-down
     * (plus repeats) and key-up so the system volume panel never appears
     * during a page turn; navigates only on key-up. When
     * `prefs.volumeKeysInverted` is on, volume-down goes back and volume-up
     * goes forward.
     *
     * @return true when the event was consumed.
     */
    fun onVolumeKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
            event.keyCode != KeyEvent.KEYCODE_VOLUME_UP
        ) {
            return false
        }
        val state = uiState.value as? ReaderUiState.Ready ?: return false
        if (!isVolumePagingActive(state)) return false
        if (event.action == KeyEvent.ACTION_UP) {
            val forward = if (state.prefs.volumeKeysInverted) {
                event.keyCode == KeyEvent.KEYCODE_VOLUME_UP
            } else {
                event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
            }
            onAction(if (forward) ReaderAction.GoForward() else ReaderAction.GoBackward())
        }
        return true
    }

    private val _dictionaryUi = MutableStateFlow<DictionaryUiState?>(null)
    val dictionaryUi: StateFlow<DictionaryUiState?> = _dictionaryUi.asStateFlow()

    private fun onSelectionChanged(text: String?) {
        dictionaryJob?.cancel()
        // Normalize first: punctuation-only or blank selections show no
        // popup at all, and the popup titles the clean headword rather than
        // the raw multi-word drag.
        val query = text?.let(::normalizeLookupWord)
        if (query.isNullOrEmpty()) {
            _dictionaryUi.value = null
            return
        }
        dictionaryJob = viewModelScope.launch {
            // Debounce first: selection drags fire per word, and a blocking
            // HTTP call cannot be cancelled mid-flight — superseded words
            // must never reach the network.
            delay(DICTIONARY_DEBOUNCE_MS)
            val lookup = async { dictionaryLookup.define(query) }
            // Reveal delay: cache hits resolve inside it (popup appears once,
            // with content); slow lookups show a skeleton instead of flashing.
            delay(DICTIONARY_REVEAL_DELAY_MS)
            if (!lookup.isCompleted) {
                _dictionaryUi.value = DictionaryUiState(word = query, loading = true)
            }
            val result = lookup.await()
            _dictionaryUi.value = result.fold(
                onSuccess = { DictionaryUiState(word = query, definition = it) },
                onFailure = {
                    DictionaryUiState(
                        word = query,
                        error = it.message ?: "Couldn't look up this word",
                    )
                },
            )
        }
    }

    private fun dismissDictionary() {
        dictionaryJob?.cancel()
        _dictionaryUi.value = null
        store.clearSelection()
        runCatching { store.navigator?.clearSelection() }
    }

    // Navigation + annotations

    private fun seekTo(progression: Float) {
        val nav = store.navigator ?: return
        val positions = positionsCache
        if (positions.isEmpty()) return
        lastProgrammaticNavMs = SystemClock.uptimeMillis()
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
        lastProgrammaticNavMs = SystemClock.uptimeMillis()
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
                selectedText = selection.locator.text.highlight.orEmpty(),
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
        val (index, count) = positionIndexAndCount()
        if (count == 0) return null
        return "${index + 1} / $count"
    }

    /** 0-based position index + total count from the cached positions table. */
    private fun positionIndexAndCount(): Pair<Int, Int> {
        val positions = positionsCache
        if (positions.isEmpty()) return 0 to 0
        val current = store.latestLocator.value ?: return 0 to positions.size
        val index = positions.indexOfFirst { it.href == current.href }
        // Fallback to nearest by total progression when hrefs diverge.
        val pos = if (index >= 0) {
            index
        } else {
            ((current.locations.totalProgression ?: 0.0) * positions.size).toInt()
                .coerceIn(0, positions.size - 1)
        }
        return pos to positions.size
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
            // Flush the latest locator before closing in the application
            // scope: viewModelScope is already cancelled here (even
            // NonCancellable children never run), so the tail write needs
            // a scope that outlives the ViewModel.
            store.latestLocator.value?.let { locator ->
                val prog = locator.locations.totalProgression?.toFloat() ?: 0f
                appScope.launch {
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

        /**
         * Locator changes inside this window after programmatic navigation
         * (open, seek, TOC jump) never hide the chrome: only user page turns
         * do.
         */
        const val LOCATOR_HIDE_GRACE_MS = 750L

        /**
         * Selection debounce before the lookup starts: drags fire per word
         * and a blocking HTTP call cannot be cancelled mid-flight, so
         * superseded words must never reach the network.
         */
        const val DICTIONARY_DEBOUNCE_MS = 150L

        /**
         * Grace period before the dictionary popup reveals: lookups finishing
         * inside it (cache hits) skip the loading skeleton entirely.
         */
        const val DICTIONARY_REVEAL_DELAY_MS = 150L
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
