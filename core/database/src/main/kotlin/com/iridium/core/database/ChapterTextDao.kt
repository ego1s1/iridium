package com.iridium.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ChapterTextDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<ChapterTextEntity>)

    /** Atomic re-index: crash between delete and insert can no longer empty the index. */
    @Transaction
    suspend fun replaceForBook(bookId: String, rows: List<ChapterTextEntity>) {
        deleteForBook(bookId)
        if (rows.isNotEmpty()) insertAll(rows)
    }

    @Query("DELETE FROM chapter_fts WHERE book_id IN (:ids)")
    suspend fun deleteForBooks(ids: List<String>)

    @Query("DELETE FROM chapter_fts WHERE book_id = :bookId")
    suspend fun deleteForBook(bookId: String)

    @Query("SELECT COUNT(*) FROM chapter_fts WHERE book_id = :bookId")
    suspend fun countForBook(bookId: String): Int

    /** FTS `MATCH`; [match] must already be a sanitized FTS expression. */
    @Query(
        "SELECT chapter_fts.book_id AS book_id, books.title AS book_title, " +
            "chapter_fts.href AS href, chapter_fts.title AS title, " +
            "snippet(chapter_fts, '', '', '…', 3, 18) AS snippet " +
            "FROM chapter_fts INNER JOIN books ON books.id = chapter_fts.book_id " +
            "WHERE chapter_fts MATCH :match ORDER BY chapter_fts.rowid LIMIT :limit",
    )
    suspend fun search(match: String, limit: Int): List<ChapterSearchRow>

    @Query("DELETE FROM chapter_fts")
    suspend fun clear()
}
