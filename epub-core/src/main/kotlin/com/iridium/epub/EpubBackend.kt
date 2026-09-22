package com.iridium.epub

/**
 * Seam over EPUB parsing. Phase 2 uses the built-in ZIP+DOM parser below;
 * Phase 3 may delegate to the Readium Streamer behind this same interface
 * without touching callers.
 */
interface EpubBackend {
    /** Parses [epubBytes] and returns metadata, cover bytes, and TOC. */
    fun inspect(epubBytes: ByteArray, fallbackTitle: String): InspectedEpub
}

/** A chapter entry from the EPUB navigation document. */
data class EpubChapter(
    val href: String,
    val title: String,
)

data class InspectedEpub(
    val title: String,
    val author: String? = null,
    val coverBytes: ByteArray? = null,
    val coverMime: String? = null,
    val spineCount: Int = 0,
    val chapters: List<EpubChapter> = emptyList(),
)
