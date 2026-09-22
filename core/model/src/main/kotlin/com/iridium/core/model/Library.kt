package com.iridium.core.model

/** How the library grid is ordered. */
enum class LibrarySortOrder {
    RECENTLY_ADDED,
    RECENTLY_READ,
    TITLE,
    UNFINISHED_FIRST,
}

/** Which subset of the library is shown. */
enum class LibraryFilter {
    ALL,
    IN_PROGRESS,
    UNREAD,
    FINISHED,
    FAVORITES,
}

/** A text query plus sort/filter preferences for the library grid. */
data class LibraryQuery(
    val text: String = "",
    val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENTLY_ADDED,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val hideErrors: Boolean = false,
)

/**
 * Persisted library display options: sort, filter, and error visibility.
 * Unlike the ephemeral search text, these survive full app restarts.
 */
data class LibraryDisplay(
    val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENTLY_ADDED,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val hideErrors: Boolean = false,
) {
    fun toQuery(text: String): LibraryQuery = LibraryQuery(
        text = text,
        sortOrder = sortOrder,
        filter = filter,
        hideErrors = hideErrors,
    )
}
