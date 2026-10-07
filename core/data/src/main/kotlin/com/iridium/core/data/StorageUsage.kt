package com.iridium.core.data

import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Storage accounting for the settings Storage hero: how many books are kept
 * and how many bytes each on-disk bucket occupies.
 */
data class StorageUsage(
    val bookCount: Int,
    val libraryBytes: Long,
    val coversBytes: Long,
    val cacheBytes: Long,
) {
    /** Sum of [libraryBytes], [coversBytes] and [cacheBytes]. */
    val totalBytes: Long get() = libraryBytes + coversBytes + cacheBytes
}

/**
 * Human-readable byte count in B/KB/MB/GB with one decimal, US locale.
 * Negative input is clamped to zero.
 */
fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    if (safe < 1024L) return "$safe B"
    val units = arrayOf("KB", "MB", "GB")
    var value = safe.toDouble()
    var unit = units.first()
    for (next in units) {
        value /= 1024.0
        unit = next
        if (value < 1024.0 || next == units.last()) break
    }
    return String.format(Locale.US, "%.1f %s", value, unit)
}

private fun dirSize(dir: File): Long {
    if (!dir.exists()) return 0L
    return runCatching {
        dir.walkTopDown().filter { it.isFile }.sumOf { file ->
            runCatching { file.length() }.getOrDefault(0L)
        }
    }.getOrDefault(0L)
}

/**
 * Measures each storage bucket off the main thread. A missing or unreadable
 * directory contributes zero instead of throwing, so one bad bucket never
 * hides the others.
 */
suspend fun computeStorageUsage(
    libraryDir: File,
    coversDir: File,
    cacheDir: File,
    bookCount: Int,
): StorageUsage = withContext(Dispatchers.IO) {
    val library = async { dirSize(libraryDir) }
    val covers = async { dirSize(coversDir) }
    val cache = async { dirSize(cacheDir) }
    StorageUsage(
        bookCount = bookCount,
        libraryBytes = library.await(),
        coversBytes = covers.await(),
        cacheBytes = cache.await(),
    )
}
