package com.iridium.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * Layout cache for the reader: one row per book per typography.
 *
 * Reflowable pagination is only known exactly once the navigator has laid a
 * chapter out, so the counts are learned while reading and kept here to keep
 * "page x of y" identical on the next open instead of re-deriving the total
 * from whichever chapter happens to be open.
 */
@Dao
interface ChapterLayoutDao {

    @Query("SELECT * FROM chapter_layouts WHERE bookId = :bookId AND layoutKey = :layoutKey")
    suspend fun get(bookId: String, layoutKey: String): ChapterLayoutEntity?

    @Upsert
    suspend fun upsert(row: ChapterLayoutEntity)

    @Query("DELETE FROM chapter_layouts WHERE bookId = :bookId")
    suspend fun deleteForBook(bookId: String)

    /**
     * Keeps only the [keep] most recently written layouts for a book. A
     * slider can mint many fingerprints, and each one would otherwise live
     * forever.
     */
    @Query(
        "DELETE FROM chapter_layouts WHERE bookId = :bookId AND layoutKey NOT IN " +
            "(SELECT layoutKey FROM chapter_layouts WHERE bookId = :bookId " +
            "ORDER BY updatedAt DESC LIMIT :keep)",
    )
    suspend fun prune(bookId: String, keep: Int)
}
