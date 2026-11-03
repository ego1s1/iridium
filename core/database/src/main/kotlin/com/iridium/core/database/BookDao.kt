package com.iridium.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    fun observeAll(): Flow<List<BookEntity>>

    /** Applies the library's non-text filters and ordering before rows are mapped. */
    @Query(
        "SELECT id, title, author, format, spineCount, sourcePath, coverPath, progress, " +
            "lastLocator, sourceDisplayName, sourceModified, createdAt, updatedAt, error, bookmarked " +
            "FROM books " +
            "WHERE (:hideErrors = 0 OR error IS NULL) " +
            "AND (:filter = 'ALL' " +
            "OR (:filter = 'IN_PROGRESS' AND progress > 0 AND progress < 1) " +
            "OR (:filter = 'UNREAD' AND progress <= 0 AND error IS NULL) " +
            "OR (:filter = 'FINISHED' AND progress >= 1) " +
            "OR (:filter = 'FAVORITES' AND bookmarked = 1)) " +
            "ORDER BY " +
            "CASE WHEN :sortOrder = 'RECENTLY_ADDED' THEN createdAt END DESC, " +
            "CASE WHEN :sortOrder = 'RECENTLY_READ' THEN updatedAt END DESC, " +
            "CASE WHEN :sortOrder = 'TITLE' THEN title COLLATE NOCASE END ASC, " +
            "CASE WHEN :sortOrder = 'UNFINISHED_FIRST' " +
                "THEN CASE WHEN progress >= 1 THEN 1 ELSE 0 END END ASC, " +
            "CASE WHEN :sortOrder = 'UNFINISHED_FIRST' THEN updatedAt END DESC, " +
            "title COLLATE NOCASE ASC, id ASC",
    )
    fun observeForLibrary(
        filter: String,
        hideErrors: Boolean,
        sortOrder: String,
    ): Flow<List<LibraryBookRow>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE sourcePath = :sourcePath LIMIT 1")
    suspend fun getBySourcePath(sourcePath: String): BookEntity?

    /** Whole table in one round trip for batch rescans (no per-file queries). */
    @Query("SELECT * FROM books")
    suspend fun getAll(): List<BookEntity>

    /**
     * Rows touched since the scan snapshot (concurrent progress/bookmark
     * writes bump updatedAt). The merge pass reads only these instead of
     * re-scanning the whole table.
     */
    @Query("SELECT * FROM books WHERE updatedAt >= :sinceMs")
    suspend fun getUpdatedSince(sinceMs: Long): List<BookEntity>

    @Upsert
    suspend fun upsert(book: BookEntity)

    @Upsert
    suspend fun upsertAll(books: List<BookEntity>)

    @Query("UPDATE books SET progress = :progress, lastLocator = :locator, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Float, locator: String?, updatedAt: Long)

    @Query("UPDATE books SET bookmarked = :bookmarked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBookmark(id: String, bookmarked: Boolean, updatedAt: Long)

    @Query("UPDATE books SET bookmarked = NOT bookmarked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun toggleBookmark(id: String, updatedAt: Long)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Deletes exactly the rows the scan found missing. Callers chunk the
     * list: a single `NOT IN (:ids)` binds one variable per book, and a
     * library past SQLite's variable limit failed the whole prune.
     */
    @Query("DELETE FROM books WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM books WHERE sourcePath LIKE 'content://%'")
    suspend fun deleteAllLinked()
}
