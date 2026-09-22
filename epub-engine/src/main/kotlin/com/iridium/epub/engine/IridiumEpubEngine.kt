package com.iridium.epub.engine

import com.iridium.epub.EpubBackend
import com.iridium.epub.EpubChapter
import com.iridium.epub.EpubSource
import com.iridium.epub.InspectedEpub
import java.io.Closeable

/**
 * High-performance EPUB engine.
 *
 * One central-directory read gives the whole entry table; the OPF, navigation
 * document, NCX and cover are then sliced by offset. No full-archive pass, no
 * DOM, no intermediate maps of the entire book — inspection cost is a handful
 * of small reads regardless of book size.
 *
 * Stability contract: [inspect] never throws (malformed archives degrade to a
 * fallback title), every read is bounded, and [open] returns `null` rather than
 * surfacing a parse error. EPUB 2 and EPUB 3 reflowable are supported; fixed
 * layout and encrypted containers are reported as unsupported, not crashes.
 */
class IridiumEpubEngine : EpubBackend {

    override fun inspect(source: EpubSource, fallbackTitle: String): InspectedEpub =
        parse(source, fallbackTitle)?.inspected ?: fallback(fallbackTitle)

    /**
     * Opens the book and keeps the source alive for on-demand chapter reads.
     * Callers own the returned [EpubBook] and must close it.
     */
    fun open(source: EpubSource, fallbackTitle: String): EpubBook? {
        val parsed = parse(source, fallbackTitle) ?: return null
        return EpubBook(source, parsed.archive, parsed.inspected, parsed.spineEntries)
    }

    // MARK: Parsing

    private class Parsed(
        val archive: ZipArchive,
        val inspected: InspectedEpub,
        val spineEntries: List<ZipEntry>,
    )

    private fun parse(source: EpubSource, fallbackTitle: String): Parsed? = try {
        parseOrThrow(source, fallbackTitle)
    } catch (_: Exception) {
        null
    }

    private fun parseOrThrow(source: EpubSource, fallbackTitle: String): Parsed? {
        val archive = ZipArchive.parse(source) ?: return null
        val opfPath = resolveOpfPath(archive) ?: return null
        val opfBytes = archive.read(opfPath) ?: return null
        val opf = OpfSax.parse(opfBytes) ?: return null
        val base = opfPath.substringBeforeLast('/', "")

        val title = opf.title?.takeIf { it.isNotBlank() } ?: fallbackTitle.ifBlank { "Unknown" }
        val cover = readCover(archive, opf, base)
        val chapters = readChapters(archive, opf, base)
        val spineEntries = opf.spine.mapNotNull { id ->
            opf.manifest[id]?.let { archive.findEntry(resolve(base, it.href)) }
        }

        val inspected = InspectedEpub(
            title = title,
            author = opf.author,
            coverBytes = cover?.first,
            coverMime = cover?.second,
            spineCount = spineEntries.size,
            chapters = chapters,
        )
        return Parsed(archive, inspected, spineEntries)
    }

    private fun resolveOpfPath(archive: ZipArchive): String? {
        val container = archive.read(CONTAINER_PATH, maxBytes = 1L * 1024 * 1024)
        container?.let { OpfSax.parseRootfile(it) }?.let { return it }
        return archive.entryNames.firstOrNull { it.endsWith(".opf", ignoreCase = true) }
    }

    private fun readCover(
        archive: ZipArchive,
        opf: OpfData,
        base: String,
    ): Pair<ByteArray, String>? {
        val href = coverHref(opf, base) ?: return null
        val entry = archive.findEntry(href) ?: return null
        val bytes = archive.read(entry, maxBytes = MAX_COVER_BYTES) ?: return null
        val mime = opf.manifest.values
            .firstOrNull { resolve(base, it.href) == href }
            ?.mediaType
            ?.takeIf { it.isNotEmpty() }
            ?: "image/jpeg"
        return bytes to mime
    }

    private fun coverHref(opf: OpfData, base: String): String? {
        opf.manifest.values.firstOrNull { it.properties.contains("cover-image") }
            ?.let { return resolve(base, it.href) }
        opf.coverId?.let { id -> opf.manifest[id]?.let { return resolve(base, it.href) } }
        opf.guideCoverHref?.let { return resolve(base, it) }
        return opf.manifest.values.firstOrNull { it.mediaType.startsWith("image/") }
            ?.let { resolve(base, it.href) }
    }

    private fun readChapters(archive: ZipArchive, opf: OpfData, base: String): List<EpubChapter> {
        // EPUB 3 navigation document.
        opf.manifest.values.firstOrNull { it.properties.contains("nav") }?.let { nav ->
            val navPath = resolve(base, nav.href)
            val bytes = archive.read(navPath, maxBytes = MAX_NAV_BYTES)
            if (bytes != null) {
                val navBase = navPath.substringBeforeLast('/', "")
                val chapters = NavSax.parseNav(bytes).map { (href, title) ->
                    EpubChapter(href = resolve(navBase, href), title = title)
                }
                if (chapters.isNotEmpty()) return chapters
            }
        }
        // EPUB 2 NCX.
        opf.manifest.values.firstOrNull { it.mediaType == NCX_MIME }?.let { ncx ->
            val ncxPath = resolve(base, ncx.href)
            val bytes = archive.read(ncxPath, maxBytes = MAX_NAV_BYTES)
            if (bytes != null) {
                val ncxBase = ncxPath.substringBeforeLast('/')
                val chapters = NavSax.parseNcx(bytes).map { (href, title) ->
                    EpubChapter(href = resolve(ncxBase, href), title = title)
                }
                if (chapters.isNotEmpty()) return chapters
            }
        }
        // Fallback: spine order with file-name labels.
        return opf.spine.mapNotNull { id ->
            val item = opf.manifest[id] ?: return@mapNotNull null
            val href = resolve(base, item.href)
            val label = href.substringAfterLast('/').substringBefore('#')
            EpubChapter(href = href, title = label.ifBlank { "Chapter" })
        }
    }

    private fun fallback(title: String) = InspectedEpub(title = title.ifBlank { "Unknown" })

    private companion object {
        const val CONTAINER_PATH = "META-INF/container.xml"
        const val NCX_MIME = "application/x-dtbncx+xml"
        const val MAX_COVER_BYTES = 16L * 1024 * 1024
        const val MAX_NAV_BYTES = 8L * 1024 * 1024
    }
}

/**
 * An opened book backing the chapter pipeline: metadata plus the spine, with
 * chapter bodies inflated on demand from the still-open [EpubSource]. This is
 * what future full-text search and content indexing read from — it never
 * re-parses the archive.
 */
class EpubBook internal constructor(
    private val source: EpubSource,
    internal val archive: ZipArchive,
    val inspected: InspectedEpub,
    private val spineEntries: List<ZipEntry>,
) : Closeable {

    val chapterCount: Int get() = spineEntries.size

    /** Chapter body by spine index, or null when unreadable/oversized. */
    fun chapterBytes(index: Int): ByteArray? =
        spineEntries.getOrNull(index)?.let { archive.read(it) }

    /** Chapter body by resolved href, or null when unreadable/oversized. */
    fun chapterBytes(href: String): ByteArray? =
        archive.findEntry(href)?.let { archive.read(it) }

    /** Chapter hrefs in spine order. */
    fun chapterHrefs(): List<String> = spineEntries.map { it.name }

    fun readResource(href: String, maxBytes: Long = MAX_RESOURCE_BYTES): ByteArray? =
        archive.findEntry(href)?.let { archive.read(it, maxBytes) }

    override fun close() {
        source.close()
    }

    private companion object {
        const val MAX_RESOURCE_BYTES = 64L * 1024 * 1024
    }
}

/** Resolves an href against a base directory, normalizing `.` and `..`. */
internal fun resolve(base: String, href: String): String {
    val clean = href.substringBefore('#').trim()
    if (clean.isEmpty()) return ""
    val rel = clean.removePrefix("/")
    if (base.isEmpty()) return rel
    val parts = ArrayList<String>()
    for (segment in base.split('/') + rel.split('/')) {
        when (segment) {
            "", "." -> Unit
            ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
            else -> parts += segment
        }
    }
    return parts.joinToString("/")
}
