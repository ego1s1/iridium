package com.iridium.core.testing

import com.iridium.core.model.Book
import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemePreferences

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
}
