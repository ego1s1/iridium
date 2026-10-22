package com.iridium.feature.reader.impl

import com.iridium.core.common.AppClock
import com.iridium.core.data.LocatorStrings
import com.iridium.core.data.BooksRepository
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.Highlight
import com.iridium.core.model.HighlightColor
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.resolvedColors
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.readium.r2.navigator.Decoration
import org.readium.r2.shared.publication.Locator

/**
 * Chrome + sheet visibility for the reader. Owns the five visibility flows,
 * the auto-hide timer, and the focused highlight id so open/close/toggle
 * paths share one implementation instead of copy-pasted flag blocks.
 */
internal class ReaderChromeController(private val scope: CoroutineScope) {

    val chromeVisible = MutableStateFlow(true)
    val settingsOpen = MutableStateFlow(false)
    val themeSheetOpen = MutableStateFlow(false)
    val tocOpen = MutableStateFlow(false)
    val highlightsOpen = MutableStateFlow(false)
    val focusedHighlightId = MutableStateFlow<String?>(null)

    private var chromeJob: Job? = null

    fun anySheetOpen(): Boolean =
        settingsOpen.value || themeSheetOpen.value ||
            tocOpen.value || highlightsOpen.value

    /** Opens a sheet and holds chrome; TOC/highlights only cancel auto-hide. */
    fun open(sheet: MutableStateFlow<Boolean>, pinChromeVisible: Boolean = false) {
        sheet.value = true
        if (pinChromeVisible) chromeVisible.value = true
        chromeJob?.cancel()
    }

    /** Closes a sheet and resumes auto-hide; highlight sheets also clear focus. */
    fun close(sheet: MutableStateFlow<Boolean>, clearHighlightFocus: Boolean = false) {
        sheet.value = false
        if (clearHighlightFocus) focusedHighlightId.value = null
        scheduleHide()
    }

    fun toggle() {
        chromeVisible.update { !it }
        if (chromeVisible.value) scheduleHide() else chromeJob?.cancel()
    }

    /** User page turns hide the chrome immediately. */
    fun hideForPageTurn() {
        chromeVisible.value = false
        chromeJob?.cancel()
    }

    /** Tapping a highlight decoration selects it and raises the sheet. */
    fun showHighlightsForDecoration(decorationId: String) {
        focusedHighlightId.value = decorationId
        highlightsOpen.value = true
        chromeVisible.value = false
        chromeJob?.cancel()
    }

    /** Clears focus when its highlight is deleted out from under the sheet. */
    fun clearFocusIf(id: String) {
        if (focusedHighlightId.value == id) focusedHighlightId.value = null
    }

    fun scheduleHide() {
        chromeJob?.cancel()
        chromeJob = scope.launch {
            delay(CHROME_AUTO_HIDE_MS)
            if (!anySheetOpen()) {
                chromeVisible.value = false
            }
        }
    }

    private companion object {
        const val CHROME_AUTO_HIDE_MS = 3000L
    }
}

/**
 * Selection → lookup → popup lifecycle for the dictionary. Serializes
 * lookups per selection drag (cancel-and-replace) with a reveal delay so
 * cache hits skip the loading skeleton entirely.
 */
internal class ReaderDictionaryController(
    private val scope: CoroutineScope,
    private val dictionaryLookup: DictionaryLookup,
    private val store: ReaderSessionStore,
) {

    private val _ui = MutableStateFlow<DictionaryUiState?>(null)
    val ui: StateFlow<DictionaryUiState?> = _ui.asStateFlow()

    private var lookupJob: Job? = null

    val isOpen: Boolean get() = _ui.value != null

    fun onSelectionChanged(text: String?) {
        lookupJob?.cancel()
        // Normalize first: punctuation-only or blank selections show no
        // popup at all, and the popup titles the clean headword rather than
        // the raw multi-word drag.
        val query = text?.let(::normalizeLookupWord)
        if (query.isNullOrEmpty()) {
            _ui.value = null
            return
        }
        lookupJob = scope.launch {
            // Debounce first: selection drags fire per word, and a blocking
            // HTTP call cannot be cancelled mid-flight — superseded words
            // must never reach the network.
            delay(DICTIONARY_DEBOUNCE_MS)
            val lookup = async { dictionaryLookup.define(query) }
            // Reveal delay: cache hits resolve inside it (popup appears once,
            // with content); slow lookups show a skeleton instead of flashing.
            delay(DICTIONARY_REVEAL_DELAY_MS)
            if (!lookup.isCompleted) {
                _ui.value = DictionaryUiState(word = query, loading = true)
            }
            val result = lookup.await()
            _ui.value = result.fold(
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

    fun dismiss() {
        lookupJob?.cancel()
        _ui.value = null
        store.clearSelection()
        runCatching { store.navigator?.clearSelection() }
    }

    private companion object {
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
    }
}

/**
 * Positions table + scrubber math for the reader. Owns the cached positions,
 * the recombine trigger version, the programmatic-nav grace clock, and
 * slider seeks; the ViewModel only forwards events.
 */
internal class ReaderPositionTracker(
    private val store: ReaderSessionStore,
    private val clock: AppClock,
) {

    /** Last programmatic jump (open/seek/TOC): locator ticks inside the grace window are the jump's own echo, not user page turns. */
    private var lastProgrammaticNavMs: Long = 0L

    private var positionsCache: List<Locator> = emptyList()
    val positionsVersion = MutableStateFlow(0)

    fun replaceAll(positions: List<Locator>) {
        positionsCache = positions
        positionsVersion.value += 1
    }

    fun markProgrammaticNav() {
        lastProgrammaticNavMs = clock.uptimeNow()
    }

    fun withinNavGrace(windowMs: Long): Boolean =
        clock.uptimeNow() - lastProgrammaticNavMs <= windowMs

    fun positionText(): String? {
        val (index, count) = indexAndCount()
        if (count == 0) return null
        return "${index + 1} / $count"
    }

    /** 0-based position index + total count from the cached positions table. */
    fun indexAndCount(): Pair<Int, Int> {
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

    /** Slider seek: jumps the navigator to the position table entry. */
    fun seekTo(progression: Float) {
        val nav = store.navigator ?: return
        val positions = positionsCache
        if (positions.isEmpty()) return
        markProgrammaticNav()
        val index = (progression * (positions.size - 1)).toInt().coerceIn(0, positions.size - 1)
        nav.go(positions[index])
    }
}

/**
 * Highlight CRUD, navigator decorations, and annotation export. Decorations
 * re-apply on every highlight-list emission; unchanged entries are cheap
 * (parse-only) while the navigator diffs the application itself.
 */
internal class ReaderAnnotationsController(
    scope: CoroutineScope,
    private val repository: BooksRepository,
    private val bookId: String,
    private val store: ReaderSessionStore,
    private val chrome: ReaderChromeController,
    private val clock: AppClock,
    private val uiState: StateFlow<ReaderUiState>,
    private val sendMessage: suspend (ReaderMessage) -> Unit,
    highlights: Flow<List<Highlight>>,
) {

    init {
        scope.launch { highlights.collect { applyDecorations(it) } }
    }

    fun addHighlight(color: HighlightColor, note: String?, launchIn: CoroutineScope) {
        launchIn.launch {
            val nav = store.navigator
            val selection = nav?.let {
                runCatching { it.currentSelection() }.getOrNull()
            }
            if (selection == null) {
                sendMessage(ReaderMessage.SelectTextFirst)
                return@launch
            }
            val locatorJson = LocatorStrings.encode(selection.locator)
            val highlight = Highlight(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                href = selection.locator.href.toString(),
                startLocator = locatorJson,
                endLocator = locatorJson,
                selectedText = selection.locator.text.highlight.orEmpty(),
                color = color,
                note = note?.ifBlank { null },
                createdAt = clock.wallNow(),
            )
            repository.upsertHighlight(highlight)
            nav.clearSelection()
            sendMessage(ReaderMessage.HighlightSaved)
        }
    }

    fun deleteHighlight(id: String, launchIn: CoroutineScope) {
        launchIn.launch {
            repository.deleteHighlight(id)
            chrome.clearFocusIf(id)
        }
    }

    /** Builds a Markdown + HTML export of the current book's highlights/notes. */
    fun export(launchIn: CoroutineScope) {
        launchIn.launch {
            val ready = uiState.value as? ReaderUiState.Ready ?: return@launch
            val title = ready.book.title
            sendMessage(
                ReaderMessage.ShareExport(
                    markdown = AnnotationExporter.markdown(title, ready.highlights),
                    html = AnnotationExporter.html(title, ready.highlights),
                    filename = title.replace(Regex("[^A-Za-z0-9]+"), "_"),
                ),
            )
        }
    }

    /** Re-submits decorations for a fresh navigator (attach drops the old one). */
    suspend fun refreshDecorations(highlights: List<Highlight>) {
        applyDecorations(highlights)
    }

    private suspend fun applyDecorations(highlights: List<Highlight>) {
        val nav = store.navigator ?: return
        val decorations = highlights.mapNotNull { h ->
            val locator = LocatorStrings.decode(h.startLocator)
                ?: return@mapNotNull null
            Decoration(
                id = h.id,
                locator = locator,
                style = Decoration.Style.Highlight(tint = highlightTint(h.color)),
            )
        }
        runCatching { nav.applyDecorations(decorations, DECORATION_GROUP) }
    }

    private companion object {
        const val DECORATION_GROUP = "highlights"
    }
}

/**
 * Reader-preference writes + navigator submission. Every write re-submits
 * the mapped Readium preferences and re-injects the forced-override theme
 * stylesheet so exact colors win over the book's own CSS.
 */
internal class ReaderPrefsController(
    private val scope: CoroutineScope,
    private val preferences: IridiumPreferencesDataSource,
    private val store: ReaderSessionStore,
) {

    fun updateReaderPrefs(transform: (ReaderPreferences) -> ReaderPreferences) {
        scope.launch {
            preferences.updateReaderPreferences(transform)
            val updated = EpubPreferencesMapper.map(preferences.readerPreferences.first())
            runCatching { store.navigator?.submitPreferences(updated) }
            applyThemeOverride()
        }
    }

    /** Forces the current theme's colors onto the book page via injected CSS. */
    suspend fun applyThemeOverride() {
        val prefs = preferences.readerPreferences.first()
        val (bg, fg) = prefs.resolvedColors()
        val script = ReaderThemeOverride.injectOverrideScriptColors(
            ReaderThemeOverride.hex(bg),
            ReaderThemeOverride.hex(fg),
        )
        runCatching {
            store.navigator?.evaluateJavascript(script)
        }
    }
}
