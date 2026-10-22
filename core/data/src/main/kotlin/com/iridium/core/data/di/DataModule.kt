package com.iridium.core.data.di

import com.iridium.core.data.BookOpener
import com.iridium.core.data.BooksRepository
import com.iridium.core.data.OfflineFirstBooksRepository
import com.iridium.core.data.ReadiumOpener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBooksRepository(
        impl: OfflineFirstBooksRepository,
    ): BooksRepository

    @Binds
    abstract fun bindBookOpener(
        impl: ReadiumOpener,
    ): BookOpener
}
