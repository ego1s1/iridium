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
    @javax.inject.Singleton
    fun provideLibraryBackupManager(
        repository: BooksRepository,
        database: com.iridium.core.database.IridiumDatabase,
    ): LibraryBackupManager = LibraryBackupManager(repository, database)
}
