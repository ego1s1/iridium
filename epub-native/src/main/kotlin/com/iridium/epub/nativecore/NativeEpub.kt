package com.iridium.epub.nativecore

import com.iridium.epub.EpubChapter
import com.iridium.epub.InspectedEpub
import org.json.JSONObject

/**
 * Kotlin façade over the native EPUB core.
 *
 * Native is an accelerator, never a requirement: [isAvailable] is false when
 * the shared library is missing (unsupported ABI, host tests), and every call
 * returns null on any failure. Callers are expected to fall back to the JVM
 * engine, so this type can never be the reason a book fails to open.
 *
 * The native core is also deliberately conservative: it refuses ZIP64 archives
 * and returns null when it finds no spine or chapters, which routes those books
 * to the JVM engine instead of producing a half-parsed result.
 */
object NativeEpub {

    val isAvailable: Boolean by lazy {
        runCatching { System.loadLibrary("iridium_epub") }.isSuccess
    }

    /**
     * Inspects a book from an open file descriptor. Returns null when native
     * is unavailable, the archive is unsupported, or the result lacks real
     * structure — the caller then uses the JVM engine.
     */
    fun inspectFd(fd: Int, fallbackTitle: String): InspectedEpub? {
        if (!isAvailable) return null
        return runCatching {
            val json = nativeInspect(fd) ?: return null
            val obj = JSONObject(json)

            val spineCount = obj.optInt("spineCount", 0)
            val chapters = obj.optJSONArray("chapters")?.let { array ->
                (0 until array.length()).mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    val href = item.optString("href")
                    if (href.isEmpty()) return@mapNotNull null
                    EpubChapter(href = href, title = item.optString("title"))
                }
            }.orEmpty()

            // Sanity gate: a result with no spine and no chapters means the
            // native scan did not really understand this book.
            if (spineCount == 0 && chapters.isEmpty()) return null

            val title = obj.optString("title").ifBlank { fallbackTitle.ifBlank { "Unknown" } }
            val author = obj.optString("author").ifBlank { null }
            val coverMime = obj.optString("coverMime").ifBlank { null }

            InspectedEpub(
                title = title,
                author = author,
                coverBytes = coverFd(fd),
                coverMime = coverMime,
                spineCount = spineCount,
                chapters = chapters,
            )
        }.getOrNull()
    }

    /** Cover image bytes, or null when absent/unreadable/oversized. */
    fun coverFd(fd: Int): ByteArray? =
        if (!isAvailable) null else runCatching { nativeCover(fd) }.getOrNull()

    /** Resource bytes by archive path (the chapter pipeline). */
    fun chapterFd(fd: Int, href: String): ByteArray? =
        if (!isAvailable) null else runCatching { nativeChapter(fd, href) }.getOrNull()

    private external fun nativeInspect(fd: Int): String?

    private external fun nativeCover(fd: Int): ByteArray?

    private external fun nativeChapter(fd: Int, name: String): ByteArray?
}
