package com.iridium.feature.reader.impl

import com.iridium.core.model.Book
import com.iridium.core.model.Highlight
import com.iridium.core.model.HighlightColor
import com.iridium.core.model.TocEntry

sealed interface ReaderUiState {
    data object Loading : ReaderUiState

    data class Ready(
        val book: Book,
        val toc: List<TocEntry>,
        val highlights: List<Highlight>,
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
        /** True once the navigator fragment attached (content visible). */
        val navigatorAttached: Boolean = false,
    ) : ReaderUiState

    /** Book row vanished (removed elsewhere). */
    data object Gone : ReaderUiState

    /** Publication failed to open. */
    data object OpenFailed : ReaderUiState
}

sealed interface ReaderAction {
    data object Back : ReaderAction
    data object ContentTapped : ReaderAction
    data object OpenSettings : ReaderAction
    data object CloseSettings : ReaderAction
    data object OpenToc : ReaderAction
    data object CloseToc : ReaderAction
    data object OpenHighlights : ReaderAction
    data object CloseHighlights : ReaderAction
    data object DismissDictionary : ReaderAction
    data class SeekTo(val progression: Float) : ReaderAction
    data class GoTocEntry(val entry: TocEntry) : ReaderAction
    data class GoForward(val animated: Boolean = true) : ReaderAction
    data class GoBackward(val animated: Boolean = true) : ReaderAction

    /** Creates a highlight from the current WebView selection. */
    data class AddHighlight(val color: HighlightColor, val note: String?) : ReaderAction
    data class DeleteHighlight(val id: String) : ReaderAction

    // Reader preference edits (persisted + submitted to the navigator).
    data class SetFlow(val flow: com.iridium.core.model.ReadingFlow) : ReaderAction
    data class SetFontScale(val scale: Float) : ReaderAction
    data class SetLineHeight(val lineHeight: Float) : ReaderAction
    data class SetTextAlign(val align: com.iridium.core.model.TextAlign) : ReaderAction
    data class SetTheme(val theme: com.iridium.core.model.ColorSchemeChoice) : ReaderAction
    data class SetBrightness(val brightness: Float) : ReaderAction
    data class SetKeepScreenOn(val enabled: Boolean) : ReaderAction
    data class SetShowPageCounter(val enabled: Boolean) : ReaderAction
    data class SetVolumeKeys(val enabled: Boolean) : ReaderAction
    data class SetVolumeKeysInverted(val inverted: Boolean) : ReaderAction
    data class SetTapZoneMode(val mode: com.iridium.core.model.TapZoneMode) : ReaderAction
    data class SetTapZoneInvert(val mode: com.iridium.core.model.TapInvertMode) : ReaderAction
    data class SetNightLight(val enabled: Boolean) : ReaderAction
    data class SetNightLightIntensity(val intensity: Float) : ReaderAction
}

/** One-shot UI messages. */
sealed interface ReaderMessage {
    data class Text(val text: String) : ReaderMessage
    data class OpenUrl(val url: String) : ReaderMessage
    data object SelectTextFirst : ReaderMessage
    data object HighlightSaved : ReaderMessage
    data object Pop : ReaderMessage
}
