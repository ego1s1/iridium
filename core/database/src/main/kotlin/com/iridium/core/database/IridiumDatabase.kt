package com.iridium.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BookEntity::class, HighlightEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class IridiumDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun highlightDao(): HighlightDao
    abstract fun bookmarkDao(): BookmarkDao
}
