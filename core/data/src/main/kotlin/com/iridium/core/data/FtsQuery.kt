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

    private val whitespace = Regex("\\s+")

    /** Returns a safe FTS expression, or null when nothing searchable remains. */
    fun build(input: String): String? {
        val terms = input.trim()
            .split(whitespace)
            .mapNotNull { term ->
                val cleaned = term.lowercase().filter { it.isLetterOrDigit() }
                cleaned.ifEmpty { null }
            }
        if (terms.isEmpty()) return null
        // Implicit AND between terms; trailing * makes each a prefix match.
        return terms.joinToString(" ") { "$it*" }
    }
}
