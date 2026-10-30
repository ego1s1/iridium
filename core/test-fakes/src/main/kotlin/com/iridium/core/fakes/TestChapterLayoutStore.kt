package com.iridium.core.fakes

import com.iridium.core.data.ChapterLayoutStore

/** In-memory [ChapterLayoutStore] with direct state hooks. */
class TestChapterLayoutStore : ChapterLayoutStore {

    private val saved = mutableMapOf<Pair<String, String>, Map<String, Int>>()

    /** Number of writes, so debounced saves can be asserted. */
    var saveCount: Int = 0
        private set

    override suspend fun load(bookId: String, layoutKey: String): Map<String, Int> =
        saved[bookId to layoutKey].orEmpty()

    override suspend fun save(bookId: String, layoutKey: String, pages: Map<String, Int>) {
        saved[bookId to layoutKey] = pages
        saveCount += 1
    }

    /** Pre-seeds a layout as if an earlier session had learned it. */
    fun seed(bookId: String, layoutKey: String, pages: Map<String, Int>) {
        saved[bookId to layoutKey] = pages
    }
}
