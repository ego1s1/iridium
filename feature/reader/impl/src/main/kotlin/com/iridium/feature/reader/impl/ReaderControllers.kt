package com.iridium.feature.reader.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
