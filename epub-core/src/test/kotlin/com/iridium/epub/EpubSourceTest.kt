package com.iridium.epub

import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Contract coverage for [EpubSource] slicing, edge reads, and cleanup:
 * the leak/edge fixes here guard every index/open path against regression.
 */
class EpubSourceTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `bytes source slices and clamps at eof`() {
        EpubSource.ofBytes(byteArrayOf(1, 2, 3, 4)).use { source ->
            assertEquals(4L, source.size)
            assertArrayEquals(byteArrayOf(2, 3), source.read(1, 2))
            assertArrayEquals(byteArrayOf(4), source.read(3, 99))
            assertArrayEquals(ByteArray(0), source.read(4, 1))
            assertArrayEquals(ByteArray(0), source.read(-1, 2))
            assertArrayEquals(ByteArray(0), source.read(0, 0))
            assertArrayEquals(ByteArray(0), source.read(0, -5))
        }
    }

    @Test
    fun `close is idempotent`() {
        val source = EpubSource.ofBytes(byteArrayOf(1))
        source.close()
        source.close()
        assertArrayEquals(byteArrayOf(1), source.read(0, 1))
    }

    @Test
    fun `file source round-trips and missing file throws`() {
        val file = temporaryFolder.newFile("a.epub").apply { writeBytes(byteArrayOf(9, 8, 7)) }
        EpubSource.ofFile(file).use { source ->
            assertEquals(3L, source.size)
            assertArrayEquals(byteArrayOf(8, 7), source.read(1, 9))
        }
        try {
            EpubSource.ofFile(File(temporaryFolder.root, "missing.epub"))
            fail("missing file must throw")
        } catch (_: Exception) {
        }
    }

    @Test
    fun `stream source spools and deletes the temp file on close`() {
        val cache = temporaryFolder.newFolder("spool")
        val source = EpubSource.ofStream(
            openStream = { "hello".byteInputStream() },
            cacheDir = cache,
        )
        assertEquals(5L, source.size)
        assertArrayEquals("hello".toByteArray(), source.read(0, 9))
        val temps = cache.listFiles()!!.toList()
        assertEquals(1, temps.size)
        source.close()
        assertTrue(cache.listFiles()!!.isEmpty())
    }
}
