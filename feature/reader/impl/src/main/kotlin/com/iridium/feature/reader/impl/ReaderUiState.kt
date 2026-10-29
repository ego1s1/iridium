package com.iridium.feature.reader.impl

import androidx.compose.runtime.Immutable
import com.iridium.core.model.Book
import com.iridium.core.model.Highlight
import com.iridium.core.model.CustomReaderTheme
import com.iridium.core.model.HighlightColor
import com.iridium.core.model.ReaderFontFamily
import com.iridium.core.model.TocEntry
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface ReaderUiState {
    data object Loading : ReaderUiState

    data class Ready(
        val book: Book,
        val toc: ImmutableList<TocEntry>,
        val highlights: ImmutableList<Highlight>,
        val prefs: com.iridium.core.model.ReaderPreferences,
        val chromeVisible: Boolean,
        val settingsOpen: Boolean,
        val tocOpen: Boolean,
        val highlightsOpen: Boolean,
        /** 0f..1f progression for the slider/counter. */
        val progression: Float,
        /** "12 / 173" style position text, when positions are known. */
        val positionText: String?,
        /** Highlight id tapped in content (shown in the highlights sheet). */
        val focusedHighlightId: String? = null,
        /** 0-based position index for the scrubber; 0 when positions unknown. */
        val positionIndex: Int = 0,
        /** Total positions for the scrubber; 0 when positions unknown. */
        val positionCount: Int = 0,
        /** Current chapter title (nearest TOC anchor); null before first tick. */
        val chapterTitle: String? = null,
        /** True once the navigator fragment attached (content visible). */
        val navigatorAttached: Boolean = false,
        /**
         * True once the navigator has reported a position, which means the
         * first page is laid out and painted. Used to keep the loading state
         * on screen instead of showing an empty page while the WebView works.
         */
        val contentReady: Boolean = false,
        /** Theme chooser sheet (dock colorscheme button) is open. */
        val themeSheetOpen: Boolean = false,
    ) : ReaderUiState

    /** Book row vanished (removed elsewhere). */
    data object Gone : ReaderUiState

    /** Publication failed to open. [fileMissing] distinguishes a gone or
     * unreadable file (re-link its folder) from a corrupt one. */
    data class OpenFailed(val fileMissing: Boolean = false) : ReaderUiState
}

sealed interface ReaderAction {
    data object Back : ReaderAction
    data object OpenSettings : ReaderAction
    data object CloseSettings : ReaderAction
    data object OpenThemeSheet : ReaderAction
    data object CloseThemeSheet : ReaderAction
    data object OpenToc : ReaderAction
    data object CloseToc : ReaderAction
    data object OpenHighlights : ReaderAction
    data object CloseHighlights : ReaderAction
    data object ExportAnnotations : ReaderAction

    /** Selects a user palette (null clears back to the preset). */
    data class SetCustomTheme(val id: String?) : ReaderAction, PrefsChange

    /** Saves (adds or replaces) a user palette by id. */
    data class SaveCustomTheme(val theme: CustomReaderTheme) : ReaderAction, PrefsChange

    /** Deletes a user palette; clears the selection when active. */
    data class DeleteCustomTheme(val id: String) : ReaderAction, PrefsChange

    data object DismissDictionary : ReaderAction
    data object DismissImage : ReaderAction
    data class SeekTo(val progression: Float) : ReaderAction
    data class GoTocEntry(val entry: TocEntry) : ReaderAction
    data class GoForward(val animated: Boolean = true) : ReaderAction
    data class GoBackward(val animated: Boolean = true) : ReaderAction

    /** Creates a highlight from the current WebView selection. */
    data class AddHighlight(val color: HighlightColor, val note: String?) : ReaderAction
    data class DeleteHighlight(val id: String) : ReaderAction

    // Reader preference edits (persisted + submitted to the navigator).
    // Each also implements PrefsChange so onAction routes them with one
    // `is` check instead of a 17-entry fallthrough list. They stay nested
    // directly under ReaderAction (dual supertypes) so existing
    // `ReaderAction.SetTheme(...)` call sites keep compiling unchanged.
    sealed interface PrefsChange : ReaderAction
    data class SetFlow(val flow: com.iridium.core.model.ReadingFlow) : ReaderAction, PrefsChange
    data class SetFontScale(val scale: Float) : ReaderAction, PrefsChange
    data class SetFontFamily(val family: ReaderFontFamily) : ReaderAction, PrefsChange
    data class SetLineHeight(val lineHeight: Float) : ReaderAction, PrefsChange
    data class SetTextAlign(val align: com.iridium.core.model.TextAlign) : ReaderAction, PrefsChange
    data class SetTheme(val theme: com.iridium.core.model.ColorSchemeChoice) : ReaderAction, PrefsChange
    data class SetBrightness(val brightness: Float) : ReaderAction, PrefsChange
    data class SetKeepScreenOn(val enabled: Boolean) : ReaderAction, PrefsChange
    data object ScrubStarted : ReaderAction
    data class ScrubFinished(val progression: Float) : ReaderAction
    data class SetVolumeKeys(val enabled: Boolean) : ReaderAction, PrefsChange
    data class SetVolumeKeysInverted(val inverted: Boolean) : ReaderAction, PrefsChange
    data class SetInvertTaps(val inverted: Boolean) : ReaderAction, PrefsChange
    data class SetNightLight(val enabled: Boolean) : ReaderAction, PrefsChange
    data class SetNightLightIntensity(val intensity: Float) : ReaderAction, PrefsChange
}

/** One-shot UI messages. */
sealed interface ReaderMessage {
    data class OpenUrl(val url: String) : ReaderMessage
    data class ShareExport(val markdown: String, val html: String, val filename: String) : ReaderMessage
    data object ContentUnavailable : ReaderMessage
    data object SelectTextFirst : ReaderMessage
    data object HighlightSaved : ReaderMessage
    data object Pop : ReaderMessage
}
