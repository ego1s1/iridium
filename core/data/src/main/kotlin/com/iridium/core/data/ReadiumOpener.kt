package com.iridium.core.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.Closeable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.toAbsoluteUrl
import org.readium.r2.shared.util.toUrl
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/** Opens a book source. Interface seam so ViewModel tests fake the streamer. */
fun interface BookOpener {
    suspend fun open(sourcePath: String): OpenResult
}

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
) : BookOpener {
    private val httpClient = DefaultHttpClient()
    private val assetRetriever = AssetRetriever(context.contentResolver, httpClient)
    private val parser = DefaultPublicationParser(context, httpClient, assetRetriever, null)
    private val opener = PublicationOpener(parser, emptyList())

    override suspend fun open(sourcePath: String): OpenResult = withContext(Dispatchers.IO) {
        when (val retrieved = retrieveAsset(sourcePath)) {
            Retrieved.Gone -> OpenResult.FileMissing
            Retrieved.Unreadable -> OpenResult.ParseFailed
            is Retrieved.Found -> {
                val result = runCatching {
                    opener.open(retrieved.asset, allowUserInteraction = false)
                }.getOrNull()
                when (result) {
                    is Try.Success<*, *> -> {
                        // The publication borrows the asset's resources lazily:
                        // it must stay open until the reader session ends
                        // (closed in onCleared).
                        @Suppress("UNCHECKED_CAST")
                        OpenResult.Opened((result as Try.Success<Publication, *>).value)
                    }
                    else -> {
                        runCatching { (retrieved.asset as? Closeable)?.close() }
                        OpenResult.ParseFailed
                    }
                }
            }
        }
    }

    /** Retrieval outcome, so gone files and corrupt files stay distinct. */
    private sealed interface Retrieved {
        data class Found(val asset: Asset) : Retrieved
        data object Gone : Retrieved
        data object Unreadable : Retrieved
    }

    private suspend fun retrieveAsset(sourcePath: String): Retrieved {
        if (isLinkedSourcePath(sourcePath)) {
            val url = Uri.parse(sourcePath).toAbsoluteUrl() ?: return Retrieved.Gone
            // A revoked SAF grant throws out of retrieve(): map to ParseFailed
            // at the call site instead of crashing the open coroutine.
            val result = runCatching { assetRetriever.retrieve(url) }.getOrNull()
            return when (result) {
                is Try.Success<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    Retrieved.Found((result as Try.Success<Asset, *>).value)
                }
                else -> Retrieved.Unreadable
            }
        }
        // file:// URIs from legacy scans, or plain absolute paths: both
        // resolve to the same File.
        val file = File(Uri.parse(sourcePath).path ?: sourcePath)
        if (!file.exists()) return Retrieved.Gone
        val result = runCatching { assetRetriever.retrieve(file) }.getOrNull()
        return when (result) {
            is Try.Success<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                Retrieved.Found((result as Try.Success<Asset, *>).value)
            }
            else -> Retrieved.Unreadable
        }
    }
}
