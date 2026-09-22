package com.iridium.epub.engine

import java.io.ByteArrayInputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

internal data class OpfManifestItem(
    val id: String,
    val href: String,
    val mediaType: String,
    val properties: Set<String>,
)

internal data class OpfData(
    val title: String?,
    val author: String?,
    val manifest: Map<String, OpfManifestItem>,
    val spine: List<String>,
    val coverId: String?,
    val guideCoverHref: String?,
)

/**
 * Streaming OPF (package document) reader.
 *
 * SAX rather than DOM: the OPF is small but on a cold folder scan of hundreds
 * of books the allocation and tree-building cost dominates, so we never build
 * a document object. XXE is disabled and the handler ignores anything it does
 * not need.
 */
internal object OpfSax {

    fun parse(opf: ByteArray): OpfData? {
        val handler = Handler()
        return try {
            newParser().parse(InputSource(ByteArrayInputStream(opf)), handler)
            handler.result()
        } catch (_: Exception) {
            null
        }
    }

    /** Reads the OPF path from `META-INF/container.xml`. */
    fun parseRootfile(containerXml: ByteArray): String? {
        val handler = RootfileHandler()
        return try {
            newParser().parse(InputSource(ByteArrayInputStream(containerXml)), handler)
            handler.path
        } catch (_: Exception) {
            null
        }
    }

    private class RootfileHandler : DefaultHandler() {
        var path: String? = null

        override fun startElement(uri: String?, localName: String, qName: String?, attrs: Attributes) {
            if (path == null && localName == "rootfile") {
                path = attrs.getValue("full-path")?.takeIf { it.isNotEmpty() }
            }
        }
    }

    private fun newParser() = SAXParserFactory.newInstance().apply {
        isNamespaceAware = true
        isFeatureQuietly("http://apache.org/xml/features/disallow-doctype-decl", true)
        isFeatureQuietly("http://xml.org/sax/features/external-general-entities", false)
        isFeatureQuietly("http://xml.org/sax/features/external-parameter-entities", false)
        isFeatureQuietly("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    }.newSAXParser()

    private fun SAXParserFactory.isFeatureQuietly(name: String, value: Boolean) {
        runCatching { setFeature(name, value) }
    }

    private class Handler : DefaultHandler() {
        private val manifest = LinkedHashMap<String, OpfManifestItem>()
        private val spine = ArrayList<String>()
        private var title: String? = null
        private var author: String? = null
        private var coverId: String? = null
        private var guideCoverHref: String? = null

        private var text: StringBuilder? = null
        private var textTarget: String? = null

        fun result(): OpfData = OpfData(
            title = title?.takeIf { it.isNotBlank() },
            author = author?.takeIf { it.isNotBlank() },
            manifest = manifest,
            spine = spine,
            coverId = coverId,
            guideCoverHref = guideCoverHref,
        )

        override fun startElement(uri: String?, localName: String, qName: String?, attrs: Attributes) {
            when (localName) {
                "title", "creator" -> {
                    textTarget = localName
                    text = StringBuilder()
                }
                "item" -> {
                    val id = attrs.value("id")
                    val href = attrs.value("href")
                    if (id != null && href != null) {
                        manifest[id] = OpfManifestItem(
                            id = id,
                            href = href,
                            mediaType = attrs.value("media-type").orEmpty(),
                            properties = attrs.value("properties")
                                .orEmpty()
                                .split(' ')
                                .filter { it.isNotEmpty() }
                                .toSet(),
                        )
                    }
                }
                "itemref" -> attrs.value("idref")?.let { spine += it }
                "meta" -> {
                    if (attrs.value("name").equals("cover", ignoreCase = true)) {
                        coverId = attrs.value("content")
                    }
                }
                "reference" -> {
                    if (attrs.value("type").equals("cover", ignoreCase = true)) {
                        guideCoverHref = attrs.value("href")
                    }
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            text?.appendRange(ch, start, start + length)
        }

        override fun endElement(uri: String?, localName: String, qName: String?) {
            if (localName == textTarget) {
                val value = text?.toString()?.trim()?.replace(WHITESPACE, " ")
                when (localName) {
                    "title" -> if (title == null) title = value
                    "creator" -> if (author == null) author = value
                }
                text = null
                textTarget = null
            }
        }

        private fun Attributes.value(name: String): String? =
            getValue(name)?.takeIf { it.isNotEmpty() }

        private val WHITESPACE = Regex("\\s+")
    }
}
