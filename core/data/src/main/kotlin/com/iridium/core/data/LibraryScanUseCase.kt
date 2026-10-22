package com.iridium.core.data

import android.content.Context
import androidx.room.withTransaction
import com.iridium.core.common.IoDispatcher
import com.iridium.core.database.BookDao
import com.iridium.core.database.BookEntity
import com.iridium.core.database.ChapterTextDao
import com.iridium.core.database.IridiumDatabase
import com.iridium.core.model.BookError
import com.iridium.core.model.TocEntry
import com.iridium.epub.EpubBackend
import com.iridium.epub.InspectedEpub
import com.iridium.epub.nativecore.NativeEpub
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

/**
 * Filesystem scan + single-file import. The scan walks every linked tree,
 * re-inspects changed books, merges concurrent reader writes
 * (last-writer-wins on progress/bookmarks only), and prunes rows whose
 * files are gone — all in one transaction per pass. Import streams one
 * shared EPUB into app-private storage, then reuses the scan's
 * inspect/index path so imports behave exactly like scanned books.
 */
internal class LibraryScanUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: IridiumDatabase,
    private val bookDao: BookDao,
    private val chapterTextDao: ChapterTextDao,
    private val backend: EpubBackend,
    private val covers: EpubCoverGenerator,
    private val treeLister: LinkedTreeLister,
    private val sourceFactory: EpubSourceFactory,
    private val management: BookManagementUseCase,
    private val search: ContentSearchUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun indexFilesystem(
        onProgress: (done: Int, total: Int) -> Unit,
    ): IndexReport = withContext(ioDispatcher) {
        val (docs, walkFailed) = treeLister.listBooks()
        // Batch: one table fetch, one upsert, one emission.
        val knownById = bookDao.getAll().associateBy { it.id }
        val rows = mutableListOf<BookEntity>()
        var failed = 0
        docs.forEachIndexed { index, doc ->
            val uri = doc.uri.toString()
            try {
                val known = knownById[uri]
                val row = if (
                    known != null &&
                    known.error == null &&
                    known.sourceModified == doc.modified &&
                    known.coverPath?.let { File(it).isFile } == true
                ) {
                    // Fast path: healthy unchanged file with a live thumbnail.
                    // Error rows always re-inspect so transient failures retry.
                    known
                } else {
                    indexDocument(doc, known)
                }
                rows += row
                if (row.error != null) failed += 1
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed += 1
            }
            onProgress(index + 1, docs.size)
        }
        // Re-read before writing: a reader progress write may have landed
        // after the snapshot above; last-writer-wins with stale rows would
        // eat it.
        val fresh = bookDao.getAll().associateBy { it.id }
        val merged = rows.map { row ->
            val live = fresh[row.id]
            val snap = knownById[row.id]
            if (live != null && (live.updatedAt > (snap?.updatedAt ?: 0L))) {
                row.copy(
                    progress = live.progress,
                    lastLocator = live.lastLocator,
                    updatedAt = live.updatedAt,
                    bookmarked = live.bookmarked,
                )
            } else {
                row
            }
        }
        // One transaction: crash/interleave can never leave book rows
        // without FTS or FTS without books. Cover files delete after
        // commit (file I/O must stay out of the DB transaction).
        val prunedLinked: List<BookEntity> = database.withTransaction {
            if (merged.isNotEmpty()) bookDao.upsertAll(merged)
            pruneLocked(
                foundIdsOf = merged,
                knownById = knownById,
                walkFailed = walkFailed,
            )
        }
        prunedLinked.forEach { management.deleteCover(it.coverPath) }
        IndexReport(total = docs.size, failed = failed)
    }

    /**
     * Imports one shared EPUB into app-private storage (`filesDir/books`).
     * The copy is streamed with a size cap; inspection/indexing reuse the
     * scan path, so imported books behave exactly like scanned ones. Prune
     * queries only touch `content://` rows, so imports survive rescans.
     */
    suspend fun importEpub(displayName: String, openStream: () -> java.io.InputStream?): String =
        withContext(ioDispatcher) {
            val name = sanitizeImportDisplayName(displayName)
            val id = UUID.randomUUID().toString()
            val booksDir = File(context.filesDir, IMPORT_DIR).apply { mkdirs() }
            val dest = File(booksDir, "$id.epub")
            val stream = openStream() ?: throw IOException("Unable to read $name")
            try {
                stream.use { input ->
                    dest.outputStream().use { output ->
                        copyCapped(input, output, MAX_IMPORT_BYTES)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                dest.delete()
                throw e
            }
            val now = System.currentTimeMillis()
            try {
                val inspected = sourceFactory.open(dest.absolutePath, name)
                    .use { source -> backend.inspect(source, name.substringBeforeLast('.')) }
                if (inspected.spineCount == 0 && inspected.chapters.isEmpty()) {
                    throw IOException("Not a readable EPUB: $name")
                }
                val coverPath = covers.generate(inspected.coverBytes, "import-$id")
                bookDao.upsert(
                    BookEntity(
                        id = id,
                        title = inspected.title,
                        author = inspected.author,
                        format = com.iridium.core.model.BookFormat.EPUB.name,
                        spineCount = inspected.spineCount,
                        sourcePath = dest.absolutePath,
                        coverPath = coverPath,
                        progress = 0f,
                        lastLocator = null,
                        sourceDisplayName = name,
                        sourceModified = dest.lastModified(),
                        tocJson = encodeToc(inspected.chapters.map { TocEntry(it.href, it.title) }),
                        error = null,
                        createdAt = now,
                        updatedAt = now,
                        bookmarked = false,
                    ),
                )
                // Best-effort FTS like the scan's follow-up pass.
                runCatching { search.indexBookContent(id) }
                id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                dest.delete()
                throw (e as? IOException ?: IOException("Could not import $name", e))
            }
        }

    /**
     * Prunes rows deleted from the tree out from under us — never on a
     * failed walk, which would read as an empty folder and wipe rows the
     * user still owns. Anything not found is gone, whatever scheme its row
     * used (legacy rows included). FTS follows only linked books: legacy
     * file rows survive in the table (the DAO deletes content:// rows
     * only), so wiping their index would orphan live rows.
     *
     * Must run inside [IridiumDatabase.withTransaction]; returns the pruned
     * linked rows so cover files delete after commit.
     */
    private suspend fun pruneLocked(
        foundIdsOf: List<BookEntity>,
        knownById: Map<String, BookEntity>,
        walkFailed: Boolean,
    ): List<BookEntity> {
        if (walkFailed) return emptyList()
        val foundIds = foundIdsOf.map { it.id }.toSet()
        val pruned = knownById.values.filter { it.id !in foundIds }
        val prunedLinked = pruned.filter { isLinkedSourcePath(it.id) }
        if (foundIdsOf.isEmpty() && pruned.isNotEmpty()) {
            bookDao.deleteAllLinked()
        } else if (pruned.isNotEmpty()) {
            bookDao.deleteMissingLinked(foundIds.toList())
        }
        if (prunedLinked.isNotEmpty()) {
            chapterTextDao.deleteForBooks(prunedLinked.map { it.id })
        }
        return prunedLinked
    }

    /**
     * Inspects one linked document and builds its row WITHOUT writing, so the
     * caller batches every row into a single upsert. The URI is the stable id,
     * so rescans refresh rows instead of duplicating them, and progress /
     * bookmarks carry over via [existing].
     */
    private suspend fun indexDocument(doc: LinkedDocument, existing: BookEntity?): BookEntity {
        val uri = doc.uri.toString()
        val now = System.currentTimeMillis()
        val coverId = linkedCoverId(uri)
        return try {
            val inspected = inspectDocument(doc)
            val coverPath = covers.generate(inspected.coverBytes, coverId)
                ?: existing?.coverPath?.takeIf { File(it).isFile }
            BookEntity(
                id = uri,
                title = inspected.title,
                author = inspected.author,
                format = com.iridium.core.model.BookFormat.EPUB.name,
                spineCount = inspected.spineCount,
                sourcePath = uri,
                coverPath = coverPath,
                progress = existing?.progress ?: 0f,
                lastLocator = existing?.lastLocator,
                sourceDisplayName = doc.name,
                sourceModified = doc.modified,
                tocJson = encodeToc(inspected.chapters.map { TocEntry(it.href, it.title) }),
                error = if (inspected.spineCount == 0 && inspected.chapters.isEmpty()) {
                    BookError.CORRUPT.name
                } else {
                    null
                },
                createdAt = existing?.createdAt ?: now,
                updatedAt = existing?.updatedAt ?: now,
                bookmarked = existing?.bookmarked ?: false,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Typed failure, not silence: keep the row so the user sees the
            // book, flagged for the detail screen's retry/remove path.
            BookEntity(
                id = uri,
                title = existing?.title ?: doc.name.substringBeforeLast('.'),
                author = existing?.author,
                format = com.iridium.core.model.BookFormat.EPUB.name,
                spineCount = 0,
                sourcePath = uri,
                coverPath = existing?.coverPath,
                progress = existing?.progress ?: 0f,
                lastLocator = existing?.lastLocator,
                sourceDisplayName = doc.name,
                sourceModified = doc.modified,
                tocJson = existing?.tocJson,
                error = BookError.CORRUPT.name,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                bookmarked = existing?.bookmarked ?: false,
            )
        }
    }

    /**
     * Reads metadata/cover/TOC for one document.
     *
     * Prefers the native core when the provider exposes a real file descriptor
     * and the native scan actually understood the book; otherwise falls back to
     * the JVM engine, which handles ZIP64 and every archive the native core
     * declines. Both paths are never-throw.
     */
    private fun inspectDocument(doc: LinkedDocument): InspectedEpub {
        val fallback = doc.name.substringBeforeLast('.')
        if (NativeEpub.isAvailable) {
            sourceFactory.withFileDescriptor(doc.uri.toString()) { fd ->
                NativeEpub.inspectFd(fd, fallback)
            }?.let { return it }
        }
        return sourceFactory.open(doc.uri.toString(), doc.name)
            .use { source -> backend.inspect(source, fallback) }
    }
}
