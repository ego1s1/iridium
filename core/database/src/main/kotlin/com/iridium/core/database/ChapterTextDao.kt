package com.iridium.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChapterTextDao {

    @Insert
    suspend fun insertAll(rows: List<ChapterTextEntity>)

    @Query("DELETE FROM chapter_fts WHERE book_id = :bookId")
    suspend fun deleteForBook(bookId: String)

    @Query("SELECT COUNT(*) FROM chapter_fts WHERE book_id = :bookId")
    suspend fun countForBook(bookId: String): Int

    /** FTS `MATCH`; [match] must already be a sanitized FTS expression. */
    @Query(
        "SELECT book_id, href, title, body FROM chapter_fts " +
            "WHERE chapter_fts MATCH :match LIMIT :limit",
    )
    suspend fun search(match: String, limit: Int): List<ChapterSearchRow>

    @Query("DELETE FROM chapter_fts")
    suspend fun clear()
}
