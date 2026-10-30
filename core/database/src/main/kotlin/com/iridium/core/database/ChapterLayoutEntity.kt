package com.iridium.core.database

import androidx.room.Entity

/**
 * Rendered page counts for one book at one typography, keyed by a layout
 * fingerprint (see `ReaderLayoutKey` in the reader feature).
 *
 * A reflowable book has no page count until it is laid out, and the layout
 * depends on font, margins, alignment and viewport. The navigator reports each
 * chapter's rendered pages as it paints them; remembering them turns a
 * per-session estimate into a stable, book-wide number that survives reopening
 * the book and restarting the app.
 */
@Entity(tableName = "chapter_layouts", primaryKeys = ["bookId", "layoutKey"])
data class ChapterLayoutEntity(
    val bookId: String,
    /** Fingerprint of everything that changes pagination. */
    val layoutKey: String,
    /** JSON object: chapter href to rendered page count. */
    val pagesJson: String,
    val updatedAt: Long,
)
