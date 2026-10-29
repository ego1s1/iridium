package com.iridium.feature.reader.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
