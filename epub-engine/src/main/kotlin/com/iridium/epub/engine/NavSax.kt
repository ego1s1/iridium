package com.iridium.epub.engine

import java.io.ByteArrayInputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

/**
 * Streaming readers for the two EPUB navigation formats, returning
 * `(href, title)` pairs in document order for the engine to resolve.
 */
internal object NavSax {

    private const val OPS_NS = "http://www.idpf.org/2007/ops"

    /** EPUB 3 navigation document: the `<nav epub:type="toc">` list. */
    fun parseNav(xhtml: ByteArray): List<Pair<String, String>> {
        val handler = NavHandler()
        return runCatching {
            newParser().parse(InputSource(ByteArrayInputStream(xhtml)), handler)
            handler.chapters
        }.getOrDefault(emptyList())
    }

    /** EPUB 2 NCX: `navPoint` labels paired with their `content` targets. */
    fun parseNcx(ncx: ByteArray): List<Pair<String, String>> {
        val handler = NcxHandler()
        return runCatching {
            newParser().parse(InputSource(ByteArrayInputStream(ncx)), handler)
            handler.chapters
        }.getOrDefault(emptyList())
    }

    private fun newParser() = SAXParserFactory.newInstance().apply {
        isNamespaceAware = true
        runCatching {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        runCatching {
            setFeature("http://xml.org/sax/features/external-general-entities", false)
        }
        runCatching {
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        }
    }.newSAXParser()

    private class NavHandler : DefaultHandler() {
        val chapters = ArrayList<Pair<String, String>>()
        private var depth = 0
        private var tocDepth = -1
        private var inAnchor = false
        private var anchorHref: String? = null
        private var anchorText: StringBuilder? = null

        override fun startElement(uri: String?, localName: String, qName: String?, attrs: Attributes) {
            depth++
            when (localName) {
                "nav" -> {
                    if (tocDepth < 0) {
                        val type = attrs.getValue(OPS_NS, "type") ?: attrs.value("type")
                        if (type?.split(' ')?.contains("toc") == true) tocDepth = depth
                    }
                }
                "a" -> if (tocDepth >= 0 && chapters.size < MAX_ENTRIES) {
                    inAnchor = true
                    anchorHref = attrs.value("href")
                    anchorText = StringBuilder()
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            anchorText?.appendRange(ch, start, start + length)
        }

        override fun endElement(uri: String?, localName: String, qName: String?) {
            when (localName) {
                "a" -> if (inAnchor) {
                    val href = anchorHref
                    val title = anchorText?.toString()?.trim()?.replace(WHITESPACE, " ")
                    if (href != null && !title.isNullOrEmpty()) chapters += href to title
                    inAnchor = false
                    anchorHref = null
                    anchorText = null
                }
                "nav" -> if (tocDepth >= 0 && depth == tocDepth) tocDepth = -1
            }
            depth--
        }
    }

    private class NcxHandler : DefaultHandler() {
        val chapters = ArrayList<Pair<String, String>>()
        private var label: String? = null
        private var capturing = false
        private var buffer: StringBuilder? = null

        override fun startElement(uri: String?, localName: String, qName: String?, attrs: Attributes) {
            when (localName) {
                "text" -> if (!capturing) {
                    capturing = true
                    buffer = StringBuilder()
                }
                // In NCX the label precedes content within a navPoint, so the
                // content element is the first point both are known — emitting
                // here preserves document order for nested points.
                "content" -> {
                    val src = attrs.value("src")
                    if (src != null && chapters.size < MAX_ENTRIES) {
                        chapters += src to (label ?: "")
                    }
                    label = null
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            buffer?.appendRange(ch, start, start + length)
        }

        override fun endElement(uri: String?, localName: String, qName: String?) {
            if (localName == "text" && capturing) {
                label = buffer?.toString()?.trim()?.replace(WHITESPACE, " ")
                capturing = false
                buffer = null
            }
        }
    }

    private fun Attributes.value(name: String): String? = getValue(name)?.takeIf { it.isNotEmpty() }

    private val WHITESPACE = Regex("\\s+")
    private const val MAX_ENTRIES = 2000
}
