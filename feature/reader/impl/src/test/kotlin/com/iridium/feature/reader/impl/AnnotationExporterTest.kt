package com.iridium.feature.reader.impl

import com.iridium.core.model.Highlight
import com.iridium.core.model.HighlightColor
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotationExporterTest {

    private fun hl(
        text: String,
        color: HighlightColor = HighlightColor.YELLOW,
        note: String? = null,
    ) = Highlight(id = "1", bookId = "b", href = "x.html", startLocator = "", endLocator = "", selectedText = text, color = color, note = note)

    @Test
    fun `markdown lists highlights with quoted text`() {
        val md = AnnotationExporter.markdown("Odyssey", listOf(hl("Wine-dark sea")))
        assertTrue(md.contains("# Highlights — Odyssey"))
        assertTrue(md.contains("“Wine-dark sea”"))
    }

    @Test
    fun `markdown embeds notes as blockquotes`() {
        val md = AnnotationExporter.markdown("Odyssey", listOf(hl("Wine-dark sea", note = "Homeric epithet")))
        assertTrue(md.contains("> Homeric epithet"))
    }

    @Test
    fun `html escapes and tints`() {
        val html = AnnotationExporter.html("A<B", listOf(hl("x < y", color = HighlightColor.GREEN)))
        assertTrue(html.contains("background:rgba(144, 238, 144,0.35)"))
        assertTrue(html.contains("x &lt; y"))
        assertTrue(html.contains("A&lt;B"))
    }
}
