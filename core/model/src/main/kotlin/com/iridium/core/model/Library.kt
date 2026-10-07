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
) {
    /** True when any sort/filter deviates from the defaults (drives the filter dot). */
    fun hasActiveFilters(): Boolean =
        sortOrder != LibrarySortOrder.RECENTLY_ADDED ||
            filter != LibraryFilter.ALL ||
            hideErrors
}

/**
 * Persisted library display options: sort, filter, error visibility, card
 * density, and grid width. Unlike the ephemeral search text, these survive
 * full app restarts.
 */
enum class LibraryDisplayMode {
    COMPACT,
    COMFORTABLE,
    COVER_ONLY,
    LIST,
}

data class LibraryDisplay(
    val sortOrder: LibrarySortOrder = LibrarySortOrder.RECENTLY_ADDED,
    val filter: LibraryFilter = LibraryFilter.ALL,
    val hideErrors: Boolean = false,
    val displayMode: LibraryDisplayMode = LibraryDisplayMode.COMPACT,
    val gridColumns: Int = 0,
) {
    fun toQuery(text: String): LibraryQuery = LibraryQuery(
        text = text,
        sortOrder = sortOrder,
        filter = filter,
        hideErrors = hideErrors,
    )
}
