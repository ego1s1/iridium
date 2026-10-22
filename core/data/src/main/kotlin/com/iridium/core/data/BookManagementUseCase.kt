package com.iridium.core.data

import android.content.Context
import androidx.room.withTransaction
import com.iridium.core.common.AppClock
import com.iridium.core.common.IoDispatcher
import com.iridium.core.database.BookDao
import com.iridium.core.database.ChapterTextDao
import com.iridium.core.database.IridiumDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Book lifecycle writes: progress/bookmark flags and full unlink.
 * Unlink removes the row, thumbnail, and search index atomically; the
 * user's original file always survives (only app-owned import copies
 * are deleted).
 */
internal class BookManagementUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: IridiumDatabase,
    private val bookDao: BookDao,
    private val chapterTextDao: ChapterTextDao,
    private val annotations: AnnotationRepository,
    private val clock: AppClock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun updateProgress(id: String, progress: Float, locator: String?) {
        bookDao.updateProgress(id, progress.coerceIn(0f, 1f), locator, clock.wallNow())
    }

    suspend fun setBookmarked(id: String, bookmarked: Boolean) {
        bookDao.updateBookmark(id, bookmarked, clock.wallNow())
    }

    /** Unlinks a book: removes the row, thumbnail and search index. */
    suspend fun removeBook(id: String) = withContext(ioDispatcher) {
        // Unlink only: the user's original file must survive removal.
        // Atomic: a crash can never leave annotations/FTS without their book.
        val row = bookDao.getById(id)
        if (row != null) {
            database.withTransaction {
                annotations.deleteAllHighlights(id)
                annotations.deleteAllBookmarks(id)
                chapterTextDao.deleteForBook(id)
                bookDao.deleteById(id)
            }
            deleteCover(row.coverPath)
            // Imported copies are app-owned: dropping the row must not orphan
            // the bytes. Linked originals (content://) are never touched.
            deleteImportedFile(row.sourcePath)
        }
        Unit
    }

    fun deleteCover(coverPath: String?) {
        if (coverPath != null) runCatching { File(coverPath).delete() }
    }

    /** Deletes an app-owned import copy; never touches linked originals. */
    private fun deleteImportedFile(sourcePath: String) {
        val booksDir = File(context.filesDir, IMPORT_DIR).absolutePath
        val file = File(sourcePath)
        if (file.absolutePath.startsWith(booksDir + File.separator)) {
            runCatching { file.delete() }
        }
    }
}
