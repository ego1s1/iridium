package com.iridium.feature.reader.impl

import com.iridium.core.model.ReaderPreferences
import kotlin.math.roundToInt

/** Viewport the book is paginated into; part of the layout fingerprint. */
data class ReaderViewport(val widthDp: Int, val densityDpi: Int)

/** Supplies the current viewport; a seam so the reader stays testable. */
fun interface ReaderViewportProvider {
    fun current(): ReaderViewport
}

/**
 * Fingerprint of everything that changes reflowable pagination: font, size,
 * leading, margins, alignment and the viewport.
 *
 * Deliberately excludes paint-only preferences (extra dim, night light,
 * brightness, volume keys). Those never move a line break, so they must not
 * throw away the learned page counts.
 */
object ReaderLayoutKey {

    fun of(prefs: ReaderPreferences, viewport: ReaderViewport): String = buildString {
        append("w").append(viewport.widthDp)
        append(":d").append(viewport.densityDpi)
        append(":f").append(hundredths(prefs.fontScale))
        append(":fam").append(prefs.fontFamily.name)
        append(":lh").append(hundredths(prefs.lineHeight))
        append(":m").append(hundredths(prefs.pageMargins))
        append(":a").append(prefs.textAlign.name)
    }

    /** Integer hundredths: no locale, no float formatting, stable as a key. */
    private fun hundredths(value: Float): Int = (value * 100).roundToInt()
}
