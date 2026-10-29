package com.iridium.feature.reader.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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

    // Exposed read-only: only this controller may change chrome state.
    private val _chromeVisible = MutableStateFlow(true)
    val chromeVisible: StateFlow<Boolean> = _chromeVisible.asStateFlow()
    private val _settingsOpen = MutableStateFlow(false)
    val settingsOpen: StateFlow<Boolean> = _settingsOpen.asStateFlow()
    private val _themeSheetOpen = MutableStateFlow(false)
    val themeSheetOpen: StateFlow<Boolean> = _themeSheetOpen.asStateFlow()
    private val _tocOpen = MutableStateFlow(false)
    val tocOpen: StateFlow<Boolean> = _tocOpen.asStateFlow()
    private val _highlightsOpen = MutableStateFlow(false)
    val highlightsOpen: StateFlow<Boolean> = _highlightsOpen.asStateFlow()
    private val _focusedHighlightId = MutableStateFlow<String?>(null)
    val focusedHighlightId: StateFlow<String?> = _focusedHighlightId.asStateFlow()

    private var chromeJob: Job? = null

    fun anySheetOpen(): Boolean =
        _settingsOpen.value || _themeSheetOpen.value ||
            _tocOpen.value || _highlightsOpen.value

    fun openSettings() = openSheet(_settingsOpen, pinChrome = true)
    fun closeSettings() = closeSheet(_settingsOpen)

    fun openThemeSheet() = openSheet(_themeSheetOpen, pinChrome = true)
    fun closeThemeSheet() = closeSheet(_themeSheetOpen)

    fun openToc() = openSheet(_tocOpen, pinChrome = false)

    fun openHighlights() = openSheet(_highlightsOpen, pinChrome = false)
    fun closeHighlights() = closeSheet(_highlightsOpen, clearHighlightFocus = true)

    /** Opens a sheet and holds chrome; TOC/highlights only cancel auto-hide. */
    private fun openSheet(sheet: MutableStateFlow<Boolean>, pinChrome: Boolean) {
        sheet.value = true
        if (pinChrome) _chromeVisible.value = true
        chromeJob?.cancel()
    }

    /** Closes a sheet and resumes auto-hide; highlight sheets also clear focus. */
    private fun closeSheet(sheet: MutableStateFlow<Boolean>, clearHighlightFocus: Boolean = false) {
        sheet.value = false
        if (clearHighlightFocus) _focusedHighlightId.value = null
        scheduleHide()
    }

    fun toggle() {
        _chromeVisible.update { !it }
        if (_chromeVisible.value) scheduleHide() else chromeJob?.cancel()
    }

    fun closeToc() = closeSheet(_tocOpen)

    /** User page turns hide the chrome immediately. */
    fun hideForPageTurn() {
        _chromeVisible.value = false
        chromeJob?.cancel()
    }

    /** Tapping a highlight decoration selects it and raises the sheet. */
    fun showHighlightsForDecoration(decorationId: String) {
        _focusedHighlightId.value = decorationId
        _highlightsOpen.value = true
        _chromeVisible.value = false
        chromeJob?.cancel()
    }

    /** Clears focus when its highlight is deleted out from under the sheet. */
    fun clearFocusIf(id: String) {
        if (_focusedHighlightId.value == id) _focusedHighlightId.value = null
    }

    fun scheduleHide() {
        chromeJob?.cancel()
        chromeJob = scope.launch {
            delay(CHROME_AUTO_HIDE_MS)
            if (!anySheetOpen()) {
                _chromeVisible.value = false
            }
        }
    }

    /** Holds chrome open while the scrubber is dragged (cancels auto-hide). */
    fun pinForScrub() {
        chromeJob?.cancel()
        _chromeVisible.value = true
    }

    private companion object {
        const val CHROME_AUTO_HIDE_MS = 3000L
    }
}
