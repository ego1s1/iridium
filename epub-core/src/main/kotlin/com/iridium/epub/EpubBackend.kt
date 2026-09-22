package com.iridium.epub

/**
 * Seam over EPUB parsing. Implementations read a [EpubSource] (random access)
 * and must never throw for malformed input — failures degrade to a fallback
 * title so a bad file can never crash a folder scan.
 */
interface EpubBackend {
    fun inspect(source: EpubSource, fallbackTitle: String): InspectedEpub

    /** Byte-array convenience for tests and small in-memory sources. */
    fun inspect(epubBytes: ByteArray, fallbackTitle: String): InspectedEpub =
        EpubSource.ofBytes(epubBytes).use { inspect(it, fallbackTitle) }
}

/** A chapter entry from the EPUB navigation document. */
data class EpubChapter(
    val href: String,
    val title: String,
)

/** Everything the library needs to index a book without opening it again. */
data class InspectedEpub(
    val title: String,
    val author: String? = null,
    val coverBytes: ByteArray? = null,
    val coverMime: String? = null,
    val spineCount: Int = 0,
    val chapters: List<EpubChapter> = emptyList(),
)
