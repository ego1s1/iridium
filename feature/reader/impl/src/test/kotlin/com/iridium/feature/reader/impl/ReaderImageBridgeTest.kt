package com.iridium.feature.reader.impl

import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderImageBridgeTest {

    @Test
    fun `image tap script reports src to the bridge`() {
        val js = ReaderImageBridge.imageTapScript()
        assertTrue(js.contains("querySelectorAll('img')"))
        assertTrue(js.contains("IridiumReader.onImageTap"))
        assertTrue(js.contains("currentSrc || this.src"))
    }

    @Test
    fun `script binds each image once`() {
        val js = ReaderImageBridge.imageTapScript()
        assertTrue(js.contains("data-iridium-image-bound"))
        // Link-wrapped images defer to normal link handling.
        assertTrue(js.contains("closest('a[href]')"))
    }
}
