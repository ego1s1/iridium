package com.iridium.core.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Binds [LibraryBackupManager] for injection into ViewModels. */
@Module
@InstallIn(SingletonComponent::class)
object BackupModule {

    @Provides
    fun provideLibraryBackupManager(
        repository: BooksRepository,
    ): LibraryBackupManager = LibraryBackupManager(repository)
}
