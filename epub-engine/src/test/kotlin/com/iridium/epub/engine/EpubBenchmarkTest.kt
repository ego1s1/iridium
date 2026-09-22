package com.iridium.epub.engine

import com.iridium.epub.EpubSource
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cold-scan benchmark. Builds a corpus of synthetic EPUBs and reports
 * per-book inspection cost, comparing against the JDK's own random-access
 * `ZipFile` reader as a sanity baseline.
 *
 * The assertion is deliberately loose: this guards against ordering-of-
 * magnitude regressions (an accidental full-archive read, a lost fast path)
 * without flaking on shared CI hardware.
 */
class EpubBenchmarkTest {

    @Test
    fun `cold scan stays within the budget`() {
        val corpus = buildCorpus(count = 40, chapterCount = 12, chapterBytes = 24 * 1024)
        val engine = IridiumEpubEngine()

        // Warm up the JIT so we measure steady-state parsing, not class loading.
        repeat(5) { corpus.forEach { engine.inspect(EpubSource.ofBytes(it), "warmup") } }

        val samples = LongArray(corpus.size)
        var index = 0L
        corpus.forEach { bytes ->
            val start = System.nanoTime()
            EpubSource.ofBytes(bytes).use { engine.inspect(it, "bench") }
            samples[index.toInt()] = System.nanoTime() - start
            index++
        }

        val totalBytes = corpus.sumOf { it.size.toLong() }
        val sorted = samples.sortedArray()
        val medianMs = sorted[sorted.size / 2] / 1_000_000.0
        val p95Ms = sorted[(sorted.size * 95) / 100] / 1_000_000.0
        val throughput = corpus.size / (sorted.sum() / 1_000_000_000.0)

        println(
            "epub-engine cold scan: books=${corpus.size} " +
                "median=${"%.2f".format(medianMs)}ms p95=${"%.2f".format(p95Ms)}ms " +
                "throughput=${"%.0f".format(throughput)} books/s " +
                "corpus=${totalBytes / 1024}KB",
        )

        // A cold inspect is a few small reads; anything near a full scan shows up here.
        assertTrue("median inspect ${medianMs}ms exceeded budget", medianMs < 25.0)
    }

    @Test
    fun `jdk ZipFile baseline for reference`() {
        val corpus = buildCorpus(count = 20, chapterCount = 12, chapterBytes = 24 * 1024)
        repeat(5) { corpus.forEach { bytes -> baselineScan(bytes) } }
        val samples = LongArray(corpus.size)
        corpus.forEachIndexed { i, bytes ->
            val start = System.nanoTime()
            baselineScan(bytes)
            samples[i] = System.nanoTime() - start
        }
        val medianMs = samples.sortedArray()[samples.size / 2] / 1_000_000.0
        println("jdk ZipFile baseline: median=${"%.2f".format(medianMs)}ms")
    }

    /** Opens the archive with the JDK reader and touches the same entries. */
    private fun baselineScan(bytes: ByteArray) {
        val temp = java.io.File.createTempFile("bench", ".epub")
        try {
            temp.writeBytes(bytes)
            ZipFile(temp).use { zip ->
                zip.getEntry("META-INF/container.xml")?.let { zip.getInputStream(it).readBytes() }
                zip.getEntry("OEBPS/content.opf")?.let { zip.getInputStream(it).readBytes() }
                zip.getEntry("OEBPS/nav.xhtml")?.let { zip.getInputStream(it).readBytes() }
                zip.getEntry("OEBPS/cover.jpg")?.let { zip.getInputStream(it).readBytes() }
            }
        } finally {
            temp.delete()
        }
    }

    private fun buildCorpus(count: Int, chapterCount: Int, chapterBytes: Int): List<ByteArray> =
        List(count) { index ->
            val chapters = (0 until chapterCount).associate { n ->
                "OEBPS/ch$n.xhtml" to buildString {
                    append("<html><body>")
                    repeat(chapterBytes / 32) { append("<p>Chapter $n paragraph.</p>") }
                    append("</body></html>")
                }.toByteArray()
            }
            val manifest = buildString {
                append("""<item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>""")
                append("""<item id="cover" href="cover.jpg" media-type="image/jpeg" properties="cover-image"/>""")
                (0 until chapterCount).forEach { n ->
                    append("""<item id="ch$n" href="ch$n.xhtml" media-type="application/xhtml+xml"/>""")
                }
            }
            val spine = (0 until chapterCount).joinToString("") { """<itemref idref="ch$it"/>""" }
            val opf = """<?xml version="1.0"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:title>Benchmark Book $index</dc:title>
    <dc:creator>Bench</dc:creator>
  </metadata>
  <manifest>$manifest</manifest>
  <spine>$spine</spine>
</package>"""
            val nav = buildString {
                append("""<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><body><nav epub:type="toc"><ol>""")
                (0 until chapterCount).forEach { n -> append("""<li><a href="ch$n.xhtml">Chapter $n</a></li>""") }
                append("</ol></nav></body></html>")
            }
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
                chapters.forEach { (name, data) -> put(name, data) }
            }
            bos.toByteArray()
        }
}
