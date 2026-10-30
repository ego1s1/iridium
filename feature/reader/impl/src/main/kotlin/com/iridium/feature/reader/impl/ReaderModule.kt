package com.iridium.feature.reader.impl

import android.content.Context
import com.iridium.core.common.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher

/** Reader-scoped bindings (collaborators the ViewModel fakes in tests). */
@Module
@InstallIn(SingletonComponent::class)
internal object ReaderModule {

    @Provides
    @Singleton
    fun provideDictionaryLookup(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DictionaryLookup = HttpDictionaryLookup(ioDispatcher = ioDispatcher)

    /**
     * The viewport is part of the page-count layout fingerprint: rotating the
     * device or changing screen size repaginates the book, so the learned
     * counts must not be reused across it.
     */
    @Provides
    @Singleton
    fun provideReaderViewport(@ApplicationContext context: Context): ReaderViewportProvider =
        ReaderViewportProvider {
            val configuration = context.resources.configuration
            ReaderViewport(
                widthDp = configuration.screenWidthDp,
                densityDpi = configuration.densityDpi,
            )
        }
}
