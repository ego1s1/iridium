package com.iridium.core.data

import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Filesystem walk: finds EPUBs, skips private/hidden trees, never prunes
 * on failure. Pure java.io against temp dirs (no Android APIs except Uri).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FilesystemLinkedTreeListerTest {

    private lateinit var root: File
    private val lister = FilesystemLinkedTreeLister()

    @Before
    fun setup() {
        root = createTempDir("fs-lister").apply { deleteOnExit() }
    }

    @Test
    fun `finds epubs recursively`() = runTest {
        write("a.epub")
        write("sub/b.epub")
        write("sub/deep/c.epub")
        write("notes.txt")

        val result = lister.listBooks(root)

        assertFalse(result.walkFailed)
        assertEquals(
            setOf("a.epub", "b.epub", "c.epub"),
            result.documents.map { it.name }.toSet(),
        )
    }

    @Test
    fun `matches epub extension case-insensitively`() = runTest {
        write("upper.EPUB")
        write("mixed.Epub")

        val result = lister.listBooks(root)

        assertEquals(2, result.documents.size)
    }

    @Test
    fun `skips Android and hidden directories`() = runTest {
        write("Android/data/x.epub")
        write(".hidden/y.epub")
        write(".thumbnails/z.epub")
        write("keep/w.epub")

        val result = lister.listBooks(root)

        assertFalse(result.walkFailed)
        assertEquals(listOf("w.epub"), result.documents.map { it.name })
    }

    @Test
    fun `documents carry file uri name and modified marker`() = runTest {
        val file = write("book.epub")
        file.setLastModified(123456789L)

        val result = lister.listBooks(root)

        val doc = result.documents.single()
        assertEquals("book.epub", doc.name)
        assertEquals(123456789L, doc.modified)
        assertTrue(doc.uri.toString().endsWith("book.epub"))
    }

    @Test
    fun `resolve returns null for missing files`() = runTest {
        val missing = android.net.Uri.fromFile(File(root, "gone.epub"))

        assertEquals(null, lister.resolve(missing))
    }

    @Test
    fun `resolve reads live files`() = runTest {
        val file = write("live.epub")

        val doc = lister.resolve(android.net.Uri.fromFile(file))

        checkNotNull(doc)
        assertEquals("live.epub", doc.name)
    }

    private fun write(relative: String): File {
        val file = File(root, relative)
        file.parentFile?.mkdirs()
        file.writeText("fake-epub")
        file.deleteOnExit()
        return file
    }
}
