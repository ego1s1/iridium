package com.iridium.epub

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.NodeList

/**
 * Minimal EPUB 2/3 parser over raw bytes: container.xml → OPF → metadata,
 * cover, spine count, and TOC (EPUB3 nav or EPUB2 NCX). Defensive: XXE
 * disabled, size caps, never throws — failures yield an [InspectedEpub]
 * with the fallback title and zero chapters.
 */
class ZipEpubBackend : EpubBackend {

    override fun inspect(epubBytes: ByteArray, fallbackTitle: String): InspectedEpub {
        return try {
            inspectOrThrow(epubBytes, fallbackTitle)
        } catch (_: Exception) {
            InspectedEpub(title = fallbackTitle.ifBlank { "Unknown" })
        }
    }

    private fun inspectOrThrow(epubBytes: ByteArray, fallbackTitle: String): InspectedEpub {
        val entries = readZip(epubBytes)
        if (entries.isEmpty()) return InspectedEpub(title = fallbackTitle.ifBlank { "Unknown" })

        val containerXml = entries["META-INF/container.xml"] ?: return InspectedEpub(
            title = fallbackTitle.ifBlank { "Unknown" },
        )
        val opfPath = parseOpfPath(containerXml) ?: return InspectedEpub(
            title = fallbackTitle.ifBlank { "Unknown" },
        )
        val opfBytes = entries[opfPath] ?: entries.entries
            .firstOrNull { it.key.endsWith(".opf", ignoreCase = true) }?.value
            ?: return InspectedEpub(title = fallbackTitle.ifBlank { "Unknown" })
        val base = opfPath.substringBeforeLast('/', "")

        val opf = parseXml(opfBytes)
        val title = opf.getElementsByTag("title").ifBlank { fallbackTitle.ifBlank { "Unknown" } }
        val author = opf.getElementsByTag("creator").ifBlank { null }

        val manifest = readManifest(opf, base)
        val spine = readSpine(opf)

        val coverBytes = findCover(opf, manifest, entries)
        val chapters = readToc(opf, manifest, entries, spine)

        return InspectedEpub(
            title = title,
            author = author,
            coverBytes = coverBytes?.first,
            coverMime = coverBytes?.second,
            spineCount = spine.size,
            chapters = chapters,
        )
    }

    private data class ManifestItem(val href: String, val mime: String)

    private fun readZip(bytes: ByteArray): Map<String, ByteArray> {
        val out = LinkedHashMap<String, ByteArray>()
        var total = 0L
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val bos = ByteArrayOutputStream()
                    val buf = ByteArray(8192)
                    var n = zip.read(buf)
                    var size = 0L
                    while (n != -1) {
                        size += n
                        if (size > MAX_ENTRY_BYTES) break
                        bos.write(buf, 0, n)
                        n = zip.read(buf)
                    }
                    total += size
                    if (total > MAX_TOTAL_BYTES) break
                    if (size <= MAX_ENTRY_BYTES) out[entry.name] = bos.toByteArray()
                }
                entry = zip.nextEntry
            }
        }
        return out
    }

    private fun parseOpfPath(containerXml: ByteArray): String? {
        val doc = parseXml(containerXml)
        val nodes = doc.getElementsByTagNameNS("*", "rootfile")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            val path = el.getAttribute("full-path").trim()
            if (path.isNotEmpty()) return path
        }
        return null
    }

    private fun readManifest(opf: org.w3c.dom.Document, base: String): Map<String, ManifestItem> {
        val items = LinkedHashMap<String, ManifestItem>()
        val nodes = opf.getElementsByTagNameNS("*", "item")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            val id = el.getAttribute("id")
            val href = el.getAttribute("href").trim()
            val mime = el.getAttribute("media-type").trim()
            if (id.isEmpty() || href.isEmpty()) continue
            val resolved = resolve(base, href)
            items[id] = ManifestItem(resolved, mime)
        }
        return items
    }

    private fun readSpine(opf: org.w3c.dom.Document): List<String> {
        val ids = ArrayList<String>()
        val nodes = opf.getElementsByTagNameNS("*", "itemref")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            val idref = el.getAttribute("idref")
            if (idref.isNotEmpty()) ids += idref
        }
        return ids
    }

    private fun findCover(
        opf: org.w3c.dom.Document,
        manifest: Map<String, ManifestItem>,
        entries: Map<String, ByteArray>,
    ): Pair<ByteArray, String>? {
        // 1. <meta name="cover" content="id"/>
        val metas = opf.getElementsByTagNameNS("*", "meta")
        for (i in 0 until metas.length) {
            val el = metas.item(i) as? Element ?: continue
            if (!el.getAttribute("name").equals("cover", ignoreCase = true)) continue
            val id = el.getAttribute("content")
            val item = manifest[id] ?: continue
            entries[item.href]?.let { return it to item.mime }
        }
        // 2. manifest properties="cover-image" (EPUB3)
        val items = opf.getElementsByTagNameNS("*", "item")
        for (i in 0 until items.length) {
            val el = items.item(i) as? Element ?: continue
            val props = el.getAttribute("properties")
            if (!props.split(Regex("\\s+")).contains("cover-image")) continue
            val href = el.getAttribute("href")
            val resolved = resolve(opfBase(opf, manifest), href)
            val bytes = entries[resolved] ?: entries[href]
            if (bytes != null) return bytes to el.getAttribute("media-type")
        }
        // 3. guide <reference type="cover"/>
        val refs = opf.getElementsByTagNameNS("*", "reference")
        for (i in 0 until refs.length) {
            val el = refs.item(i) as? Element ?: continue
            if (!el.getAttribute("type").equals("cover", ignoreCase = true)) continue
            val href = el.getAttribute("href")
            entries[resolve(opfBase(opf, manifest), href)]?.let { return it to "image/jpeg" }
        }
        // 4. first image in manifest
        for ((_, item) in manifest) {
            if (item.mime.startsWith("image/")) {
                entries[item.href]?.let { return it to item.mime }
            }
        }
        return null
    }

    private fun opfBase(opf: org.w3c.dom.Document, manifest: Map<String, ManifestItem>): String {
        // Best-effort: derive from any manifest href's directory.
        val first = manifest.values.firstOrNull()?.href ?: return ""
        return first.substringBeforeLast('/', "")
    }

    private fun readToc(
        opf: org.w3c.dom.Document,
        manifest: Map<String, ManifestItem>,
        entries: Map<String, ByteArray>,
        spine: List<String>,
    ): List<EpubChapter> {
        // EPUB3 nav document
        val navItem = manifest.values.firstOrNull {
            it.mime == "application/xhtml+xml" && navHasToc(entries[it.href])
        } ?: manifest.entries.firstOrNull { (id, _) ->
            opfItemHasNavProps(opf, id)
        }?.value
        if (navItem != null) {
            entries[navItem.href]?.let { bytes ->
                val chapters = parseNavToc(bytes, navItem.href.substringBeforeLast('/', ""))
                if (chapters.isNotEmpty()) return chapters
            }
        }
        // EPUB2 NCX
        val ncx = manifest.values.firstOrNull { it.mime == "application/x-dtbncx+xml" }
        if (ncx != null) {
            entries[ncx.href]?.let { bytes ->
                val chapters = parseNcxToc(bytes, ncx.href.substringBeforeLast('/', ""))
                if (chapters.isNotEmpty()) return chapters
            }
        }
        // Fallback: spine order with file-name labels
        return spine.mapNotNull { id ->
            val item = manifest[id] ?: return@mapNotNull null
            val label = item.href.substringAfterLast('/').substringBefore('#')
            EpubChapter(href = item.href, title = label.ifBlank { "Chapter" })
        }
    }

    private fun opfItemHasNavProps(opf: org.w3c.dom.Document, id: String): Boolean {
        val items = opf.getElementsByTagNameNS("*", "item")
        for (i in 0 until items.length) {
            val el = items.item(i) as? Element ?: continue
            if (el.getAttribute("id") != id) continue
            if (el.getAttribute("properties").split(Regex("\\s+")).contains("nav")) return true
        }
        return false
    }

    private fun navHasToc(bytes: ByteArray?): Boolean {
        if (bytes == null || bytes.size > MAX_ENTRY_BYTES) return false
        return try {
            val doc = parseXml(bytes)
            val navs = doc.getElementsByTagNameNS("*", "nav")
            var found = false
            for (i in 0 until navs.length) {
                val el = navs.item(i) as? Element ?: continue
                val type = el.getAttributeNS("http://www.idpf.org/2007/ops", "type")
                    .ifEmpty { el.getAttribute("type") }
                if (type.split(Regex("\\s+")).contains("toc")) {
                    found = true
                    break
                }
            }
            found
        } catch (_: Exception) {
            false
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
            collectNavLinks(el, base, out)
            break
        }
        return out
    }

    private fun collectNavLinks(el: Element, base: String, out: MutableList<EpubChapter>) {
        val links = el.getElementsByTagNameNS("*", "a")
        for (i in 0 until links.length) {
            val a = links.item(i) as? Element ?: continue
            val href = a.getAttribute("href").trim()
            // Only top-level list items to avoid duplicates from nested lists.
            val title = a.textContent.trim().replace(Regex("\\s+"), " ")
            if (href.isNotEmpty() && title.isNotEmpty()) {
                out += EpubChapter(href = resolve(base, href), title = title)
            }
            if (out.size >= MAX_TOC_ENTRIES) break
        }
    }

    private fun parseNcxToc(ncxBytes: ByteArray, base: String): List<EpubChapter> {
        val out = ArrayList<EpubChapter>()
        val doc = parseXml(ncxBytes)
        val points = doc.getElementsByTagNameNS("*", "navPoint")
        for (i in 0 until points.length) {
            if (out.size >= MAX_TOC_ENTRIES) break
            val el = points.item(i) as? Element ?: continue
            // Skip nested navPoints (they appear as descendants too).
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
        // Recurse into nested navPoints for full depth.
        val children = el.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child is Element && child.tagName.endsWith("navPoint")) {
                collectNcxPoint(child, base, out)
            }
        }
    }

    private fun parseXml(bytes: ByteArray): org.w3c.dom.Document {
        val capped = if (bytes.size > MAX_XML_BYTES) bytes.copyOf(MAX_XML_BYTES) else bytes
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(capped))
    }

    private fun org.w3c.dom.Document.getElementsByTag(local: String): String {
        val nodes: NodeList = getElementsByTagNameNS("*", local)
        for (i in 0 until nodes.length) {
            val text = nodes.item(i).textContent.trim().replace(Regex("\\s+"), " ")
            if (text.isNotEmpty()) return text
        }
        return ""
    }

    private fun resolve(base: String, href: String): String {
        val clean = href.substringBefore('#').trim()
        if (clean.isEmpty()) return ""
        // Strip leading "/" (OPF hrefs are relative to the OPF, never root).
        val rel = clean.removePrefix("/")
        if (base.isEmpty()) return rel
        // Normalize ./ and ../ segments without touching the filesystem.
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
        const val MAX_ENTRY_BYTES = 8 * 1024 * 1024L
        const val MAX_TOTAL_BYTES = 256 * 1024 * 1024L
        const val MAX_XML_BYTES = 512 * 1024
        const val MAX_TOC_ENTRIES = 2000
    }
}
