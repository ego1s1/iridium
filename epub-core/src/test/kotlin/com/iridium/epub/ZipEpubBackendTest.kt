package com.iridium.epub

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZipEpubBackendTest {

    private val backend = ZipEpubBackend()

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
        val result = backend.inspect(bytes, "fallback.epub")

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
        val result = backend.inspect(bytes, "fallback.epub")

        assertEquals("Huck Finn", result.title)
        assertEquals(1, result.spineCount)
        assertEquals(2, result.chapters.size)
        assertEquals("Chapter One", result.chapters[0].title)
        assertEquals("Nested Section", result.chapters[1].title)
    }

    @Test
    fun `garbage bytes yield fallback title without throwing`() {
        val result = backend.inspect(byteArrayOf(0, 1, 2, 3), "mybook.epub")
        assertEquals("mybook.epub", result.title)
        assertTrue(result.chapters.isEmpty())
    }

    @Test
    fun `empty bytes yield empty error signal`() {
        val result = backend.inspect(ByteArray(0), "mybook.epub")
        assertEquals(0, result.spineCount)
        assertTrue(result.chapters.isEmpty())
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
