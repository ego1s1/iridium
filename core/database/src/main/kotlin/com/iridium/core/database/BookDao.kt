package com.iridium.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE sourcePath = :sourcePath LIMIT 1")
    suspend fun getBySourcePath(sourcePath: String): BookEntity?

    /** Whole table in one round trip for batch rescans (no per-file queries). */
    @Query("SELECT * FROM books")
    suspend fun getAll(): List<BookEntity>

    @Query("SELECT id FROM books")
    suspend fun getIds(): List<String>

    @Upsert
    suspend fun upsert(book: BookEntity)

    @Upsert
    suspend fun upsertAll(books: List<BookEntity>)

    @Query("UPDATE books SET progress = :progress, lastLocator = :locator, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, locator: String?, updatedAt: Long)

    @Query("UPDATE books SET bookmarked = :bookmarked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBookmark(id: String, bookmarked: Boolean, updatedAt: Long)

    @Query("UPDATE books SET coverPath = NULL")
    suspend fun clearCovers()

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Prunes linked rows no longer present in their source tree. The app
     * links a single tree, so every linked row belongs to the pass that just
     * ran; anything not re-found was deleted out from under us.
     */
    @Query("DELETE FROM books WHERE sourcePath LIKE 'content://%' AND id NOT IN (:ids)")
    suspend fun deleteMissingLinked(ids: List<String>)

    @Query("DELETE FROM books WHERE sourcePath LIKE 'content://%'")
    suspend fun deleteAllLinked()
}
