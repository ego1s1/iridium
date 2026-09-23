package com.iridium.core.data

/**
 * FTS expression building and result snippets.
 *
 * User input is never handed to `MATCH` verbatim: FTS treats bare words like
 * `AND`/`OR`/`NOT` as operators and `"` as a phrase delimiter, so an
 * unescaped query could throw or match wildly. Terms are reduced to letters
 * and digits, lowercased, and given a prefix `*`.
 */
internal object FtsQuery {

    /** Returns a safe FTS expression, or null when nothing searchable remains. */
    fun build(input: String): String? {
        val terms = input.trim()
            .split(Regex("\\s+"))
            .mapNotNull { term ->
                val cleaned = term.lowercase().filter { it.isLetterOrDigit() }
                cleaned.ifEmpty { null }
            }
        if (terms.isEmpty()) return null
        // Implicit AND between terms; trailing * makes each a prefix match.
        return terms.joinToString(" ") { "$it*" }
    }

    /** A short window of [body] around the first matching term. */
    fun snippet(body: String, input: String, radius: Int = 60): String {
        val haystack = body.lowercase()
        val needle = input.trim()
            .split(Regex("\\s+"))
            .mapNotNull { term -> term.lowercase().filter { it.isLetterOrDigit() }.ifEmpty { null } }
            .maxByOrNull { it.length }
            ?: return body.take(radius * 2).trim()

        val index = haystack.indexOf(needle)
        if (index < 0) return body.take(radius * 2).trim()

        val start = (index - radius).coerceAtLeast(0)
        val end = (index + needle.length + radius).coerceAtMost(body.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < body.length) "…" else ""
        return (prefix + body.substring(start, end).trim() + suffix)
            .replace(Regex("\\s+"), " ")
    }
}
