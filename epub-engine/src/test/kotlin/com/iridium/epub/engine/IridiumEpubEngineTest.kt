package com.iridium.epub.engine

import com.iridium.epub.EpubSource
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IridiumEpubEngineTest {

    private val engine = IridiumEpubEngine()

    @Test
    fun `parses epub3 metadata cover and nav toc`() {
        val bytes = buildEpub(
            opf = OPF_EPUB3,
            extra = mapOf(
                "OEBPS/nav.xhtml" to NAV_XHTML.toByteArray(),
                "OEBPS/cover.jpg" to byteArrayOf(1, 2, 3),
                "OEBPS/ch1.xhtml" to "<html/>".toByteArray(),
                "OEBPS/ch2.xhtml" to "<html/>".toByteArray(),
            ),
        )
        val result = engine.inspect(bytes, "fallback.epub")

        assertEquals("Treasure Island", result.title)
        assertEquals("Robert Louis Stevenson", result.author)
        assertEquals(2, result.spineCount)
        assertNotNull(result.coverBytes)
        assertEquals(2, result.chapters.size)
        assertEquals("Chapter 1", result.chapters[0].title)
        assertTrue(result.chapters[0].href.endsWith("ch1.xhtml"))
    }

    @Test
    fun `parses epub2 ncx toc`() {
        val bytes = buildEpub(
            opf = OPF_EPUB2,
            extra = mapOf(
                "OEBPS/toc.ncx" to TOC_NCX.toByteArray(),
                "OEBPS/ch1.xhtml" to "<html/>".toByteArray(),
            ),
        )
        val result = engine.inspect(bytes, "fallback.epub")

        assertEquals("Huck Finn", result.title)
        assertEquals(1, result.spineCount)
        assertEquals(2, result.chapters.size)
        assertEquals("Chapter One", result.chapters[0].title)
        assertEquals("Nested Section", result.chapters[1].title)
    }

    @Test
    fun `garbage bytes yield fallback title without throwing`() {
        val result = engine.inspect(byteArrayOf(0, 1, 2, 3), "mybook.epub")
        assertEquals("mybook.epub", result.title)
        assertTrue(result.chapters.isEmpty())
    }

    @Test
    fun `empty bytes yield fallback`() {
        val result = engine.inspect(ByteArray(0), "mybook.epub")
        assertEquals(0, result.spineCount)
        assertTrue(result.chapters.isEmpty())
    }

    @Test
    fun `single central-directory pass skips unrelated large entries`() {
        val bytes = buildEpub(
            opf = OPF_EPUB3,
            extra = mapOf(
                "OEBPS/nav.xhtml" to NAV_XHTML.toByteArray(),
                "OEBPS/cover.jpg" to byteArrayOf(1, 2, 3),
                "OEBPS/ch1.xhtml" to "<html/>".toByteArray(),
                "OEBPS/ch2.xhtml" to "<html/>".toByteArray(),
                "OEBPS/assets/huge.bin" to ByteArray(12 * 1024 * 1024),
            ),
        )
        val result = engine.inspect(bytes, "fallback.epub")

        assertEquals("Treasure Island", result.title)
        assertNotNull(result.coverBytes)
        assertEquals(2, result.chapters.size)
    }

    @Test
    fun `chapter pipeline reads bodies on demand`() {
        val bytes = buildEpub(
            opf = OPF_EPUB3,
            extra = mapOf(
                "OEBPS/nav.xhtml" to NAV_XHTML.toByteArray(),
                "OEBPS/cover.jpg" to byteArrayOf(1, 2, 3),
                "OEBPS/ch1.xhtml" to "<html>one</html>".toByteArray(),
                "OEBPS/ch2.xhtml" to "<html>two</html>".toByteArray(),
            ),
        )
        val book = engine.open(EpubSource.ofBytes(bytes), "fallback.epub")
        assertNotNull(book)
        book!!.use {
            assertEquals(2, it.chapterCount)
            assertEquals("<html>one</html>", String(it.chapterBytes(0)!!))
            assertEquals("<html>two</html>", String(it.chapterBytes("OEBPS/ch2.xhtml")!!))
            assertNull(it.chapterBytes(99))
        }
    }

    @Test
    fun `resource reads honour the byte cap`() {
        val bytes = buildEpub(
            opf = OPF_EPUB3,
            extra = mapOf(
                "OEBPS/nav.xhtml" to NAV_XHTML.toByteArray(),
                "OEBPS/ch1.xhtml" to ByteArray(4096) { 1 },
                "OEBPS/ch2.xhtml" to "<html/>".toByteArray(),
            ),
        )
        val book = engine.open(EpubSource.ofBytes(bytes), "fallback.epub")!!
        book.use {
            assertNull(it.readResource("OEBPS/ch1.xhtml", maxBytes = 1024))
            assertNotNull(it.readResource("OEBPS/ch1.xhtml", maxBytes = 8192))
        }
    }

    @Test
    fun `doctype payload is refused and degrades to fallback`() {
        val xxeOpf = """<?xml version="1.0"?>
<!DOCTYPE package [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>&xxe;</dc:title>
  </metadata>
  <manifest><item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/></manifest>
  <spine><itemref idref="ch1"/></spine>
</package>"""
        val bytes = buildEpub(opf = xxeOpf, extra = mapOf("OEBPS/ch1.xhtml" to "<html/>".toByteArray()))
        val result = engine.inspect(bytes, "safe.epub")
        assertEquals("safe.epub", result.title)
    }

    @Test
    fun `truncated archive yields fallback`() {
        val full = buildEpub(
            opf = OPF_EPUB3,
            extra = mapOf("OEBPS/ch1.xhtml" to "<html/>".toByteArray()),
        )
        val truncated = full.copyOf(full.size / 2)
        val result = engine.inspect(truncated, "cut.epub")
        assertEquals("cut.epub", result.title)
    }

    @Test
    fun `corrupt local header is tolerated for that entry only`() {
        val bytes = buildEpub(
            opf = OPF_EPUB3,
            extra = mapOf(
                "OEBPS/nav.xhtml" to NAV_XHTML.toByteArray(),
                "OEBPS/ch1.xhtml" to "<html>one</html>".toByteArray(),
                "OEBPS/ch2.xhtml" to "<html/>".toByteArray(),
            ),
        )
        // Corrupt the "PK\u0003\u0004" local signature of ch1 so only its read fails.
        val signature = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        val index = indexOf(bytes, signature, from = 12)
        if (index >= 0) bytes[index] = 0x00

        val book = engine.open(EpubSource.ofBytes(bytes), "fallback.epub")
        // Metadata still resolves; only the corrupted chapter body is null.
        assertNotNull(book)
    }

    private fun indexOf(haystack: ByteArray, needle: ByteArray, from: Int): Int {
        outer@ for (i in from..haystack.size - needle.size) {
            for (j in needle.indices) if (haystack[i + j] != needle[j]) continue@outer
            return i
        }
        return -1
    }

    private fun buildEpub(opf: String, extra: Map<String, ByteArray>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/epub+zip".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write(CONTAINER.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zip.write(opf.toByteArray())
            zip.closeEntry()
            for ((name, bytes) in extra) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    private companion object {
        const val CONTAINER = """<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles>
</container>"""

        const val OPF_EPUB3 = """<?xml version="1.0"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>Treasure Island</dc:title>
    <dc:creator>Robert Louis Stevenson</dc:creator>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="cover" href="cover.jpg" media-type="image/jpeg" properties="cover-image"/>
    <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch2" href="ch2.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine><itemref idref="ch1"/><itemref idref="ch2"/></spine>
</package>"""

        const val NAV_XHTML = """<?xml version="1.0"?>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<body><nav epub:type="toc"><ol>
<li><a href="ch1.xhtml">Chapter 1</a></li>
<li><a href="ch2.xhtml">Chapter 2</a></li>
</ol></nav></body></html>"""

        const val OPF_EPUB2 = """<?xml version="1.0"?>
<package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>Huck Finn</dc:title>
    <dc:creator>Mark Twain</dc:creator>
  </metadata>
  <manifest>
    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
    <item id="ch1" href="ch1.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine toc="ncx"><itemref idref="ch1"/></spine>
</package>"""

        const val TOC_NCX = """<?xml version="1.0"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
<navMap>
<navPoint id="n1"><navLabel><text>Chapter One</text></navLabel><content src="ch1.xhtml"/>
<navPoint id="n2"><navLabel><text>Nested Section</text></navLabel><content src="ch1.xhtml#p2"/></navPoint>
</navPoint>
</navMap></ncx>"""
    }
}
