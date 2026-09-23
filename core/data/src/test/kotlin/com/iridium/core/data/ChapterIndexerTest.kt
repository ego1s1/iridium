package com.iridium.core.data

import androidx.test.core.app.ApplicationProvider
import com.iridium.epub.engine.IridiumEpubEngine
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the chapter pipeline end to end against a real EPUB on disk: the
 * indexer opens the file, walks the spine, strips the XHTML and yields rows
 * ready for the FTS table.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChapterIndexerTest {

    private lateinit var indexer: ChapterIndexer
    private lateinit var tempDir: File

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        indexer = ChapterIndexer(EpubSourceFactory(context), IridiumEpubEngine())
        tempDir = File(context.cacheDir, "chapter-indexer-test").apply { mkdirs() }
    }

    @Test
    fun `extracts plain text for each spine chapter`() = runTest {
        val file = writeEpub()
        val rows = indexer.extract(file.absolutePath, "test.epub", toc = emptyList())

        assertEquals(2, rows.size)
        assertEquals("OEBPS/ch1.xhtml", rows[0].href)
        assertEquals("Chapter 1", rows[0].title)
        assertTrue(rows[0].body.contains("Hispaniola"))
        assertTrue("markup must be stripped", !rows[0].body.contains("<"))
        assertEquals("OEBPS/ch2.xhtml", rows[1].href)
        assertEquals("Chapter 2", rows[1].title)
    }

    @Test
    fun `uses toc titles when the href matches`() = runTest {
        val file = writeEpub()
        val rows = indexer.extract(
            file.absolutePath,
            "test.epub",
            toc = listOf(com.iridium.core.model.TocEntry("OEBPS/ch1.xhtml", "The Old Buccaneer")),
        )
        assertEquals("The Old Buccaneer", rows[0].title)
    }

    @Test
    fun `a missing file yields no rows instead of throwing`() = runTest {
        val rows = indexer.extract("/does/not/exist.epub", "missing.epub", toc = emptyList())
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `a non-epub file yields no rows instead of throwing`() = runTest {
        val file = File(tempDir, "garbage.epub").apply { writeBytes(ByteArray(2048) { 7 }) }
        val rows = indexer.extract(file.absolutePath, "garbage.epub", toc = emptyList())
        assertTrue(rows.isEmpty())
    }

    private fun writeEpub(): File {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            fun put(name: String, data: String) {
                zip.putNextEntry(ZipEntry(name)); zip.write(data.toByteArray()); zip.closeEntry()
            }
            put("mimetype", "application/epub+zip")
            put(
                "META-INF/container.xml",
                """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""",
            )
            put(
                "OEBPS/content.opf",
                """<?xml version="1.0"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Test</dc:title></metadata>
  <manifest>
    <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine><itemref idref="ch1"/><itemref idref="ch2"/></spine>
</package>""",
            )
            put(
                "OEBPS/ch1.xhtml",
                "<html><head><title>One</title></head><body><h1>Chapter One</h1>" +
                    "<p>The Hispaniola was rolling scuppers under in the ocean swell.</p></body></html>",
            )
            put(
                "OEBPS/ch2.xhtml",
                "<html><body><h1>Chapter Two</h1><p>Wild stone spires stood above the grey woods.</p></body></html>",
            )
        }
        return File(tempDir, "test-${System.nanoTime()}.epub").apply { writeBytes(bos.toByteArray()) }
    }
}
