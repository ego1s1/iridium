package com.iridium.core.data

import com.iridium.core.common.AppClock
import com.iridium.core.common.safeCatching
import com.iridium.core.database.ChapterLayoutDao
import com.iridium.core.database.ChapterLayoutEntity
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

/**
 * Storage seam for the reader's per-typography chapter page counts. Kept out
 * of [BooksRepository] so the reader can depend on one small contract.
 */
interface ChapterLayoutStore {

    /**
     * Page counts learned for [layoutKey] on an earlier session, keyed by
     * chapter href. Empty when the book has never been read at this layout.
     */
    suspend fun load(bookId: String, layoutKey: String): Map<String, Int>

    /** Replaces the stored counts for this book at this layout. */
    suspend fun save(bookId: String, layoutKey: String, pages: Map<String, Int>)
}

@Singleton
internal class RoomChapterLayoutStore @Inject constructor(
    private val dao: ChapterLayoutDao,
    private val clock: AppClock,
) : ChapterLayoutStore {

    override suspend fun load(bookId: String, layoutKey: String): Map<String, Int> {
        val raw = dao.get(bookId, layoutKey)?.pagesJson ?: return emptyMap()
        return safeCatching { decode(raw) }.getOrDefault(emptyMap())
    }

    override suspend fun save(bookId: String, layoutKey: String, pages: Map<String, Int>) {
        if (layoutKey.isEmpty() || pages.isEmpty()) return
        dao.upsert(
            ChapterLayoutEntity(
                bookId = bookId,
                layoutKey = layoutKey,
                pagesJson = JSONObject().also { json ->
                    pages.forEach { (href, count) -> json.put(href, count) }
                }.toString(),
                updatedAt = clock.wallNow(),
            ),
        )
        dao.prune(bookId, KEEP_LAYOUTS)
    }

    private fun decode(raw: String): Map<String, Int> {
        val json = JSONObject(raw)
        val result = mutableMapOf<String, Int>()
        json.keys().forEach { href ->
            val pages = json.optInt(href, 0)
            if (pages > 0) result[href] = pages
        }
        return result
    }

    private companion object {
        /**
         * How many typographies to remember per book. Two or three cover the
         * fonts a reader actually uses; a dragged slider can mint far more.
         */
        const val KEEP_LAYOUTS = 3
    }
}
