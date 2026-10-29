package com.iridium.feature.reader.impl

import com.iridium.core.common.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/** Reader-scoped bindings (network collaborators the ViewModel fakes in tests). */
@Module
@InstallIn(SingletonComponent::class)
internal object ReaderModule {

    @Provides
    @Singleton
    fun provideDictionaryLookup(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DictionaryLookup = HttpDictionaryLookup(ioDispatcher = ioDispatcher)
}
