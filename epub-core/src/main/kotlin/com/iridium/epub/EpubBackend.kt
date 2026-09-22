package com.iridium.epub

import java.io.InputStream

/**
 * Seam over EPUB parsing. Streaming is the primary contract: callers hand a
 * factory that opens a fresh stream, so large archives are read in bounded
 * passes instead of being loaded whole into memory.
 */
interface EpubBackend {
    /**
     * Parses the EPUB produced by [openStream]. [openStream] may be invoked
     * more than once (the parser reads the archive in bounded passes).
     */
    fun inspect(openStream: () -> InputStream, fallbackTitle: String): InspectedEpub

    /**
     * Byte-array convenience used by tests and small in-memory sources.
     * The default reads the array once and delegates to the stream path.
     */
    fun inspect(epubBytes: ByteArray, fallbackTitle: String): InspectedEpub =
        inspect({ epubBytes.inputStream() }, fallbackTitle)
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
