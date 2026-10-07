package com.iridium.feature.reader.impl

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication

/** One-shot events from the Readium host fragment to the ViewModel/UI. */
sealed interface ReaderSessionEvent {
    /** Process death wiped the in-memory publication; leave the reader. */
    data object SessionLost : ReaderSessionEvent

    /** The navigator fragment attached; (re-)apply prefs and decorations. */
    data object NavigatorAttached : ReaderSessionEvent

    /** A tap on the content (chrome toggle). */
    data object ContentTapped : ReaderSessionEvent

    /** The user tapped a highlight decoration. */
    data class DecorationTapped(val decorationId: String) : ReaderSessionEvent

    /** An external http(s) link was activated in the content. */
    data class ExternalLink(val url: String) : ReaderSessionEvent

    /** A resource failed to load inside the navigator. */
    data object ResourceFailed : ReaderSessionEvent

    /**
     * The user's text selection changed. Non-null [text] is the freshly
     * selected word/phrase (highlight text from the selection locator);
     * null means the selection was cleared (tap, page turn, dismiss).
     */
    data class SelectionChanged(val text: String?) : ReaderSessionEvent
}

/**
 * In-memory reader session, bridging the [EpubNavigatorFragment] (which
 * outlives Compose recompositions and owns the [Publication]) with the
 * [ReaderViewModel]. Single-book: opening another book replaces the session
 * and closes the previous publication.
 *
 * Opening a book is asynchronous (database read, preferences, then a Readium
 * parse), so the UI must never attach the navigator host before the session is
 * published — that race used to hand the fragment a null factory. [sessionReady]
 * is the single source of truth for "safe to attach", and [openInFlight]
 * distinguishes "still opening" from "genuinely lost" so a slow open is never
 * mistaken for process death.
 */
@Singleton
class ReaderSessionStore @Inject constructor() {

    var bookId: String? = null
        private set
    var publication: Publication? = null
        private set
    var navigatorFactory: EpubNavigatorFactory? = null
        private set
    var initialLocator: Locator? = null
        private set
    var initialPreferences: EpubPreferences? = null
        private set

    /** Attached navigator; main-thread only, set by the host fragment. */
    var navigator: EpubNavigatorFragment? = null

    private val _latestLocator = MutableStateFlow<Locator?>(null)
    val latestLocator: StateFlow<Locator?> = _latestLocator

    /**
     * Currently selected word/phrase, from the navigator's selection
     * locator highlight text. Drives the dictionary lookup popup; null
     * when nothing is selected. Owned by the host fragment (writes) and
     * observed by the reader screen/ViewModel (reads).
     */
    private val _selectedText = MutableStateFlow<String?>(null)
    val selectedText: StateFlow<String?> = _selectedText.asStateFlow()

    /** True once [publish] has run; the reader host may attach only then. */
    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady.asStateFlow()

    /** True while an open is running, so a missing factory is not "lost". */
    private val _openInFlight = MutableStateFlow(false)
    val openInFlight: StateFlow<Boolean> = _openInFlight.asStateFlow()

    private val eventChannel = Channel<ReaderSessionEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    /** Marks the start of an open: nothing may attach until [publish]. */
    fun beginOpen() {
        closeCurrent()
        _sessionReady.value = false
        _openInFlight.value = true
    }

    fun publish(
        bookId: String,
        publication: Publication,
        factory: EpubNavigatorFactory,
        initialLocator: Locator?,
        initialPreferences: EpubPreferences,
    ) {
        closeCurrent()
        this.bookId = bookId
        this.publication = publication
        this.navigatorFactory = factory
        this.initialLocator = initialLocator
        this.initialPreferences = initialPreferences
        _sessionReady.value = true
        _openInFlight.value = false
    }

    /** The open finished without a publication (missing or unparseable file). */
    fun failOpen() {
        closeCurrent()
        _sessionReady.value = false
        _openInFlight.value = false
    }

    fun onLocator(locator: Locator) {
        _latestLocator.value = locator
        // A page turn invalidates any active text selection.
        clearSelection()
    }

    /** Records a fresh user selection and notifies popup listeners. */
    fun onSelection(text: String) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        _selectedText.value = cleaned
        tryEmit(ReaderSessionEvent.SelectionChanged(cleaned))
    }

    /** Clears the active selection; no-op (no event) when already empty. */
    fun clearSelection() {
        if (_selectedText.value == null) return
        _selectedText.value = null
        tryEmit(ReaderSessionEvent.SelectionChanged(null))
    }

    suspend fun emit(event: ReaderSessionEvent) {
        eventChannel.send(event)
    }

    fun tryEmit(event: ReaderSessionEvent): Boolean = eventChannel.trySend(event).isSuccess

    fun clear() {
        closeCurrent()
        _sessionReady.value = false
        _openInFlight.value = false
    }

    /** Releases the publication and book fields, leaving flags untouched. */
    private fun closeCurrent() {
        navigator = null
        _latestLocator.value = null
        _selectedText.value = null
        runCatching { publication?.close() }
        publication = null
        navigatorFactory = null
        initialLocator = null
        initialPreferences = null
        bookId = null
    }
}
