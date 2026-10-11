package com.iridium.core.data

import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.LibraryQuery
import kotlinx.coroutines.flow.first

/** Result of a [LibraryBackupManager.restore]. */
data class RestoreReport(val restored: Int, val skipped: Int)

/**
 * Snapshots and restores library metadata (progress, bookmarks, annotations)
 * as JSON. Books are matched by [BackupBook.sourcePath]; EPUB files are never
 * copied. Restore re-applies metadata only to books still present in the
 * library.
 */
class LibraryBackupManager(
    private val repository: BooksRepository,
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
    suspend fun restore(json: String): RestoreReport {
        val backup = LibraryBackupCodec.decode(json)
        val current = repository.observeLibrary(LibraryQuery()).first()
        val byPath = current.associateBy { it.sourcePath }
        var restored = 0
        var skipped = 0
        backup.books.forEach { bb ->
            val book = byPath[bb.sourcePath] ?: run {
                skipped++
                return@forEach
            }
            repository.updateProgress(book.id, bb.progress, bb.lastLocator)
            repository.setBookmarked(book.id, bb.bookmarked)
            repository.deleteAllHighlights(book.id)
            bb.highlights.forEach { h ->
                repository.upsertHighlight(
                    Highlight(
                        id = h.id,
                        bookId = book.id,
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
            repository.deleteAllBookmarks(book.id)
            bb.bookmarks.forEach { m ->
                repository.upsertBookmark(
                    Bookmark(id = m.id, bookId = book.id, locator = m.locator, label = m.label, createdAt = m.createdAt),
                )
            }
            restored++
        }
        return RestoreReport(restored = restored, skipped = skipped)
    }
}
