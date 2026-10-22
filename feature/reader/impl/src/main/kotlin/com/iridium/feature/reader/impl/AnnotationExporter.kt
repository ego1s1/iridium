package com.iridium.feature.reader.impl

import com.iridium.core.model.Highlight

/**
 * Export of a book's highlights + notes. Pure string building,
 * unit-tested; the caller ships the text via a share/SAF intent.
 */
object AnnotationExporter {

    /** Markdown for a share sheet / plain export. */
    fun markdown(bookTitle: String, highlights: List<Highlight>): String = buildString {
        append("# Highlights — ").append(bookTitle).append('\n')
        if (highlights.isEmpty()) {
            append("\n_No highlights._\n")
            return@buildString
        }
        highlights.forEach { h ->
            append("\n- ")
            appendQuote(h.selectedText)
            h.note?.takeIf { it.isNotBlank() }?.let { note ->
                append("\n  > ").append(note)
            }
            append('\n')
        }
    }

    /** HTML with per-highlight background colors. */
    fun html(bookTitle: String, highlights: List<Highlight>): String = buildString {
        append("<!doctype html><html><head><meta charset=\"utf-8\">")
        append("<title>Highlights — ").append(escape(bookTitle)).append("</title>")
        append("<style>body{font-family:sans-serif;max-width:40em;margin:2em auto;line-height:1.6}")
        append(".h{white-space:pre-wrap;padding:.2em .4em;border-radius:4px;margin:.4em 0}")
        append(".n{color:#555;font-style:italic}</style></head><body>")
        append("<h1>Highlights — ").append(escape(bookTitle)).append("</h1>")
        if (highlights.isEmpty()) {
            append("<p><em>No highlights.</em></p>")
        } else {
            highlights.forEach { h ->
                val color = hexFor(h.color)
                append("<div class=\"h\" style=\"background:rgba(")
                append(rgb(color)).append(",0.35)\">")
                append(escape(h.selectedText))
                h.note?.takeIf { it.isNotBlank() }?.let { note ->
                    append("<div class=\"n\">").append(escape(note)).append("</div>")
                }
                append("</div>")
            }
        }
        append("</body></html>")
    }

    private fun StringBuilder.appendQuote(s: String) {
        val lines = s.lines()
        if (lines.size == 1) append("“").append(lines[0]).append("”") else {
            append(">\n")
            lines.forEach { append("> ").append(it).append('\n') }
        }
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    /** #RRGGBB from the highlight's tint. */
    private fun rgb(argb: Int): String =
        listOf(16, 8, 0).map { shift -> (argb shr shift) and 0xFF }.joinToString(", ")

    private fun hexFor(color: com.iridium.core.model.HighlightColor): Int = color.argb
}
