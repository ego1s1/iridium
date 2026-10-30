package com.iridium.feature.reader.impl

import com.iridium.core.common.safeCatching
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.resolvedColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubPreferences

/**
 * Reader preference writes and navigator submission.
 *
 * A preference is either structural (font, size, leading, margins, alignment,
 * theme) or cosmetic (extra dim, night light, brightness, hardware keys). Only
 * structural edits reach the navigator.
 *
 * That distinction is the whole point: every write used to re-submit the
 * mapped [EpubPreferences] and then refresh the position table by walking the
 * entire publication, even when nothing about the layout had changed - and the
 * extra-dim slider ran that per drag frame. Cosmetic edits now stop at the CSS
 * injection, and structural edits cost exactly one submit.
 */
internal class ReaderPrefsController(
    private val scope: CoroutineScope,
    private val preferences: IridiumPreferencesDataSource,
    private val store: ReaderSessionStore,
    private val currentPrefs: () -> ReaderPreferences,
) {

    /** Last preferences handed to the navigator; cosmetic edits match this. */
    private var submitted: EpubPreferences? = null

    fun updateReaderPrefs(transform: (ReaderPreferences) -> ReaderPreferences) {
        scope.launch {
            preferences.updateReaderPreferences(transform)
            sync(preferences.readerPreferences.first())
        }
    }

    /**
     * Live preview while a slider is dragged: restyles the page without
     * writing DataStore or repaginating. The value is committed on release,
     * which is what every other slider in the sheet already does.
     */
    fun preview(transform: (ReaderPreferences) -> ReaderPreferences) {
        scope.launch { applyThemeOverride(transform(currentPrefs())) }
    }

    /** The navigator (re)attached: adopt the preferences the factory applied. */
    suspend fun onAttached() {
        submitted = store.initialPreferences
        sync(preferences.readerPreferences.first())
    }

    private suspend fun sync(prefs: ReaderPreferences) {
        val mapped = EpubPreferencesMapper.map(prefs)
        if (mapped != submitted) {
            runCatching { store.navigator?.submitPreferences(mapped) }
            submitted = mapped
        }
        applyThemeOverride(prefs)
    }

    /** Forces the current theme's colors onto the book page via injected CSS. */
    suspend fun applyThemeOverride(prefs: ReaderPreferences = currentPrefs()) {
        val (bg, fg) = prefs.resolvedColors()
        // Night light warms text in CSS (background keeps its exact color,
        // so pure black stays #000000 with OLED pixels off). The Compose
        // scrim below handles non-black themes; see ReaderScreen night-light
        // gate which skips the scrim when the page is pure black.
        // Order matters: warming rebuilds an opaque colour, so the dim alpha
        // has to be applied after it or it would be erased.
        val warmedFg = if (prefs.nightLight) {
            ReaderThemeOverride.warmForeground(fg, prefs.nightLightIntensity)
        } else {
            fg
        }
        val finalFg = if (prefs.extraDim) {
            val capped = prefs.extraDimIntensity.coerceIn(
                0f,
                ReaderThemeOverride.maxDimFor(bg),
            )
            ReaderThemeOverride.dimForeground(warmedFg, capped)
        } else {
            warmedFg
        }
        val fgCss = if (((finalFg ushr 24) and 0xFF) != 0xFF) {
            ReaderThemeOverride.hexWithAlpha(finalFg)
        } else {
            ReaderThemeOverride.hex(finalFg)
        }
        val selection = store.selectionColors.value
        val script = ReaderThemeOverride.injectOverrideScriptColors(
            bgHex = ReaderThemeOverride.hex(bg),
            fgHex = fgCss,
            selectionBgHex = selection?.let { ReaderThemeOverride.hex(it.first) },
            selectionFgHex = selection?.let { ReaderThemeOverride.hex(it.second) },
        )
        safeCatching {
            store.navigator?.evaluateJavascript(script)
        }
    }
}
