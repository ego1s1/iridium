package com.iridium.feature.reader.impl

import android.view.KeyEvent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.iridium.core.common.safeCatching
import com.iridium.core.data.LocatorStrings
import com.iridium.core.data.BookOpener
import com.iridium.core.data.BooksRepository
import com.iridium.core.data.OpenResult
import com.iridium.core.datastore.ApplicationScope
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.Book
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.HighlightColor
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.TocEntry
import com.iridium.core.model.resolvedColors
import com.iridium.feature.reader.api.ReaderKeyInterceptor
import com.iridium.feature.reader.api.ReaderRoute
import kotlinx.collections.immutable.toImmutableList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.Url

@Suppress("TooManyFunctions") // Large host ViewModel; refactor deferred.
@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clock: com.iridium.core.common.AppClock,
    private val keyInterceptor: ReaderKeyInterceptor,
    private val repository: BooksRepository,
    private val preferences: IridiumPreferencesDataSource,
    private val opener: BookOpener,
    private val dictionaryLookup: DictionaryLookup,
    private val store: ReaderSessionStore,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val route: ReaderRoute = savedStateHandle.toRoute()
    private val bookId: String = route.bookId

    private val chrome = ReaderChromeController(viewModelScope)
    private val dictionary = ReaderDictionaryController(viewModelScope, dictionaryLookup, store)
    private val positions = ReaderPositionTracker(store, clock)
    private val prefsApplier = ReaderPrefsController(viewModelScope, preferences, store, positions)
    private val navigatorAttached = MutableStateFlow(false)
    private val openFailed = MutableStateFlow(false)
    private val openFileMissing = MutableStateFlow(false)

    /** Cached prefs for tap paths that must not suspend on DataStore IO. */
    private val prefsFlow: StateFlow<ReaderPreferences> =
        preferences.readerPreferences.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ReaderPreferences(),
        )

    private val messageChannel = Channel<ReaderMessage>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    /** Burst guard state for [requestTurn] (main-thread only). */
    private var lastTurnMs = 0L
    private var burstWindowStartMs = 0L
    private var burstTurns = 0

    /**
     * Tap-time page-turn pulses (+1 forward, -1 backward), emitted the moment
     * a turn is accepted — before the navigator's asynchronous commit. The UI
     * starts its slide from this so the motion begins with the tap instead of
     * one WebView round-trip later.
     */
    private val _pageTurns = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    val pageTurns: SharedFlow<Int> = _pageTurns.asSharedFlow()

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

    /** Publication content: book + navigation + annotations + prefs. */
    private data class ReaderContent(
        val book: Book?,
        val toc: List<TocEntry>,
        val highlights: List<Highlight>,
        val prefs: ReaderPreferences,
    )

    /** Chrome/sheet visibility bundle. */
    private data class ReaderChromeBundle(
        val chromeVisible: Boolean,
        val settingsOpen: Boolean,
        val tocOpen: Boolean,
        val highlightsOpen: Boolean,
        val themeSheetOpen: Boolean,
    )

    /** Navigation/session bundle. */
    private data class ReaderNavBundle(
        val focusedHighlightId: String?,
        val navigatorAttached: Boolean,
        val openFailed: Boolean,
        val progression: Float,
        val openFileMissing: Boolean,
    )

    private val content: Flow<ReaderContent> =
        combine(book, toc, highlights, preferences.readerPreferences, ::ReaderContent)
    private val chromeBundle: Flow<ReaderChromeBundle> =
        combine(
            chrome.chromeVisible, chrome.settingsOpen, chrome.tocOpen,
            chrome.highlightsOpen, chrome.themeSheetOpen, ::ReaderChromeBundle,
        )
    private val navBundle: Flow<ReaderNavBundle> =
        combine(
            chrome.focusedHighlightId, navigatorAttached, openFailed, progression,
            openFileMissing, ::ReaderNavBundle,
        )

    val uiState: StateFlow<ReaderUiState> = combine(
        content, chromeBundle, navBundle, positions.positionsVersion, store.latestLocator,
    ) { c, ch, n, _, _,
        ->
        // positionsVersion + latestLocator are recombine triggers: the
        // tracker reads latestLocator synchronously, but combine only
        // re-evaluates when an input changes — the debounced progression
        // alone left the counter frozen for rapid/small moves.
        when {
            c.book == null && n.openFailed -> ReaderUiState.OpenFailed(n.openFileMissing)
            c.book == null -> ReaderUiState.Gone
            n.openFailed -> ReaderUiState.OpenFailed(n.openFileMissing)
            else -> {
                val (positionIndex, positionCount) = positions.indexAndCount()
                ReaderUiState.Ready(
                    book = c.book,
                    toc = c.toc.toImmutableList(),
                    highlights = c.highlights.toImmutableList(),
                    prefs = c.prefs,
                    chromeVisible = ch.chromeVisible,
                    settingsOpen = ch.settingsOpen,
                    tocOpen = ch.tocOpen,
                    highlightsOpen = ch.highlightsOpen,
                    // Before the navigator attaches there is no live locator:
                    // show the saved progress. After attach, the debounced
                    // locator value is truth (0f is a real "start of book",
                    // not "not loaded").
                    progression = if (!n.navigatorAttached) c.book.progress else n.progression,
                    positionText = positions.positionText(),
                    positionIndex = positionIndex,
                    positionCount = positionCount,
                    focusedHighlightId = n.focusedHighlightId,
                    navigatorAttached = n.navigatorAttached,
                    themeSheetOpen = ch.themeSheetOpen,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState.Loading)

    private val annotations = ReaderAnnotationsController(
        scope = viewModelScope,
        repository = repository,
        bookId = bookId,
        store = store,
        chrome = chrome,
        clock = clock,
        uiState = uiState,
        sendMessage = { messageChannel.send(it) },
        highlights = highlights,
    )

    init {
        viewModelScope.launch { openPublication() }
        viewModelScope.launch {
            store.events.collect { handleSessionEvent(it) }
        }
        viewModelScope.launch {
            // Persist progress on every debounced locator value.
            progression.collect { prog ->
                val locator = store.latestLocator.value ?: return@collect
                repository.updateProgress(bookId, prog, LocatorStrings.encode(locator))
            }
        }
        viewModelScope.launch {
            // User page turns hide the chrome; programmatic jumps (open,
            // seek, TOC) and the initial publish must not.
            store.latestLocator.collect { locator ->
                if (locator != null &&
                    !positions.withinNavGrace(LOCATOR_HIDE_GRACE_MS)
                ) {
                    chrome.hideForPageTurn()
                }
            }
        }
        chrome.scheduleHide()
    }

    // UI actions

    fun onAction(action: ReaderAction) {
        when (action) {
            ReaderAction.Back -> viewModelScope.launch { messageChannel.send(ReaderMessage.Pop) }
            ReaderAction.OpenSettings -> chrome.open(chrome.settingsOpen, pinChromeVisible = true)
            ReaderAction.CloseSettings -> chrome.close(chrome.settingsOpen)
            ReaderAction.OpenThemeSheet -> chrome.open(chrome.themeSheetOpen, pinChromeVisible = true)
            ReaderAction.CloseThemeSheet -> chrome.close(chrome.themeSheetOpen)
            ReaderAction.OpenToc -> chrome.open(chrome.tocOpen)
            ReaderAction.CloseToc -> chrome.close(chrome.tocOpen)
            ReaderAction.OpenHighlights -> chrome.open(chrome.highlightsOpen)
            ReaderAction.CloseHighlights -> chrome.close(chrome.highlightsOpen, clearHighlightFocus = true)
            ReaderAction.DismissDictionary -> dictionary.dismiss()
            ReaderAction.DismissImage -> store.dismissImage()
            ReaderAction.ExportAnnotations -> annotations.export(viewModelScope)
            is ReaderAction.SeekTo -> {
                positions.seekTo(action.progression)
                chrome.scheduleHide()
            }
            is ReaderAction.ScrubStarted -> chrome.pinForScrub()
            is ReaderAction.ScrubFinished -> {
                positions.seekTo(action.progression)
                chrome.scheduleHide()
            }
            is ReaderAction.GoTocEntry -> goTocEntry(action.entry)
            is ReaderAction.GoForward -> requestTurn(1)
            is ReaderAction.GoBackward -> requestTurn(-1)
            is ReaderAction.AddHighlight -> annotations.addHighlight(action.color, action.note, viewModelScope)
            is ReaderAction.DeleteHighlight -> annotations.deleteHighlight(action.id, viewModelScope)
            is ReaderAction.PrefsChange -> onPrefsAction(action)
        }
    }

    /** Reader-preference writes, split out so onAction stays under the complexity gate. */
    private fun onPrefsAction(action: ReaderAction.PrefsChange) {
        when (action) {
            is ReaderAction.SetFlow -> prefsApplier.updateReaderPrefs { it.copy(flow = action.flow) }
            is ReaderAction.SetFontScale -> prefsApplier.updateReaderPrefs {
                it.copy(fontScale = action.scale.coerceIn(0.5f, 3f))
            }
            is ReaderAction.SetFontFamily -> prefsApplier.updateReaderPrefs {
                it.copy(fontFamily = action.family)
            }
            is ReaderAction.SetLineHeight -> prefsApplier.updateReaderPrefs {
                it.copy(lineHeight = action.lineHeight.coerceIn(1f, 2.5f))
            }
            is ReaderAction.SetTextAlign -> prefsApplier.updateReaderPrefs { it.copy(textAlign = action.align) }
            is ReaderAction.SetTheme -> prefsApplier.updateReaderPrefs { it.copy(theme = action.theme, customThemeId = null) }
            is ReaderAction.SetCustomTheme -> prefsApplier.updateReaderPrefs { it.copy(customThemeId = action.id) }
            is ReaderAction.SaveCustomTheme -> prefsApplier.updateReaderPrefs { prefs ->
                val themes = prefs.customThemes.filterNot { it.id == action.theme.id } + action.theme
                prefs.copy(customThemes = themes.toImmutableList(), customThemeId = action.theme.id)
            }
            is ReaderAction.DeleteCustomTheme -> prefsApplier.updateReaderPrefs { prefs ->
                prefs.copy(
                    customThemes = prefs.customThemes.filterNot { it.id == action.id }.toImmutableList(),
                    customThemeId = prefs.customThemeId?.takeUnless { it == action.id },
                )
            }
            is ReaderAction.SetBrightness -> prefsApplier.updateReaderPrefs {
                it.copy(brightness = action.brightness.coerceIn(-1f, 1f))
            }
            is ReaderAction.SetKeepScreenOn -> prefsApplier.updateReaderPrefs {
                it.copy(keepScreenOn = action.enabled)
            }
            is ReaderAction.SetVolumeKeys -> prefsApplier.updateReaderPrefs {
                it.copy(volumeKeys = action.enabled)
            }
            is ReaderAction.SetVolumeKeysInverted -> prefsApplier.updateReaderPrefs {
                it.copy(volumeKeysInverted = action.inverted)
            }
            is ReaderAction.SetInvertTaps -> prefsApplier.updateReaderPrefs {
                it.copy(invertTaps = action.inverted)
            }
            is ReaderAction.SetNightLight -> prefsApplier.updateReaderPrefs {
                it.copy(nightLight = action.enabled)
            }
            is ReaderAction.SetNightLightIntensity -> prefsApplier.updateReaderPrefs {
                it.copy(nightLightIntensity = action.intensity.coerceIn(0f, 1f))
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
                        LocatorStrings.decode(raw)
                    }
                val factory = EpubNavigatorFactory(result.publication)
                store.publish(bookId, result.publication, factory, initialLocator, mapped)
                positions.replaceAll(
                    safeCatching { result.publication.positions() }.getOrDefault(emptyList()),
                )
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
            safeCatching {
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
                if (dictionary.isOpen) {
                    dictionary.dismiss()
                } else if (!chrome.anySheetOpen()) {
                    chrome.toggle()
                }
            }
            is ReaderSessionEvent.ContentTappedAt ->
                handleZonedTap(event.fractionX, event.fractionY, event.dismissedPopup)
            ReaderSessionEvent.NavigatorAttached -> {
                navigatorAttached.value = true
                positions.markProgrammaticNav()
                // Fresh navigator: submit current prefs + decorations.
                val prefs = EpubPreferencesMapper.map(preferences.readerPreferences.first())
                runCatching { store.navigator?.submitPreferences(prefs) }
                prefsApplier.applyThemeOverride()
                annotations.refreshDecorations(highlights.first())
                // Image-showcase bridge: wire taps in the freshly loaded document.
                safeCatching { store.navigator?.evaluateJavascript(ReaderImageBridge.imageTapScript()) }
            }
            is ReaderSessionEvent.DecorationTapped -> {
                chrome.showHighlightsForDecoration(event.decorationId)
            }
            is ReaderSessionEvent.ExternalLink ->
                messageChannel.send(ReaderMessage.OpenUrl(event.url))
            ReaderSessionEvent.ResourceFailed ->
                messageChannel.send(ReaderMessage.ContentUnavailable)
            is ReaderSessionEvent.SelectionChanged -> dictionary.onSelectionChanged(event.text)
            else -> Unit
        }
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
            dictionary.dismiss()
            return
        }
        if (dictionary.isOpen || chrome.anySheetOpen()) {
            return
        }
        when (
            chromeZoneForTap(
                fractionX,
                fractionY,
                prefsFlow.value.invertTaps,
            )
        ) {
            ChromeTapZone.PREV -> requestTurn(-1)
            ChromeTapZone.NEXT -> requestTurn(1)
            ChromeTapZone.MENU -> chrome.toggle()
        }
    }

    /**
     * Single gate for every page turn. Caps rapid tapping so a burst cannot
     * outrun the navigator (the WebView swap plus the slide would queue and
     * tear), and emits the tap-time [pageTurns] pulse the UI slides on.
     *
     * @return true when the turn was accepted.
     */
    private fun requestTurn(direction: Int): Boolean {
        val now = clock.uptimeNow()
        // Burst window: reset once the user pauses.
        if (now - burstWindowStartMs > BURST_WINDOW_MS) {
            burstWindowStartMs = now
            burstTurns = 0
        }
        if (burstTurns >= MAX_BURST_TURNS) return false
        if (now - lastTurnMs < MIN_TURN_INTERVAL_MS) return false
        lastTurnMs = now
        burstTurns += 1
        // Keep chrome up while the user is turning; the timer restarts on
        // every accepted turn, so it hides only once turning stops.
        positions.markProgrammaticNav()
        _pageTurns.tryEmit(direction)
        viewModelScope.launch { positions.completeTurn(forward = direction > 0) }
        chrome.scheduleHide()
        return true
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

    init {
        // The handler lives only while the gate holds, so volume always
        // works normally everywhere else; cleared with the ViewModel so a
        // stale handler can never page a dead navigator.
        viewModelScope.launch {
            volumePagingActive.collect { active ->
                if (active) keyInterceptor.install(::onVolumeKeyEvent) else keyInterceptor.clear()
            }
        }
    }

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

    val dictionaryUi: StateFlow<DictionaryUiState?> = dictionary.ui

    /** Href of the book `<img>` being showcased full-screen, or null. */
    val shownImage: StateFlow<String?> = store.shownImage

    /** Reads a book resource's bytes for the image showcase (null if gone). */
    @OptIn(ExperimentalReadiumApi::class)
    suspend fun loadImageBytes(src: String): ByteArray? {
        val publication = store.publication ?: return null
        val url = Url(src) ?: return null
        val resource = publication.get(url) ?: return null
        val length = resource.length().getOrNull() ?: return null
        if (length > MAX_SHOWCASE_BYTES) return null
        return safeCatching { resource.read(0 until length).getOrNull() }.getOrNull()
    }

    /** Builds a Markdown + HTML export of the current book's highlights/notes. */
    // Navigation

    private fun goTocEntry(entry: TocEntry) {
        val publication = store.publication ?: return
        val nav = store.navigator ?: return
        chrome.tocOpen.value = false
        chrome.scheduleHide()
        val link = publication.tableOfContents.firstOrNull {
            it.href.toString().substringBefore('#') == entry.href.substringBefore('#') ||
                entry.href.substringBefore('#').endsWith(it.href.toString().substringBefore('#'))
        } ?: return
        val locator = runCatching { publication.locatorFromLink(link) }.getOrNull() ?: return
        positions.markProgrammaticNav()
        nav.go(locator)
    }

    override fun onCleared() {
        keyInterceptor.clear()
        if (store.bookId == bookId) {
            // Flush the latest locator before closing in the application
            // scope: viewModelScope is already cancelled here (even
            // NonCancellable children never run), so the tail write needs
            // a scope that outlives the ViewModel.
            store.latestLocator.value?.let { locator ->
                val prog = locator.locations.totalProgression?.toFloat() ?: 0f
                appScope.launch {
                    safeCatching {
                        repository.updateProgress(bookId, prog, LocatorStrings.encode(locator))
                    }
                }
            }
            store.clear()
        }
        super.onCleared()
    }

    private companion object {
        const val MAX_SHOWCASE_BYTES = 32 * 1024 * 1024L
        /**
         * Locator changes inside this window after programmatic navigation
         * (open, seek, TOC jump) never hide the chrome: only user page turns
         * do. Generous because nav.go() echoes asynchronously — a slow
         * WebView can land well after a 750ms window.
         */
        const val LOCATOR_HIDE_GRACE_MS = 1500L

        const val LOCATOR_SAVE_DEBOUNCE_MS = 500L

        /** Minimum gap between accepted turns: below this a tap is a repeat. */
        const val MIN_TURN_INTERVAL_MS = 90L

        /** At most [MAX_BURST_TURNS] accepted turns per [BURST_WINDOW_MS]. */
        const val BURST_WINDOW_MS = 1_000L
        const val MAX_BURST_TURNS = 6
    }
}

/** Canonical tint for [color]: the single source of truth also used by exports. */
internal fun highlightTint(color: com.iridium.core.model.HighlightColor): Int = color.argb
