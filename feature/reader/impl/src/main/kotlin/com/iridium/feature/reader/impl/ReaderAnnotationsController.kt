package com.iridium.feature.reader.impl

import com.iridium.core.common.AppClock
import com.iridium.core.common.safeCatching
import com.iridium.core.data.BooksRepository
import com.iridium.core.data.LocatorStrings
import com.iridium.core.model.Highlight
import com.iridium.core.model.HighlightColor
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.r2.navigator.Decoration

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
                safeCatching { it.currentSelection() }.getOrNull()
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

    /** Parsed decorations by highlight id: Room re-emits the whole list on every upsert, so only newly added highlights pay for parsing. */
    private val decorationCache = mutableMapOf<String, Decoration>()

    /** Re-submits decorations for a fresh navigator (attach drops the old one). */
    suspend fun refreshDecorations(highlights: List<Highlight>) {
        applyDecorations(highlights)
    }

    private suspend fun applyDecorations(highlights: List<Highlight>) {
        val nav = store.navigator ?: return
        // Evict decorations whose highlights are gone before rebuilding.
        decorationCache.keys.retainAll(highlights.mapTo(mutableSetOf()) { it.id })
        val decorations = highlights.mapNotNull { h ->
            decorationCache.getOrPut(h.id) {
                val locator = LocatorStrings.decode(h.startLocator)
                    ?: return@mapNotNull null
                Decoration(
                    id = h.id,
                    locator = locator,
                    style = Decoration.Style.Highlight(tint = highlightTint(h.color)),
                )
            }
        }
        safeCatching { nav.applyDecorations(decorations, DECORATION_GROUP) }
    }

    private companion object {
        const val DECORATION_GROUP = "highlights"
    }
}
