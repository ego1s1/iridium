package com.iridium.core.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toAbsoluteUrl
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/** Failure opening a book with Readium. */
sealed interface OpenResult {
    data class Opened(val publication: Publication) : OpenResult
    data object FileMissing : OpenResult
    data object ParseFailed : OpenResult
}

/**
 * Opens books with the Readium Streamer. Linked rows are SAF document URIs,
 * so Readium reads them in place through the ContentResolver — the app never
 * copies the user's file. Legacy absolute paths still resolve.
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
        val asset = retrieveAsset(sourcePath) ?: return OpenResult.ParseFailed
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

    private suspend fun retrieveAsset(sourcePath: String): Asset? {
        val result = if (isLinkedSourcePath(sourcePath)) {
            val url = Uri.parse(sourcePath).toAbsoluteUrl() ?: return null
            assetRetriever.retrieve(url)
        } else {
            val file = File(sourcePath)
            if (!file.exists()) return null
            assetRetriever.retrieve(file)
        }
        return when (result) {
            is Try.Success<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                (result as Try.Success<Asset, *>).value
            }
            else -> null
        }
    }
}
