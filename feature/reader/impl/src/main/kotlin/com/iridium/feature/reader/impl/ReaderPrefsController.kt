package com.iridium.feature.reader.impl

import com.iridium.core.common.safeCatching
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.resolvedColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.services.positions

/**
 * Reader-preference writes + navigator submission. Every write re-submits
 * the mapped Readium preferences and re-injects the forced-override theme
 * stylesheet so exact colors win over the book's own CSS.
 */
internal class ReaderPrefsController(
    private val scope: CoroutineScope,
    private val preferences: IridiumPreferencesDataSource,
    private val store: ReaderSessionStore,
    private val positions: ReaderPositionTracker,
) {

    fun updateReaderPrefs(transform: (ReaderPreferences) -> ReaderPreferences) {
        scope.launch {
            preferences.updateReaderPreferences(transform)
            val updated = EpubPreferencesMapper.map(preferences.readerPreferences.first())
            runCatching { store.navigator?.submitPreferences(updated) }
            applyThemeOverride()
            // Pagination moves under the slider after font/margin/line/flow
            // changes: refresh the positions table, then re-measure the
            // rendered page count so the counter follows the new typography.
            val fresh = safeCatching { store.publication?.positions() }.getOrNull()
            if (fresh != null) positions.replaceAll(fresh)
            positions.invalidatePagination()
            delay(PAGINATION_SETTLE_MS)
            positions.measurePagination()
        }
    }

    /** Forces the current theme's colors onto the book page via injected CSS. */
    suspend fun applyThemeOverride() {
        val prefs = preferences.readerPreferences.first()
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

    private companion object {
        /** Time for the navigator to re-lay out before measuring its pages. */
        const val PAGINATION_SETTLE_MS = 350L
    }
}
