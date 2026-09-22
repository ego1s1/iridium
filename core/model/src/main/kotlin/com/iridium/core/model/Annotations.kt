package com.iridium.core.model

/** A highlight anchored to a stable EPUB locator (href + progression range). */
data class Highlight(
    val id: String,
    val bookId: String,
    val href: String,
    val startLocator: String,
    val endLocator: String,
    val selectedText: String,
    val color: HighlightColor = HighlightColor.YELLOW,
    val note: String? = null,
    val createdAt: Long = 0L,
)

enum class HighlightColor {
    YELLOW,
    GREEN,
    TEAL,
    BLUE,
    RED,
    PURPLE,
}

/** A bookmark at a stable EPUB locator. */
data class Bookmark(
    val id: String,
    val bookId: String,
    val locator: String,
    val label: String?,
    val createdAt: Long = 0L,
)

/** A table-of-contents entry parsed from the EPUB navigation document. */
data class TocEntry(
    val href: String,
    val title: String,
)
