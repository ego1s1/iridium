package com.iridium.epub.nativecore

import com.iridium.epub.EpubSource
import com.iridium.epub.engine.IridiumEpubEngine
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Head-to-head cold-scan benchmark: native core versus the JVM engine on an
 * identical corpus. Purely informational (no timing assertions) — it exists so
 * the native path's value is measurable rather than assumed.
 */
class NativeBenchmarkTest {

    @Test
    fun `native versus jvm cold scan`() {
        assumeTrue("Native core unavailable; skipping.", NativeEpub.isAvailable)

        val corpus = List(30) { index -> epub(index) }
        val files = corpus.map { bytes ->
            File.createTempFile("bench-native-", ".epub").apply { writeBytes(bytes) }
        }
        val engine = IridiumEpubEngine()

        // Warm up both paths.
        repeat(5) {
            corpus.forEach { EpubSource.ofBytes(it).use { s -> engine.inspect(s, "w") } }
            files.forEach { file -> fd(file).use { handle -> NativeEpub.inspectFd(handle.fd, "w") } }
        }

        val nativeSamples = LongArray(files.size)
        files.forEachIndexed { i, file ->
            val start = System.nanoTime()
            fd(file).use { handle -> NativeEpub.inspectFd(handle.fd, "bench") }
            nativeSamples[i] = System.nanoTime() - start
        }

        val jvmSamples = LongArray(corpus.size)
        corpus.forEachIndexed { i, bytes ->
            val start = System.nanoTime()
            EpubSource.ofBytes(bytes).use { s -> engine.inspect(s, "bench") }
            jvmSamples[i] = System.nanoTime() - start
        }

        val nativeMedian = nativeSamples.sortedArray()[nativeSamples.size / 2] / 1_000_000.0
        val jvmMedian = jvmSamples.sortedArray()[jvmSamples.size / 2] / 1_000_000.0
        println(
            "epub cold scan (n=${corpus.size}, no file I/O in JVM path): " +
                "native median=${"%.2f".format(nativeMedian)}ms " +
                "jvm median=${"%.2f".format(jvmMedian)}ms",
        )

        files.forEach { it.delete() }
        assertEquals(corpus.size, jvmSamples.size)
    }

    private fun fd(file: File): FdHandle = FdHandle(FileInputStream(file))

    private class FdHandle(private val stream: FileInputStream) : AutoCloseable {
        val fd: Int get() {
            val field = FileDescriptor::class.java.getDeclaredField("fd")
            field.isAccessible = true
            return field.getInt(stream.fd)
        }

        override fun close() = stream.close()
    }

    private fun epub(index: Int): ByteArray {
        val chapters = 12
        val bodies = (0 until chapters).associate { n ->
            "OEBPS/ch$n.xhtml" to buildString {
                append("<html><body>")
                repeat(1200) { append("<p>Chapter $n paragraph $it.</p>") }
                append("</body></html>")
            }.toByteArray()
        }
        val manifest = buildString {
            append("""<item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>""")
            append("""<item id="cover" href="cover.jpg" media-type="image/jpeg" properties="cover-image"/>""")
            (0 until chapters).forEach {
                append("""<item id="ch$it" href="ch$it.xhtml" media-type="application/xhtml+xml"/>""")
            }
        }
        val spine = (0 until chapters).joinToString("") { """<itemref idref="ch$it"/>""" }
        val nav = buildString {
            append("""<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><body><nav epub:type="toc"><ol>""")
            (0 until chapters).forEach { append("""<li><a href="ch$it.xhtml">Chapter $it</a></li>""") }
            append("</ol></nav></body></html>")
        }
        val opf = """<?xml version="1.0"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>Benchmark $index</dc:title><dc:creator>Bench</dc:creator>
  </metadata>
  <manifest>$manifest</manifest><spine>$spine</spine>
</package>"""
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            fun put(name: String, data: ByteArray) {
                zip.putNextEntry(ZipEntry(name)); zip.write(data); zip.closeEntry()
            }
            put("mimetype", "application/epub+zip".toByteArray())
            put(
                "META-INF/container.xml",
                """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>"""
                    .toByteArray(),
            )
            put("OEBPS/content.opf", opf.toByteArray())
            put("OEBPS/nav.xhtml", nav.toByteArray())
            put("OEBPS/cover.jpg", ByteArray(18 * 1024) { 7 })
            bodies.forEach { (name, data) -> put(name, data) }
        }
        return bos.toByteArray()
    }
}
