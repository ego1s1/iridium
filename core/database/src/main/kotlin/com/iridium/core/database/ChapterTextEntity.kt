package com.iridium.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Full-text index over a book's chapters. One row per spine document, holding
 * the plain text of that chapter so search can match inside the prose rather
 * than only on titles.
 *
 * FTS4 rather than a normal table: `MATCH` is orders of magnitude faster than
 * `LIKE '%term%'` once a library has thousands of chapters, and it is available
 * on every API level this app supports.
 */
@Fts4
@Entity(tableName = "chapter_fts")
data class ChapterTextEntity(
    /** FTS rowid; 0 lets SQLite assign one on insert. */
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long = 0,
    @ColumnInfo(name = "book_id")
    val bookId: String,
    @ColumnInfo(name = "href")
    val href: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "body")
    val body: String,
)

/** One search hit with a bounded excerpt, not the full indexed chapter body. */
data class ChapterSearchRow(
    @ColumnInfo(name = "book_id") val bookId: String,
    @ColumnInfo(name = "book_title") val bookTitle: String,
    val href: String,
    val title: String,
    val snippet: String,
)
