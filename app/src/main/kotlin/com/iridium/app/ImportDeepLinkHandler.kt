package com.iridium.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.provider.OpenableColumns
import com.iridium.core.data.BooksRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A parsed share/open request for one EPUB document. */
data class ImportRequest(val uri: Uri, val displayName: String?)

/**
 * Handles EPUB deep links from other apps: ACTION_VIEW of an EPUB document
 * and ACTION_SEND with an EPUB stream. Copies the bytes into app-private
 * storage via [BooksRepository.importEpub] and returns the new book id.
 *
 * EPUB only: anything else (including plain text) is declined with null
 * rather than importing a row the reader cannot open.
 */
@Singleton
class ImportDeepLinkHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: BooksRepository,
) {

    /** Extracts an [ImportRequest] from an intent, or null when not ours. */
    fun parseIntent(intent: Intent?): ImportRequest? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return null
                if (!isEpubIntent(intent.type, uri)) return null
                ImportRequest(uri, null)
            }
            Intent.ACTION_SEND -> {
                if (!isEpubIntent(intent.type, null)) return null
                val uri = intent.parcelableExtraCompat<Uri>(Intent.EXTRA_STREAM) ?: return null
                ImportRequest(uri, null)
            }
            else -> null
        }
    }

    /** Imports the intent's book; null when absent, declined, or failed. */
    suspend fun handle(intent: Intent?): String? = withContext(Dispatchers.IO) {
        val request = parseIntent(intent) ?: return@withContext null
        val name = resolveDisplayName(request.uri) ?: request.uri.lastPathSegment
        importUri(name) {
            try {
                context.contentResolver.openInputStream(request.uri)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
    }

    /** Imports one document with an explicit stream (test seam). */
    suspend fun importUri(
        displayName: String?,
        openStream: () -> java.io.InputStream?,
    ): String? = withContext(Dispatchers.IO) {
        try {
            repository.importEpub(displayName ?: "book.epub", openStream)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun isEpubIntent(mimeType: String?, uri: Uri?): Boolean {
        if (mimeType != null) return mimeType.equals(EPUB_MIME, ignoreCase = true)
        // No type: fall back to the file extension (some senders omit it).
        val name = uri?.lastPathSegment.orEmpty()
        return name.substringAfterLast('.', "").lowercase() == "epub"
    }

    private fun resolveDisplayName(uri: Uri): String? =
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    private companion object {
        const val EPUB_MIME = "application/epub+zip"
    }
}

@Suppress("DEPRECATION")
private inline fun <reified T : Parcelable> Intent.parcelableExtraCompat(name: String): T? =
    getParcelableExtra(name) as? T
