package com.iridium.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/** Failure opening a book with Readium. */
sealed interface OpenResult {
    data class Opened(val publication: Publication) : OpenResult
    data object FileMissing : OpenResult
    data object ParseFailed : OpenResult
}

/**
 * Opens app-private EPUB files with the Readium Streamer. Import-time
 * metadata stays on the lightweight [com.iridium.epub.ZipEpubBackend];
 * this is only for rendering, so the reader pays the Streamer cost lazily.
 */
@Singleton
class ReadiumOpener @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    private val parser = DefaultPublicationParser(context, httpClient, assetRetriever, null)
    private val opener = PublicationOpener(parser, emptyList())

    suspend fun open(sourcePath: String): OpenResult {
        val file = File(sourcePath)
        if (!file.exists()) return OpenResult.FileMissing
        val asset = when (val result = assetRetriever.retrieve(file)) {
            is Try.Success<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                (result as Try.Success<org.readium.r2.shared.util.asset.Asset, *>).value
            }
            else -> return OpenResult.ParseFailed
        }
        return when (val result = opener.open(asset, allowUserInteraction = false)) {
            is Try.Success<*, *> -> {
                // The publication borrows the asset's resources lazily: it must
                // stay open until the reader session ends (closed in onCleared).
                @Suppress("UNCHECKED_CAST")
                OpenResult.Opened((result as Try.Success<Publication, *>).value)
            }
            else -> OpenResult.ParseFailed
        }
    }
}
