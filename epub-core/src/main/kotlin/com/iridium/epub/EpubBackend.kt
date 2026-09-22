package com.iridium.epub

/**
 * Thin seam over the Readium Kotlin toolkit (streamer/navigator land here in
 * Phase 3). Scaffold keeps it dependency-free so the build stays green.
 */
interface EpubBackend {
    suspend fun inspect(sourceUri: String): InspectedEpub
}

data class InspectedEpub(
    val title: String,
    val author: String?,
    val coverBytes: ByteArray? = null,
)
