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

enum class HighlightColor(val argb: Int) {
    YELLOW(0xFFFFFF00.toInt()),
    GREEN(0xFF90EE90.toInt()),
    TEAL(0xFF40E0D0.toInt()),
    BLUE(0xFFADD8E6.toInt()),
    RED(0xFFFF7F7F.toInt()),
    PURPLE(0xFFDDA0DD.toInt()),
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
