package com.iridium.core.data

import com.iridium.core.database.ChapterTextEntity
import com.iridium.core.model.TocEntry
import com.iridium.epub.engine.HtmlText
import com.iridium.epub.engine.IridiumEpubEngine
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Extracts the plain text of a book's chapters for the full-text index.
 *
 * Reads the spine through the JVM engine's [com.iridium.epub.engine.EpubBook]
 * pipeline: indexing is a per-book, on-demand job rather than the hot folder
 * scan, and the pipeline already random-accesses a seekable source with no
 * full-archive read. Chapter titles come from the parsed TOC when the href
 * matches, otherwise the file name.
 */
@Singleton
internal class ChapterIndexer @Inject constructor(
    private val sourceFactory: EpubSourceFactory,
    private val engine: IridiumEpubEngine,
) {
    suspend fun extract(
        sourcePath: String,
        displayName: String,
        toc: List<TocEntry>,
    ): List<ChapterTextEntity> = withContext(Dispatchers.Default) {
        val source = runCatching { sourceFactory.open(sourcePath, displayName) }.getOrNull()
            ?: return@withContext emptyList()

        source.use { open ->
            val book = runCatching { engine.open(open, displayName) }.getOrNull()
                ?: return@withContext emptyList()
            try {
                val titlesByHref = toc.associate { it.href.substringBefore('#') to it.title }
                val rows = ArrayList<ChapterTextEntity>()
                var totalChars = 0
                val hrefs = book.chapterHrefs().take(MAX_CHAPTERS)

                hrefs.forEachIndexed { index, href ->
                    ensureActive()
                    // Total budget: a 1000-chapter omnibus at 200k chars each
                    // would otherwise stage ~200MB in one Room transaction.
                    if (totalChars >= MAX_TOTAL_CHARS) return@forEachIndexed
                    // One bad chapter must not abort the whole book's index.
                    val bytes = runCatching { book.chapterBytes(href) }.getOrNull()
                        ?: return@forEachIndexed
                    if (bytes.size > MAX_CHAPTER_BYTES) return@forEachIndexed

                    val text = runCatching { HtmlText.toPlainText(String(bytes, Charsets.UTF_8)) }
                        .getOrNull() ?: return@forEachIndexed
                    if (text.length < MIN_CHAPTER_CHARS) return@forEachIndexed

                    // TOC title when the href matches; a numbered label reads far
                    // better in search results than a raw file name.
                    val title = titlesByHref[href.substringBefore('#')] ?: "Chapter ${index + 1}"

                    val clipped = text.take(MAX_BODY_CHARS)
                    totalChars += clipped.length
                    rows += ChapterTextEntity(
                        bookId = sourcePath,
                        href = href,
                        title = title,
                        body = clipped,
                    )
                }
                rows
            } finally {
                runCatching { book.close() }
            }
        }
    }

    private companion object {
        /** Bounds on what a single book may add to the index. */
        const val MAX_CHAPTERS = 1000
        const val MAX_CHAPTER_BYTES = 4 * 1024 * 1024
        const val MAX_BODY_CHARS = 200_000

        /** Total staged text per book: bounds the Room transaction. */
        const val MAX_TOTAL_CHARS = 2_000_000

        /** Chapters shorter than this are covers/blank pages, not prose. */
        const val MIN_CHAPTER_CHARS = 24
    }
}
