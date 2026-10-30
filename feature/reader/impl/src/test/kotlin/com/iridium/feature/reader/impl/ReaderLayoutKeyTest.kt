package com.iridium.feature.reader.impl

import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.TextAlign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The layout fingerprint decides when the learned page counts are thrown
 * away. Only settings that actually move a line break may change it: a paint
 * change that invalidated them would make the page total jump for no reason.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderLayoutKeyTest {

    private val viewport = ReaderViewport(widthDp = 400, densityDpi = 420)

    @Test
    fun `paint-only preferences keep the same fingerprint`() {
        val base = ReaderPreferences()
        val painted = base.copy(
            extraDim = true,
            extraDimIntensity = 0.8f,
            nightLight = true,
            nightLightIntensity = 0.4f,
            brightness = 0.3f,
            volumeKeys = true,
            keepScreenOn = true,
            invertTaps = true,
        )

        assertEquals(
            ReaderLayoutKey.of(base, viewport),
            ReaderLayoutKey.of(painted, viewport),
        )
    }

    @Test
    fun `typography changes the fingerprint`() {
        val base = ReaderPreferences()
        val key = ReaderLayoutKey.of(base, viewport)

        assertNotEquals(key, ReaderLayoutKey.of(base.copy(fontScale = base.fontScale + 0.1f), viewport))
        assertNotEquals(key, ReaderLayoutKey.of(base.copy(lineHeight = base.lineHeight + 0.1f), viewport))
        assertNotEquals(key, ReaderLayoutKey.of(base.copy(pageMargins = base.pageMargins + 0.1f), viewport))
        assertNotEquals(key, ReaderLayoutKey.of(base.copy(textAlign = TextAlign.LEFT), viewport))
    }

    @Test
    fun `viewport changes the fingerprint`() {
        val base = ReaderPreferences()
        assertNotEquals(
            ReaderLayoutKey.of(base, viewport),
            ReaderLayoutKey.of(base, ReaderViewport(widthDp = 800, densityDpi = 420)),
        )
    }

    @Test
    fun `the same settings produce the same key`() {
        val prefs = ReaderPreferences(fontScale = 1.3f, lineHeight = 1.6f)
        assertEquals(
            ReaderLayoutKey.of(prefs, viewport),
            ReaderLayoutKey.of(prefs.copy(brightness = 0.9f), viewport),
        )
    }
}
