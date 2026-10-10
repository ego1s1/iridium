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
    fun `comments and doctypes never leak into prose`() {
        assertEquals("Hello world.", HtmlText.toPlainText("Hello <!-- a > b -->world."))
        assertEquals("Hi.", HtmlText.toPlainText("<!DOCTYPE html><p>Hi.</p>"))
        assertEquals("Hi.", HtmlText.toPlainText("<?xml version=\"1.0\"?><p>Hi.</p>"))
    }

    @Test
    fun `unclosed script keeps the rest of the chapter`() {
        val text = HtmlText.toPlainText("<p>First</p><script>var x = 1;<p>Second</p>")
        assertTrue(text.contains("First"))
        assertTrue(text.contains("Second"))
    }

    @Test
    fun `non-bmp entities decode to real characters`() {
        // U+1F600 grinning face: must survive as one code point, not surrogates.
        val text = HtmlText.toPlainText("A&#128512;B")
        assertEquals("A\uD83D\uDE00B", text)
        assertEquals(4, text.length)
    }

    @Test
    fun `invalid code points are left literal, never mojibake`() {
        // Out-of-range numerics are not valid entities: keep the raw text
        // (browser behavior) rather than emitting lone surrogates.
        assertEquals("A&#x110000;B", HtmlText.toPlainText("A&#x110000;B"))
        assertEquals("A&#-1;B", HtmlText.toPlainText("A&#-1;B"))
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
