package com.iridium.epub.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlTextTest {

    @Test
    fun `strips tags and keeps text`() {
        val text = HtmlText.toPlainText("<html><body><p>Hello <em>world</em>.</p></body></html>")
        assertEquals("Hello world.", text)
    }

    @Test
    fun `block tags separate paragraphs`() {
        val text = HtmlText.toPlainText("<p>One</p><p>Two</p>")
        assertEquals("One\nTwo", text)
    }

    @Test
    fun `script and style bodies are dropped`() {
        val html = """
            <html><head><title>Ignored</title><style>p{color:red}</style></head>
            <body>Visible<script>var x = 1;</script></body></html>
        """.trimIndent()
        val text = HtmlText.toPlainText(html)
        assertEquals("Visible", text)
    }

    @Test
    fun `entities are decoded`() {
        assertEquals("Tom & Jerry <3", HtmlText.toPlainText("Tom &amp; Jerry &lt;3"))
        assertEquals("a b", HtmlText.toPlainText("a&nbsp;b"))
        assertEquals("—", HtmlText.toPlainText("&mdash;"))
        assertEquals("é", HtmlText.toPlainText("&#233;"))
    }

    @Test
    fun `whitespace is collapsed`() {
        val text = HtmlText.toPlainText("<p>  lots   of\n\n\n   space  </p>")
        assertEquals("lots of space", text)
    }

    @Test
    fun `malformed markup does not throw`() {
        assertEquals("start", HtmlText.toPlainText("start<p"))
        assertTrue(HtmlText.toPlainText("").isEmpty())
        assertFalse(HtmlText.toPlainText("<p></p>").contains("<"))
    }

    @Test
    fun `chapter text is searchable prose`() {
        val chapter = """
            <?xml version="1.0"?>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>Ch 1</title></head>
            <body>
              <h1>Chapter One</h1>
              <p>The Hispaniola was rolling scuppers under in the ocean swell.</p>
            </body>
            </html>
        """.trimIndent()
        val text = HtmlText.toPlainText(chapter)
        assertTrue(text.contains("Hispaniola"))
        assertFalse(text.contains("<"))
    }
}
