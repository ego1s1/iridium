package com.iridium.feature.library.impl

/** Test tags for the library screen. */
object LibraryTestTags {
    const val Grid = "libraryGrid"
    const val SortFilterSheet = "librarySortFilterSheet"
    const val EmptyState = "libraryEmpty"
    const val Snackbar = "librarySnackbar"

    const val NowReadingHero = "libraryNowReadingHero"
    const val HeroResume = "libraryHeroResume"
    const val HeroDetails = "libraryHeroDetails"

    const val QuickFilterCapsule = "libraryQuickFilterCapsule"

    fun quickFilterChip(filter: com.iridium.core.model.LibraryFilter): String =
        "libraryQuickFilter:${filter.name}"

    fun cardFor(id: String): String = "libraryCard:$id"

    fun bookmarkBadgeFor(id: String): String = "libraryBookmark:$id"

    const val MenuSheet = "libraryMenuSheet"
    const val MenuTitle = "libraryMenuTitle"
    const val MenuRead = "libraryMenuRead"
    const val MenuDetails = "libraryMenuDetails"
    const val MenuBookmark = "libraryMenuBookmark"
    const val MenuDelete = "libraryMenuDelete"
    const val MenuDeleteDialog = "libraryMenuDeleteDialog"
    const val MenuDeleteConfirm = "libraryMenuDeleteConfirm"
}
