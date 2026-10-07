package com.iridium.core.data

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StorageUsageTest {

    @Test
    fun `formats zero and bare bytes`() {
        assertEquals("0 B", formatBytes(0L))
        assertEquals("1 B", formatBytes(1L))
        assertEquals("1023 B", formatBytes(1023L))
    }

    @Test
    fun `formats kilobytes at the 1024 boundary`() {
        assertEquals("1.0 KB", formatBytes(1024L))
        assertEquals("1.5 KB", formatBytes(1536L))
    }

    @Test
    fun `formats megabytes`() {
        assertEquals("1.0 MB", formatBytes(1024L * 1024L))
    }

    @Test
    fun `formats gigabytes`() {
        assertEquals("1.0 GB", formatBytes(1024L * 1024L * 1024L))
        assertEquals("2.5 GB", formatBytes((2.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun `clamps negative input to zero`() {
        assertEquals("0 B", formatBytes(-1L))
    }

    @Test
    fun `totalBytes sums the three buckets`() {
        val usage = StorageUsage(bookCount = 3, libraryBytes = 100L, coversBytes = 20L, cacheBytes = 5L)
        assertEquals(125L, usage.totalBytes)
    }

    @Test
    fun `computes sizes from temp dirs with known files`() = runTest {
        val root = Files.createTempDirectory("storage-usage-test").toFile()
        try {
            val library = File(root, "library").apply { mkdirs() }
            val covers = File(root, "covers").apply { mkdirs() }
            val cache = File(root, "cache").apply { mkdirs() }
            File(library, "a.epub").writeBytes(ByteArray(2048))
            File(library, "nested").apply { mkdirs() }
            File(File(library, "nested"), "b.epub").writeBytes(ByteArray(1024))
            File(covers, "c1.jpg").writeBytes(ByteArray(512))
            // Cache intentionally left empty.

            val usage = computeStorageUsage(library, covers, cache, bookCount = 2)

            assertEquals(2, usage.bookCount)
            assertEquals(3072L, usage.libraryBytes)
            assertEquals(512L, usage.coversBytes)
            assertEquals(0L, usage.cacheBytes)
            assertEquals(3584L, usage.totalBytes)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `missing dirs yield zero instead of throwing`() = runTest {
        val root = Files.createTempDirectory("storage-usage-missing").toFile()
        try {
            val usage = computeStorageUsage(
                File(root, "no-library"),
                File(root, "no-covers"),
                File(root, "no-cache"),
                bookCount = 0,
            )
            assertEquals(0L, usage.libraryBytes)
            assertEquals(0L, usage.coversBytes)
            assertEquals(0L, usage.cacheBytes)
            assertEquals(0L, usage.totalBytes)
        } finally {
            root.deleteRecursively()
        }
    }
}
