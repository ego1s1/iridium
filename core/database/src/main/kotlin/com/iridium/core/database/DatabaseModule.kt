package com.iridium.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database graph: the Room instance and its DAOs. Lives in `core:database`
 * (not `core:data`) so storage wiring stays with the schema it builds.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IridiumDatabase =
        Room.databaseBuilder(context, IridiumDatabase::class.java, "iridium.db").build()

    @Provides
    @Singleton
    fun provideBookDao(database: IridiumDatabase): BookDao = database.bookDao()

    @Provides
    @Singleton
    fun provideHighlightDao(database: IridiumDatabase): HighlightDao = database.highlightDao()

    @Provides
    @Singleton
    fun provideBookmarkDao(database: IridiumDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    @Singleton
    fun provideChapterTextDao(database: IridiumDatabase): ChapterTextDao =
        database.chapterTextDao()
}
