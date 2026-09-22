package com.iridium.epub

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Minimal EPUB 2/3 parser over a re-openable ZIP stream.
 *
 * The archive is read in bounded passes instead of being loaded whole: pass
 * one pulls only the container/OPF XML, pass two pulls only the navigation
 * document and cover image named by the manifest. Bytes for everything else
 * are skipped, never materialized, so a large book costs the same memory as a
 * small one. Defensive throughout: XXE disabled, per-entry and total caps,
 * never throws — failures yield the fallback title.
 */
class ZipEpubBackend : EpubBackend {

    override fun inspect(openStream: () -> InputStream, fallbackTitle: String): InspectedEpub {
        return try {
            inspectStreaming(openStream, fallbackTitle)
        } catch (_: Exception) {
            InspectedEpub(title = fallbackTitle.ifBlank { "Unknown" })
        }
    }

    private fun inspectStreaming(
        openStream: () -> InputStream,
        fallbackTitle: String,
    ): InspectedEpub {
        val fallback = fallbackTitle.ifBlank { "Unknown" }

        // Pass 1: container.xml + the package document (both small XML).
        var containerBytes: ByteArray? = null
        var opfBytes: ByteArray? = null
        var opfPath: String? = null
        scan(
            openStream = openStream,
            shouldRead = { name ->
                name == CONTAINER_PATH || name.endsWith(".opf", ignoreCase = true)
            },
            onEntry = { name, bytes ->
                when {
                    name == CONTAINER_PATH -> containerBytes = bytes
                    name.endsWith(".opf", ignoreCase = true) && opfBytes == null -> {
                        opfBytes = bytes
                        opfPath = name
                    }
                }
            },
        )

        // Prefer the OPF named by the container; otherwise the first one found.
        val containerPath = containerBytes?.let(::parseRootfilePath)
        val effectiveOpfPath = containerPath ?: opfPath ?: return InspectedEpub(title = fallback)
        val effectiveOpfBytes = when {
            containerPath != null && containerPath == opfPath -> opfBytes
            containerPath != null -> readSingleEntry(openStream, containerPath)
            else -> opfBytes
        } ?: return InspectedEpub(title = fallback)

        val base = effectiveOpfPath.substringBeforeLast('/', "")
        val opf = parseXml(effectiveOpfBytes)
        val title = opf.firstText("title").ifBlank { fallback }
        val author = opf.firstText("creator").ifBlank { null }
        val manifest = readManifest(opf, base)
        val spine = readSpine(opf)

        // Names we still need: navigation document, NCX, and cover image.
        val navHref = manifest.values.firstOrNull { it.properties.contains("nav") }?.href
        val ncxHref = manifest.values.firstOrNull {
            it.mime == "application/x-dtbncx+xml"
        }?.href
        val coverHref = resolveCoverHref(opf, manifest)

        val wanted = setOfNotNull(navHref, ncxHref, coverHref).toSet()
        val bodies = mutableMapOf<String, ByteArray>()
        if (wanted.isNotEmpty()) {
            scan(
                openStream = openStream,
                shouldRead = { it in wanted },
                onEntry = { name, bytes -> bodies[name] = bytes },
            )
        }

        val cover = coverHref?.let { href ->
            bodies[href]?.let { bytes ->
                val mime = manifest.values.firstOrNull { it.href == href }?.mime ?: "image/jpeg"
                bytes to mime
            }
        }

        val chapters = readToc(opf, manifest, bodies, spine, navHref, ncxHref)

        return InspectedEpub(
            title = title,
            author = author,
            coverBytes = cover?.first,
            coverMime = cover?.second,
            spineCount = spine.size,
            chapters = chapters,
        )
    }

    // MARK: ZIP scanning

    /**
     * Streams [input] once, invoking [shouldRead] per entry name and
     * [onEntry] only for entries it approves (and that fit the caps).
     */
    private inline fun scan(
        openStream: () -> InputStream,
        shouldRead: (String) -> Boolean,
        onEntry: (String, ByteArray) -> Unit,
    ) {
        ZipInputStream(openStream()).use { zip ->
            var total = 0L
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && shouldRead(name)) {
                    val bytes = readCapped(zip) ?: return
                    total += bytes.size
                    if (total > MAX_TOTAL_BYTES) return
                    onEntry(name, bytes)
                }
                entry = zip.nextEntry
            }
        }
    }

    private fun readSingleEntry(openStream: () -> InputStream, path: String): ByteArray? {
        var result: ByteArray? = null
        scan(
            openStream = openStream,
            shouldRead = { it == path },
            onEntry = { _, bytes -> result = bytes },
        )
        return result
    }

    private inline fun readCapped(zip: ZipInputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var size = 0L
        var n = zip.read(buf)
        while (n != -1) {
            size += n
            if (size > MAX_SINGLE_ENTRY_BYTES) return null
            out.write(buf, 0, n)
            n = zip.read(buf)
        }
        return out.toByteArray()
    }

    // MARK: OPF

    private data class ManifestItem(
        val href: String,
        val mime: String,
        val properties: Set<String>,
    )

    private fun parseRootfilePath(containerXml: ByteArray): String? {
        val doc = parseXml(containerXml)
        val nodes = doc.getElementsByTagNameNS("*", "rootfile")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            val path = el.getAttribute("full-path").trim()
            if (path.isNotEmpty()) return path
        }
        return null
    }

    private fun readManifest(opf: Document, base: String): Map<String, ManifestItem> {
        val items = LinkedHashMap<String, ManifestItem>()
        val nodes = opf.getElementsByTagNameNS("*", "item")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            val id = el.getAttribute("id")
            val href = el.getAttribute("href").trim()
            if (id.isEmpty() || href.isEmpty()) continue
            items[id] = ManifestItem(
                href = resolve(base, href),
                mime = el.getAttribute("media-type").trim(),
                properties = el.getAttribute("properties").split(Regex("\\s+")).filter { it.isNotEmpty() }.toSet(),
            )
        }
        return items
    }

    private fun readSpine(opf: Document): List<String> {
        val ids = ArrayList<String>()
        val nodes = opf.getElementsByTagNameNS("*", "itemref")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            val idref = el.getAttribute("idref")
            if (idref.isNotEmpty()) ids += idref
        }
        return ids
    }

    private fun resolveCoverHref(opf: Document, manifest: Map<String, ManifestItem>): String? {
        // EPUB3: manifest properties="cover-image".
        manifest.values.firstOrNull { it.properties.contains("cover-image") }?.let { return it.href }
        // EPUB2: <meta name="cover" content="id"/>.
        val metas = opf.getElementsByTagNameNS("*", "meta")
        for (i in 0 until metas.length) {
            val el = metas.item(i) as? Element ?: continue
            if (el.getAttribute("name").equals("cover", ignoreCase = true)) {
                manifest[el.getAttribute("content")]?.let { return it.href }
            }
        }
        // guide <reference type="cover"/>.
        val refs = opf.getElementsByTagNameNS("*", "reference")
        for (i in 0 until refs.length) {
            val el = refs.item(i) as? Element ?: continue
            if (el.getAttribute("type").equals("cover", ignoreCase = true)) {
                val href = el.getAttribute("href")
                if (href.isNotEmpty()) {
                    val base = manifest.values.firstOrNull()?.href?.substringBeforeLast('/', "") ?: ""
                    return resolve(base, href)
                }
            }
        }
        // Last resort: first image in the manifest.
        return manifest.values.firstOrNull { it.mime.startsWith("image/") }?.href
    }

    // MARK: TOC

    private fun readToc(
        opf: Document,
        manifest: Map<String, ManifestItem>,
        bodies: Map<String, ByteArray>,
        spine: List<String>,
        navHref: String?,
        ncxHref: String?,
    ): List<EpubChapter> {
        if (navHref != null) {
            bodies[navHref]?.let { bytes ->
                val chapters = parseNavToc(bytes, navHref.substringBeforeLast('/', ""))
                if (chapters.isNotEmpty()) return chapters
            }
        }
        if (ncxHref != null) {
            bodies[ncxHref]?.let { bytes ->
                val chapters = parseNcxToc(bytes, ncxHref.substringBeforeLast('/', ""))
                if (chapters.isNotEmpty()) return chapters
            }
        }
        // Fallback: spine order with file-name labels.
        return spine.mapNotNull { id ->
            val item = manifest[id] ?: return@mapNotNull null
            val label = item.href.substringAfterLast('/').substringBefore('#')
            EpubChapter(href = item.href, title = label.ifBlank { "Chapter" })
        }
    }

    private fun parseNavToc(navBytes: ByteArray, base: String): List<EpubChapter> {
        val out = ArrayList<EpubChapter>()
        val doc = parseXml(navBytes)
        val navs = doc.getElementsByTagNameNS("*", "nav")
        for (i in 0 until navs.length) {
            val el = navs.item(i) as? Element ?: continue
            val type = el.getAttributeNS("http://www.idpf.org/2007/ops", "type")
                .ifEmpty { el.getAttribute("type") }
            if (!type.split(Regex("\\s+")).contains("toc")) continue
            val links = el.getElementsByTagNameNS("*", "a")
            for (j in 0 until links.length) {
                if (out.size >= MAX_TOC_ENTRIES) break
                val a = links.item(j) as? Element ?: continue
                val href = a.getAttribute("href").trim()
                val title = a.textContent.trim().replace(Regex("\\s+"), " ")
                if (href.isNotEmpty() && title.isNotEmpty()) {
                    out += EpubChapter(href = resolve(base, href), title = title)
                }
            }
            break
        }
        return out
    }

    private fun parseNcxToc(ncxBytes: ByteArray, base: String): List<EpubChapter> {
        val out = ArrayList<EpubChapter>()
        val doc = parseXml(ncxBytes)
        val points = doc.getElementsByTagNameNS("*", "navPoint")
        for (i in 0 until points.length) {
            if (out.size >= MAX_TOC_ENTRIES) break
            val el = points.item(i) as? Element ?: continue
            val parent = el.parentNode
            if (parent is Element && parent.tagName.endsWith("navPoint")) continue
            collectNcxPoint(el, base, out)
        }
        return out
    }

    private fun collectNcxPoint(el: Element, base: String, out: MutableList<EpubChapter>) {
        val labels = el.getElementsByTagNameNS("*", "text")
        val title = (0 until labels.length)
            .map { labels.item(it).textContent.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?.replace(Regex("\\s+"), " ")
        val contents = el.getElementsByTagNameNS("*", "content")
        val src = (0 until contents.length)
            .map { (contents.item(it) as? Element)?.getAttribute("src").orEmpty().trim() }
            .firstOrNull { it.isNotEmpty() }
        if (title != null && src != null && out.size < MAX_TOC_ENTRIES) {
            out += EpubChapter(href = resolve(base, src), title = title)
        }
        val children = el.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child is Element && child.tagName.endsWith("navPoint")) {
                collectNcxPoint(child, base, out)
            }
        }
    }

    // MARK: XML helpers

    private fun parseXml(bytes: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
    }

    private fun Document.firstText(local: String): String {
        val nodes = getElementsByTagNameNS("*", local)
        for (i in 0 until nodes.length) {
            val text = nodes.item(i).textContent.trim().replace(Regex("\\s+"), " ")
            if (text.isNotEmpty()) return text
        }
        return ""
    }

    private fun resolve(base: String, href: String): String {
        val clean = href.substringBefore('#').trim()
        if (clean.isEmpty()) return ""
        val rel = clean.removePrefix("/")
        if (base.isEmpty()) return rel
        val parts = ArrayList<String>()
        for (seg in (base.split('/') + rel.split('/'))) {
            when (seg) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
                else -> parts += seg
            }
        }
        return parts.joinToString("/")
    }

    private companion object {
        const val CONTAINER_PATH = "META-INF/container.xml"

        /** Per-entry materialization cap (nav docs and covers only). */
        const val MAX_SINGLE_ENTRY_BYTES = 8 * 1024 * 1024L

        /** Total materialized per pass. */
        const val MAX_TOTAL_BYTES = 16 * 1024 * 1024L
        const val MAX_TOC_ENTRIES = 2000
    }
}
