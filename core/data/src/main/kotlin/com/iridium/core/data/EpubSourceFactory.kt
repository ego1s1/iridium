package com.iridium.core.data

import android.content.Context
import com.iridium.epub.EpubSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds seekable [EpubSource]s for library books.
 *
 * Prefers a content-provider file descriptor so the engine random-accesses the
 * user's file and nothing is copied; only pipe-like providers fall back to
 * spooling a temporary copy into the cache.
 */
@Singleton
class EpubSourceFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun open(sourcePath: String, displayName: String): EpubSource {
        if (isLinkedSourcePath(sourcePath)) {
            runCatching {
                val descriptor = context.contentResolver.openFileDescriptor(
                    android.net.Uri.parse(sourcePath),
                    "r",
                )
                if (descriptor != null) {
                    val channel = java.io.FileInputStream(descriptor.fileDescriptor).channel
                    if (channel.size() > 0L) {
                        return EpubSource.ofChannel(channel) { runCatching { descriptor.close() } }
                    }
                    runCatching { channel.close() }
                    runCatching { descriptor.close() }
                }
            }
            return EpubSource.ofStream(
                openStream = {
                    context.contentResolver.openInputStream(android.net.Uri.parse(sourcePath))
                        ?: throw IOException("Unable to read $displayName")
                },
                cacheDir = File(context.cacheDir, "epub-index"),
            )
        }
        return EpubSource.ofFile(File(sourcePath))
    }

    /**
     * Runs [block] with a raw file descriptor for the book, or returns null
     * when the provider cannot expose one. Used to feed the native core.
     */
    fun <T> withFileDescriptor(sourcePath: String, block: (Int) -> T): T? {
        if (!isLinkedSourcePath(sourcePath)) return null
        return runCatching {
            context.contentResolver
                .openFileDescriptor(android.net.Uri.parse(sourcePath), "r")
                ?.use { descriptor -> block(descriptor.fd) }
        }.getOrNull()
    }
}
