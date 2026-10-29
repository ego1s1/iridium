package com.iridium.core.data.di

import com.iridium.core.data.AnnotationRepository
import com.iridium.core.data.LibraryObserver
import com.iridium.core.data.OfflineFirstAnnotationRepository
import com.iridium.core.data.OfflineFirstLibraryObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the internal repository implementations to their public interfaces so
 * consumers depend on the seam, never on the storage-backed class.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryBindingsModule {

    @Binds
    abstract fun bindAnnotationRepository(
        impl: OfflineFirstAnnotationRepository,
    ): AnnotationRepository

    @Binds
    abstract fun bindLibraryObserver(
        impl: OfflineFirstLibraryObserver,
    ): LibraryObserver
}
