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
    ],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ],
    exportSchema = true,
)
abstract class IridiumDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun highlightDao(): HighlightDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun chapterTextDao(): ChapterTextDao
}
