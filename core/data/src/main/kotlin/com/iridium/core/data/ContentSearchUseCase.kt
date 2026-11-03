package com.iridium.core.data

import com.iridium.core.common.IoDispatcher
import com.iridium.core.database.BookDao
import com.iridium.core.database.ChapterTextDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Full-text chapter search: extract → index → round-robin query. */
internal class ContentSearchUseCase @Inject constructor(
    private val bookDao: BookDao,
    private val chapterTextDao: ChapterTextDao,
    private val chapterIndexer: ChapterIndexer,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /** Extracts and indexes a book's chapter text. Returns chapters indexed (0 when unsupported/unreadable). */
    suspend fun indexBookContent(bookId: String): Int = withContext(ioDispatcher) {
        val row = bookDao.getById(bookId) ?: return@withContext 0
        val toc = row.tocEntries()
        val chapters = runCatching {
            chapterIndexer.extract(bookId, row.sourcePath, row.sourceDisplayName, toc)
        }.getOrDefault(emptyList())

        // Never wipe a good index for a failed extract (unmounted file,
        // transient parse error): the atomic replace below only runs when
        // there is something to replace with.
        if (chapters.isEmpty()) return@withContext 0
        chapterTextDao.replaceForBook(bookId, chapters)
        chapters.size
    }

    /** True when a book already has chapter text in the search index. */
    suspend fun isContentIndexed(bookId: String): Boolean =
        withContext(ioDispatcher) {
            runCatching { chapterTextDao.countForBook(bookId) > 0 }.getOrDefault(false)
        }

    /** Full-text search across every indexed chapter, fairly shared per book. */
    suspend fun searchContent(query: String, limit: Int): List<ContentHit> =
        withContext(ioDispatcher) {
            android.os.Trace.beginSection("iri.search.content")
            try {
                val expression = FtsQuery.build(query) ?: return@withContext emptyList()
                val resultLimit = limit.coerceIn(0, MAX_SEARCH_RESULTS)
                if (resultLimit == 0) return@withContext emptyList()
                val rows = runCatching {
                    chapterTextDao.search(expression, resultLimit * SEARCH_FANOUT)
                }
                    .getOrDefault(emptyList())
                if (rows.isEmpty()) return@withContext emptyList()

                // Round-robin across books: one hit-heavy book must not starve
                // the rest out of the global limit.
                val byBook = rows.map { row ->
                    ContentHit(
                        bookId = row.bookId,
                        bookTitle = row.bookTitle,
                        href = row.href,
                        chapterTitle = row.title,
                        snippet = row.snippet,
                    )
                }.groupBy { it.bookId }
                val rounds = ArrayDeque(byBook.values.map { it.toMutableList() })
                val out = ArrayList<ContentHit>(resultLimit)
                while (out.size < resultLimit && rounds.isNotEmpty()) {
                    val queue = rounds.removeFirst()
                    // removeAt(0), not removeFirst(): the latter binds to the
                    // JDK 21 SequencedCollection member when compiling against a
                    // recent android.jar, then throws NoSuchMethodError on every
                    // runtime older than API 35.
                    out += queue.removeAt(0)
                    if (queue.isNotEmpty()) rounds.addLast(queue)
                }
                out
            } finally {
                android.os.Trace.endSection()
            }
        }

    private companion object {
        /** Overscan factor: fetch extra rows so round-robin fairness has a pool. */
        const val SEARCH_FANOUT = 4
        const val MAX_SEARCH_RESULTS = 40
    }
}
