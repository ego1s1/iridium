package com.iridium.epub.nativecore

import com.iridium.epub.engine.IridiumEpubEngine
import com.iridium.epub.EpubSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Exercises the native core on the host JVM.
 *
 * The shared library is compiled for this platform by the `compileHostNative`
 * task and resolved through `java.library.path`, so these tests run the real
 * C++ parser rather than the JVM fallback. When the toolchain is unavailable
 * the suite is skipped rather than failing the build.
 */
class NativeEpubTest {

    @Test
    fun `native library loads on the host`() {
        assumeNative()
        assertTrue(NativeEpub.isAvailable)
    }

    @Test
    fun `inspects epub3 metadata and toc`() {
        assumeNative()
        val file = writeTemp(epub3())
        val inspected = fdOf(file).use { handle -> NativeEpub.inspectFd(handle.fd, "fallback") }

        assertNotNull(inspected)
        inspected!!
        assertEquals("Treasure Island", inspected.title)
        assertEquals("Robert Louis Stevenson", inspected.author)
        assertEquals(2, inspected.spineCount)
        assertEquals(2, inspected.chapters.size)
        assertEquals("Chapter 1", inspected.chapters[0].title)
        assertEquals("OEBPS/ch1.xhtml", inspected.chapters[0].href)
        assertNotNull(inspected.coverBytes)
    }

    @Test
    fun `inspects epub2 ncx toc`() {
        assumeNative()
        val file = writeTemp(epub2())
        val inspected = fdOf(file).use { handle -> NativeEpub.inspectFd(handle.fd, "fallback") }

        assertNotNull(inspected)
        assertEquals("Huck Finn", inspected!!.title)
        assertEquals(2, inspected.chapters.size)
        assertEquals("Chapter One", inspected.chapters[0].title)
    }

    @Test
    fun `chapter pipeline reads a body by href`() {
        assumeNative()
        val file = writeTemp(epub3())
        fdOf(file).use { handle ->
            val bytes = NativeEpub.chapterFd(handle.fd, "OEBPS/ch1.xhtml")
            assertNotNull(bytes)
            assertEquals("<html>one</html>", String(bytes!!))
        }
    }

    @Test
    fun `native agrees with the jvm engine on the same archive`() {
        assumeNative()
        val bytes = epub3()

        val native = fdOf(writeTemp(bytes)).use { handle -> NativeEpub.inspectFd(handle.fd, "f") }
        val jvm = EpubSource.ofBytes(bytes).use { IridiumEpubEngine().inspect(it, "f") }

        assertNotNull(native)
        assertEquals(jvm.title, native!!.title)
        assertEquals(jvm.author, native.author)
        assertEquals(jvm.spineCount, native.spineCount)
        assertEquals(jvm.chapters.map { it.href }, native.chapters.map { it.href })
        assertEquals(jvm.chapters.map { it.title }, native.chapters.map { it.title })
    }

    @Test
    fun `malformed archive returns null instead of crashing`() {
        assumeNative()
        val garbage = ByteArray(4096) { (it % 251).toByte() }
        val result = fdOf(writeTemp(garbage)).use { handle -> NativeEpub.inspectFd(handle.fd, "safe") }
        // The native core declines, and the caller falls back to the JVM engine.
        assertEquals(null, result)
    }

    // MARK: Helpers

    private fun assumeNative() {
        assumeTrue(
            "Native core unavailable (host library not built); skipping.",
            NativeEpub.isAvailable,
        )
    }

    private class FdHandle(private val stream: FileInputStream) : AutoCloseable {
        val fd: Int get() {
            val field = FileDescriptor::class.java.getDeclaredField("fd")
            field.isAccessible = true
            return field.getInt(stream.fd)
        }

        override fun close() = stream.close()
    }

    private fun fdOf(file: File) = FdHandle(FileInputStream(file))

    private fun writeTemp(bytes: ByteArray): File =
        File.createTempFile("native-epub-", ".epub").apply { writeBytes(bytes) }

    private fun epub3(): ByteArray = buildEpub(
        opf = OPF_EPUB3,
        extra = mapOf(
            "OEBPS/nav.xhtml" to NAV_XHTML.toByteArray(),
            "OEBPS/cover.jpg" to byteArrayOf(1, 2, 3),
            "OEBPS/ch1.xhtml" to "<html>one</html>".toByteArray(),
            "OEBPS/ch2.xhtml" to "<html>two</html>".toByteArray(),
        ),
    )

    private fun epub2(): ByteArray = buildEpub(
        opf = OPF_EPUB2,
        extra = mapOf(
            "OEBPS/toc.ncx" to TOC_NCX.toByteArray(),
            "OEBPS/ch1.xhtml" to "<html/>".toByteArray(),
        ),
    )

    private fun buildEpub(opf: String, extra: Map<String, ByteArray>): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            fun put(name: String, data: ByteArray) {
                zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry()
            }
            put("mimetype", "application/epub+zip".toByteArray())
            put("META-INF/container.xml", CONTAINER.toByteArray())
            put("OEBPS/content.opf", opf.toByteArray())
            extra.forEach { (name, data) -> put(name, data) }
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
