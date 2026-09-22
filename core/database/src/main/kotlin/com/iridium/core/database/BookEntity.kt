package com.iridium.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Indexed book row. The source of truth for the library UI; EPUB bytes live
 * in app-private storage, covers as extracted image files.
 */
@Entity(tableName = "books")
data class BookEntity(
    /** Stable id (UUID) assigned at import. */
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    /** [com.iridium.core.model.BookFormat] name. */
    val format: String,
    val spineCount: Int,
    val sourcePath: String,
    val coverPath: String?,
    /** 0f..1f reading progress (Readium locator progression). */
    val progress: Float,
    /** Last stable locator JSON (href + progression), for resume. */
    val lastLocator: String?,
    val sourceDisplayName: String,
    /** Chapter TOC as JSON ([TocEntryDto] list), for the detail screen. */
    val tocJson: String?,
    /** [com.iridium.core.model.BookError] name, or null when healthy. */
    val error: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val bookmarked: Boolean = false,
)
