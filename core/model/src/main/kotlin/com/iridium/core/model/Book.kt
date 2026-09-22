package com.iridium.core.model

/** Reading flow for the EPUB navigator. Mirrors Lithium's Auto/Paged/Scrolled. */
enum class ReadingFlow { AUTO, PAGED, SCROLLED }

enum class BookFormat { EPUB }

enum class BookError { CORRUPT, EMPTY, UNSUPPORTED }

enum class MotionStyle { EXPRESSIVE, CALM }

/**
 * A book in the user's library. Pure Kotlin, no Android dependencies.
 * Progress is a stable 0f..1f fraction (Readium locator progression),
 * never a page index — reflowable EPUBs have no fixed pages.
 */
data class Book(
    val id: String,
    val title: String,
    val author: String?,
    val format: BookFormat = BookFormat.EPUB,
    val spineCount: Int = 0,
    val sourcePath: String,
    val coverPath: String?,
    val progress: Float = 0f,
    val lastLocator: String? = null,
    val sourceDisplayName: String,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val error: BookError? = null,
    val bookmarked: Boolean = false,
) {
    /** True when the user has started but not finished this book. */
    val isInProgress: Boolean
        get() = progress > 0f && progress < 1f

    /** True when the user has reached the end. */
    val isFinished: Boolean
        get() = progress >= 1f
}

/** In-progress books by recency, backing the continue shelf (max 10). */
fun List<Book>.continueShelf(max: Int = 10): List<Book> =
    filter { it.isInProgress }.sortedByDescending { it.updatedAt }.take(max)

/**
 * Most recently touched readable book. Errored rows and untouched books
 * never win, so resume can't deep-link into something the reader rejects.
 */
fun List<Book>.resumeTarget(): Book? =
    filter { it.error == null && it.progress > 0f }.maxByOrNull { it.updatedAt }

/** Applies a text/sort/filter query to an in-memory book list. */
fun List<Book>.applyQuery(query: LibraryQuery): List<Book> {
    var result = this
    if (query.hideErrors) result = result.filter { it.error == null }
    when (query.filter) {
        LibraryFilter.ALL -> Unit
        LibraryFilter.IN_PROGRESS -> result = result.filter { it.isInProgress }
        LibraryFilter.UNREAD -> result = result.filter { it.progress <= 0f }
        LibraryFilter.FINISHED -> result = result.filter { it.isFinished }
        LibraryFilter.FAVORITES -> result = result.filter { it.bookmarked }
    }
    val text = query.text.trim()
    if (text.isNotBlank()) {
        result = result.filter {
            it.title.contains(text, ignoreCase = true) ||
                (it.author?.contains(text, ignoreCase = true) == true)
        }
    }
    result = when (query.sortOrder) {
        LibrarySortOrder.RECENTLY_ADDED -> result.sortedByDescending { it.createdAt }
        LibrarySortOrder.RECENTLY_READ -> result.sortedByDescending { it.updatedAt }
        LibrarySortOrder.TITLE -> result.sortedBy { it.title.lowercase() }
        LibrarySortOrder.UNFINISHED_FIRST ->
            result.sortedWith(compareBy({ it.isFinished }, { -it.updatedAt }))
    }
    return result
}
