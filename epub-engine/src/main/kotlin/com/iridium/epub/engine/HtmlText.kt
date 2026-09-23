package com.iridium.epub.engine

/**
 * Converts XHTML chapter markup to searchable plain text.
 *
 * A single forward scan, no DOM: it skips `script`/`style` bodies, turns
 * block-level tags into line breaks so words never run together across
 * paragraphs, decodes the common named and numeric entities, and collapses
 * whitespace. Anything malformed degrades to "strip angle brackets", never
 * throws.
 */
object HtmlText {

    /** Block-level tags that should introduce a line break. */
    private val BLOCK_TAGS = setOf(
        "p", "div", "br", "li", "ul", "ol", "tr", "td", "th", "table",
        "h1", "h2", "h3", "h4", "h5", "h6", "section", "article", "header",
        "footer", "nav", "aside", "blockquote", "pre", "figure", "figcaption", "hr",
    )

    /** Tags whose entire content is dropped. */
    private val SKIPPED_TAGS = setOf("script", "style", "head", "title", "svg", "noscript")

    fun toPlainText(html: String): String {
        val out = StringBuilder(html.length / 2)
        var index = 0
        val length = html.length

        while (index < length) {
            val char = html[index]
            if (char != '<') {
                if (char == '&') {
                    val decoded = decodeEntity(html, index)
                    if (decoded != null) {
                        out.append(decoded.first)
                        index += decoded.second
                        continue
                    }
                }
                // Source formatting whitespace is not a paragraph break; only
                // block-level tags introduce newlines.
                out.append(if (char.isWhitespace()) ' ' else char)
                index++
                continue
            }

            // Tag: read its name.
            val tagEnd = html.indexOf('>', index + 1)
            if (tagEnd < 0) {
                // Unbalanced markup: drop the rest rather than emit noise.
                break
            }
            val rawTag = html.substring(index + 1, tagEnd)
            val name = tagName(rawTag)
            val closing = rawTag.startsWith("/")

            if (!closing && name in SKIPPED_TAGS) {
                // Skip the element body entirely.
                val closeTag = "</$name"
                val bodyEnd = html.indexOf(closeTag, tagEnd, ignoreCase = true)
                if (bodyEnd < 0) break
                val gt = html.indexOf('>', bodyEnd)
                index = if (gt < 0) bodyEnd + closeTag.length else gt + 1
                continue
            }

            if (name in BLOCK_TAGS) out.append('\n')
            index = tagEnd + 1
        }

        return collapse(out)
    }

    private fun tagName(rawTag: String): String {
        var start = 0
        if (start < rawTag.length && rawTag[start] == '/') start++
        var end = start
        while (end < rawTag.length) {
            val c = rawTag[end]
            if (c.isWhitespace() || c == '/' || c == '>') break
            end++
        }
        return rawTag.substring(start, end).lowercase()
    }

    private fun collapse(text: StringBuilder): String {
        val out = StringBuilder(text.length)
        for (c in text) {
            when {
                c == '\n' -> {
                    // Drop trailing spaces and avoid blank lines.
                    while (out.isNotEmpty() && out.last() == ' ') out.deleteCharAt(out.length - 1)
                    if (out.isNotEmpty() && out.last() != '\n') out.append('\n')
                }
                c.isWhitespace() -> {
                    if (out.isNotEmpty() && out.last() != ' ' && out.last() != '\n') out.append(' ')
                }
                else -> out.append(c)
            }
        }
        return out.toString().trim()
    }

    /** Returns the decoded char and the number of source chars consumed. */
    private fun decodeEntity(html: String, start: Int): Pair<Char, Int>? {
        val semicolon = html.indexOf(';', start + 1)
        if (semicolon < 0 || semicolon - start > 10) return null
        val body = html.substring(start + 1, semicolon)
        val decoded = when {
            body.startsWith("#x") || body.startsWith("#X") ->
                body.substring(2).toIntOrNull(16)?.let { it.toChar() }
            body.startsWith("#") ->
                body.substring(1).toIntOrNull()?.let { it.toChar() }
            else -> NAMED_ENTITIES[body]
        } ?: return null
        return decoded to (semicolon - start + 1)
    }

    private val NAMED_ENTITIES: Map<String, Char> = mapOf(
        "amp" to '&',
        "lt" to '<',
        "gt" to '>',
        "quot" to '"',
        "apos" to '\'',
        "nbsp" to ' ',
        "mdash" to '—',
        "ndash" to '–',
        "hellip" to '…',
        "lsquo" to '‘',
        "rsquo" to '’',
        "ldquo" to '“',
        "rdquo" to '”',
        "copy" to '©',
        "deg" to '°',
        "middot" to '·',
        "eacute" to 'é',
        "egrave" to 'è',
        "agrave" to 'à',
        "ccedil" to 'ç',
        "ouml" to 'ö',
        "uuml" to 'ü',
        "auml" to 'ä',
        "szlig" to 'ß',
    )
}
