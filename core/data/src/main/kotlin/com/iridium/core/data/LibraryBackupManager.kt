package com.iridium.core.data

import androidx.room.withTransaction
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.LibraryQuery
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Result of a [LibraryBackupManager.restore]. */
data class RestoreReport(val restored: Int, val skipped: Int)

/**
 * Snapshots and restores library metadata (progress, bookmarks, annotations)
 * as JSON. Books are matched by [BackupBook.sourcePath]; EPUB files are never
 * copied. Restore re-applies metadata only to books still present in the
 * library.
 */
class LibraryBackupManager @Inject constructor(
    private val repository: BooksRepository,
    private val database: com.iridium.core.database.IridiumDatabase? = null,
) {

    /** Serializes the whole library's metadata to JSON. */
    suspend fun snapshot(): String {
        val books = repository.observeLibrary(LibraryQuery()).first()
        val backupBooks = books.map { book ->
            val highlights = repository.observeHighlights(book.id).first()
            val bookmarks = repository.observeBookmarks(book.id).first()
            BackupBook(
                sourcePath = book.sourcePath,
                title = book.title,
                author = book.author,
                progress = book.progress,
                lastLocator = book.lastLocator,
                bookmarked = book.bookmarked,
                highlights = highlights.map {
                    BackupHighlight(
                        id = it.id,
                        href = it.href,
                        startLocator = it.startLocator,
                        endLocator = it.endLocator,
                        selectedText = it.selectedText,
                        color = it.color.name,
                        note = it.note,
                        createdAt = it.createdAt,
                    )
                },
                bookmarks = bookmarks.map {
                    BackupBookmark(id = it.id, locator = it.locator, label = it.label, createdAt = it.createdAt)
                },
            )
        }
        return LibraryBackupCodec.encode(
            LibraryBackup(createdAt = System.currentTimeMillis(), books = backupBooks),
        )
    }

    /** Re-applies a backup's metadata to books still present. */
    @Suppress("TooGenericExceptionCaught") // decode throws SerializationException/IllegalArgument on corrupt JSON
    suspend fun restore(json: String): RestoreReport {
        val backup = try {
            LibraryBackupCodec.decode(json)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            throw java.io.IOException("Invalid backup file", e)
        }
        val current = repository.observeLibrary(LibraryQuery()).first()
        val byPath = current.associateBy { it.sourcePath }
        var restored = 0
        var skipped = 0
        backup.books.forEach { bb ->
            val book = byPath[bb.sourcePath] ?: run {
                skipped++
                return@forEach
            }
            val db = database
            if (db != null) {
                db.withTransaction { restoreOne(book.id, bb) }
            } else {
                restoreOne(book.id, bb)
            }
            restored++
        }
        return RestoreReport(restored = restored, skipped = skipped)
    }

    private suspend fun restoreOne(bookId: String, bb: BackupBook) {
        repository.updateProgress(bookId, bb.progress, bb.lastLocator)
        repository.setBookmarked(bookId, bb.bookmarked)
        repository.deleteAllHighlights(bookId)
        bb.highlights.forEach { h ->
            repository.upsertHighlight(
                Highlight(
                    id = h.id,
                    bookId = bookId,
                    href = h.href,
                    startLocator = h.startLocator,
                    endLocator = h.endLocator,
                    selectedText = h.selectedText,
                    color = backupColor(h.color),
                    note = h.note,
                    createdAt = h.createdAt,
                ),
            )
        }
        repository.deleteAllBookmarks(bookId)
        bb.bookmarks.forEach { m ->
            repository.upsertBookmark(
                Bookmark(id = m.id, bookId = bookId, locator = m.locator, label = m.label, createdAt = m.createdAt),
            )
        }
    }
}
