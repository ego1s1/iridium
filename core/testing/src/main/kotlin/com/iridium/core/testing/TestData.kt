package com.iridium.core.testing

import com.iridium.core.model.Book
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.HighlightColor
import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemePreferences
import com.iridium.core.model.TocEntry

/** Shared fixtures so tests describe intent, not construction boilerplate. */
object TestData {

    fun book(
        id: String = "book-1",
        title: String = "Treasure Island",
        author: String? = "Robert Louis Stevenson",
        progress: Float = 0f,
        coverPath: String? = null,
        error: com.iridium.core.model.BookError? = null,
        bookmarked: Boolean = false,
        updatedAt: Long = 0L,
        createdAt: Long = 0L,
    ): Book = Book(
        id = id,
        title = title,
        author = author,
        sourcePath = "content://docs/$id.epub",
        coverPath = coverPath,
        progress = progress,
        sourceDisplayName = "$id.epub",
        createdAt = createdAt,
        updatedAt = updatedAt,
        error = error,
        bookmarked = bookmarked,
    )

    val theme = ThemePreferences()
    val reader = ReaderPreferences()
    val libraryDisplay = LibraryDisplay()
    val motion = MotionStyle.EXPRESSIVE

    fun highlight(
        id: String = "hl-1",
        bookId: String = "book-1",
        href: String = "OEBPS/ch1.xhtml",
        text: String = "It was a bright cold day in April",
        color: HighlightColor = HighlightColor.YELLOW,
        note: String? = null,
    ): Highlight = Highlight(
        id = id,
        bookId = bookId,
        href = href,
        startLocator = "{}",
        endLocator = "{}",
        selectedText = text,
        color = color,
        note = note,
    )

    fun bookmark(
        id: String = "bm-1",
        bookId: String = "book-1",
        label: String? = "Chapter 1",
    ): Bookmark = Bookmark(
        id = id,
        bookId = bookId,
        locator = "{}",
        label = label,
    )

    fun tocEntry(
        href: String = "OEBPS/ch1.xhtml",
        title: String = "Chapter 1",
    ): TocEntry = TocEntry(href = href, title = title)
}
