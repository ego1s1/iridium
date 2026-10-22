package com.iridium.core.data.di

import com.iridium.core.data.LinkedTreeLister
import com.iridium.core.data.SafLinkedTreeLister
import com.iridium.epub.EpubBackend
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object EpubModule {

    @Provides
    @Singleton
    fun provideEpubEngine(): com.iridium.epub.engine.IridiumEpubEngine =
        com.iridium.epub.engine.IridiumEpubEngine()

    @Provides
    @Singleton
    fun provideEpubBackend(
        engine: com.iridium.epub.engine.IridiumEpubEngine,
    ): EpubBackend = engine

    @Provides
    @Singleton
    fun provideLinkedTreeLister(lister: SafLinkedTreeLister): LinkedTreeLister = lister
}
