package com.iridium.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        HighlightEntity::class,
        BookmarkEntity::class,
        ChapterTextEntity::class,
        ChapterLayoutEntity::class,
    ],
    version = 6,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        // Adds chapter_layouts; no existing table changes, so Room can derive
        // the migration.
        AutoMigration(from = 4, to = 5),
        // Adds the updatedAt index the scan merge reads through.
        AutoMigration(from = 5, to = 6),
    ],
    exportSchema = true,
)
abstract class IridiumDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun highlightDao(): HighlightDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun chapterTextDao(): ChapterTextDao
    abstract fun chapterLayoutDao(): ChapterLayoutDao
}
