package com.iridium.core.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.iridium.core.database.BookDao
import com.iridium.core.database.BookEntity
import com.iridium.core.model.BookError
import com.iridium.epub.EpubBackend
import com.iridium.epub.ZipEpubBackend
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Result of a single-file EPUB import. */
sealed interface ImportResult {
    data class Imported(val bookId: String) : ImportResult
    data class AlreadyInLibrary(val bookId: String) : ImportResult
    data class Failed(val reason: BookError) : ImportResult
}

/**
 * Copies an EPUB picked via the system picker into app-private storage,
 * parses metadata/cover/TOC, and upserts the book row. Files are capped at
 * 256MB; parse failures still index an error row instead of throwing.
 */
@Singleton
class EpubImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val backend: EpubBackend = ZipEpubBackend(),
) {
    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri) ?: "book.epub"
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext ImportResult.Failed(BookError.CORRUPT)
        } catch (_: Exception) {
            return@withContext ImportResult.Failed(BookError.CORRUPT)
        }
        if (bytes.isEmpty()) return@withContext ImportResult.Failed(BookError.EMPTY)
        if (bytes.size > MAX_EPUB_BYTES) return@withContext ImportResult.Failed(BookError.UNSUPPORTED)

        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
        val id = UUID.randomUUID().toString()
        val bookFile = File(booksDir, "$id.epub")
        try {
            bookFile.writeBytes(bytes)
        } catch (_: Exception) {
            return@withContext ImportResult.Failed(BookError.CORRUPT)
        }

        val inspected = backend.inspect(bytes, displayName.substringBeforeLast('.'))
        val now = System.currentTimeMillis()
        val coverPath = inspected.coverBytes?.let { coverBytes ->
            val ext = when {
                inspected.coverMime?.contains("png", ignoreCase = true) == true -> "png"
                inspected.coverMime?.contains("webp", ignoreCase = true) == true -> "webp"
                inspected.coverMime?.contains("gif", ignoreCase = true) == true -> "gif"
                else -> "jpg"
            }
            val coverFile = File(coversDir, "$id.$ext")
            try {
                coverFile.writeBytes(coverBytes)
                coverFile.absolutePath
            } catch (_: Exception) {
                null
            }
        }

        val error = if (inspected.spineCount == 0 && inspected.chapters.isEmpty()) {
            // Parser found no spine: keep the row so the user sees the book,
            // flagged for the detail screen's retry/remove path.
            BookError.CORRUPT.name
        } else {
            null
        }

        val toc = inspected.chapters.map {
            com.iridium.core.model.TocEntry(it.href, it.title)
        }
        val entity = BookEntity(
            id = id,
            title = inspected.title,
            author = inspected.author,
            format = com.iridium.core.model.BookFormat.EPUB.name,
            spineCount = inspected.spineCount,
            sourcePath = bookFile.absolutePath,
            coverPath = coverPath,
            progress = 0f,
            lastLocator = null,
            sourceDisplayName = displayName,
            tocJson = encodeToc(toc),
            error = error,
            createdAt = now,
            updatedAt = now,
        )
        return@withContext try {
            bookDao.upsert(entity)
            ImportResult.Imported(id)
        } catch (_: Exception) {
            bookFile.delete()
            coverPath?.let { File(it).delete() }
            ImportResult.Failed(BookError.CORRUPT)
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        // Content URIs from the system picker carry DISPLAY_NAME; file paths fall back.
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1 && cursor.moveToFirst()) {
                        return cursor.getString(idx)
                    }
                }
            } catch (_: Exception) {
                // Fall through to path fallback.
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private companion object {
        const val MAX_EPUB_BYTES = 256 * 1024 * 1024
    }
}
