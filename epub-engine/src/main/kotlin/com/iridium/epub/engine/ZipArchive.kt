package com.iridium.epub.engine

import com.iridium.epub.EpubSource
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

/**
 * A ZIP entry located from the central directory: name, compression, sizes,
 * and the offset of its local header. No data is read until requested.
 */
internal data class ZipEntry(
    val name: String,
    val method: Int,
    val compressedSize: Long,
    val uncompressedSize: Long,
    val localHeaderOffset: Long,
    val encrypted: Boolean,
)

/**
 * ZIP reader built on the central directory.
 *
 * The directory is read exactly once from the archive tail, giving a complete
 * name → entry table in a single pass with no per-entry scanning. Individual
 * entries are then sliced by offset and inflated on demand, so inspecting a
 * book touches a handful of small ranges instead of streaming the whole file.
 *
 * Bounded throughout: oversized entries and a runaway directory are refused
 * rather than buffered, and every malformed structure yields `null` instead of
 * throwing.
 */
internal class ZipArchive private constructor(
    private val source: EpubSource,
    private val entries: Map<String, ZipEntry>,
) {

    val entryNames: Set<String> get() = entries.keys

    operator fun get(name: String): ZipEntry? = entries[name]

    /** Reads and inflates [entry], or returns null when unreadable/oversized. */
    fun read(entry: ZipEntry, maxBytes: Long = MAX_ENTRY_BYTES): ByteArray? {
        if (entry.encrypted) return null
        if (entry.uncompressedSize < 0 || entry.uncompressedSize > maxBytes) return null
        if (entry.compressedSize < 0 || entry.compressedSize > maxBytes) return null

        val header = source.read(entry.localHeaderOffset, LOCAL_HEADER_SIZE)
        if (header.size < LOCAL_HEADER_SIZE || u32(header, 0) != LOCAL_SIG) return null
        val nameLength = u16(header, 26)
        val extraLength = u16(header, 28)
        val dataOffset = entry.localHeaderOffset + LOCAL_HEADER_SIZE + nameLength + extraLength
        if (dataOffset < 0 || dataOffset >= source.size) return null

        val compressed = source.read(dataOffset, entry.compressedSize.toInt())
        if (compressed.size.toLong() < entry.compressedSize) return null

        return when (entry.method) {
            METHOD_STORED -> compressed
            METHOD_DEFLATED -> inflate(compressed, entry.uncompressedSize, maxBytes)
            else -> null
        }
    }

    fun read(name: String, maxBytes: Long = MAX_ENTRY_BYTES): ByteArray? =
        entries[name]?.let { read(it, maxBytes) }

    /** True when [href] (possibly percent-encoded) matches a stored entry. */
    fun findEntry(href: String): ZipEntry? {
        entries[href]?.let { return it }
        val decoded = decodePercent(href)
        return entries[decoded]
    }

    private fun inflate(data: ByteArray, expected: Long, maxBytes: Long): ByteArray? {
        // ZIP deflate streams are raw: no zlib header, so nowrap must be true.
        val inflater = Inflater(true)
        return try {
            inflater.setInput(data)
            val initial = expected.coerceIn(64L, maxBytes).toInt()
            val out = ByteArrayOutputStream(initial)
            val buffer = ByteArray(INFLATE_BUFFER)
            var total = 0L
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                total += count
                if (total > maxBytes) return null
                out.write(buffer, 0, count)
            }
            out.toByteArray()
        } catch (_: Exception) {
            null
        } finally {
            inflater.end()
        }
    }

    internal companion object {
        const val METHOD_STORED = 0
        const val METHOD_DEFLATED = 8

        private const val EOCD_SIG = 0x06054b50L
        private const val ZIP64_EOCD_SIG = 0x06064b50L
        private const val ZIP64_LOCATOR_SIG = 0x07064b50L
        private const val CD_SIG = 0x02014b50L
        private const val LOCAL_SIG = 0x04034b50L

        private const val EOCD_SIZE = 22
        private const val CD_HEADER_SIZE = 46
        private const val LOCAL_HEADER_SIZE = 30
        private const val ZIP64_LOCATOR_SIZE = 20
        private const val MAX_COMMENT = 0xFFFF
        private const val INFLATE_BUFFER = 16 * 1024

        /** Caps: refuse rather than buffer. */
        const val MAX_ENTRY_BYTES = 64L * 1024 * 1024
        private const val MAX_CD_BYTES = 32L * 1024 * 1024

        fun parse(source: EpubSource): ZipArchive? {
            val eocdOffset = findEocd(source) ?: return null
            val eocd = source.read(eocdOffset, EOCD_SIZE)
            if (eocd.size < EOCD_SIZE) return null

            var cdOffset = u32(eocd, 16)
            var entryCount = u16(eocd, 10)

            // ZIP64: sentinel values redirect us to the ZIP64 EOCD record.
            if (cdOffset == 0xFFFFFFFFL || entryCount == 0xFFFF) {
                val zip64 = readZip64(source, eocdOffset)
                if (zip64 != null) {
                    cdOffset = zip64.first
                    entryCount = zip64.second
                }
            }

            if (cdOffset <= 0 || cdOffset >= source.size) return null
            val cdLength = minOf(source.size - cdOffset, MAX_CD_BYTES).toInt()
            val cd = source.read(cdOffset, cdLength)
            if (cd.isEmpty()) return null

            val map = LinkedHashMap<String, ZipEntry>(minOf(entryCount, 1024))
            var p = 0
            var parsed = 0
            while (p + CD_HEADER_SIZE <= cd.size && parsed < entryCount) {
                if (u32(cd, p) != CD_SIG) break
                val method = u16(cd, p + 10)
                val flags = u16(cd, p + 8)
                var compressedSize = u32(cd, p + 20)
                var uncompressedSize = u32(cd, p + 24)
                val nameLength = u16(cd, p + 28)
                val extraLength = u16(cd, p + 30)
                val commentLength = u16(cd, p + 32)
                var localOffset = u32(cd, p + 42)

                val nameStart = p + CD_HEADER_SIZE
                val nameEnd = nameStart + nameLength
                if (nameEnd > cd.size) break
                val name = String(cd, nameStart, nameLength, Charsets.UTF_8)

                // ZIP64 extended information lives in the extra field.
                if (compressedSize == 0xFFFFFFFFL || uncompressedSize == 0xFFFFFFFFL ||
                    localOffset == 0xFFFFFFFFL
                ) {
                    val extraStart = nameEnd
                    val extraEnd = minOf(extraStart + extraLength, cd.size)
                    val zip64 = parseZip64Extra(cd, extraStart, extraEnd)
                    if (zip64 != null) {
                        uncompressedSize = zip64.getOrElse(0) { uncompressedSize }
                        compressedSize = zip64.getOrElse(1) { compressedSize }
                        localOffset = zip64.getOrElse(2) { localOffset }
                    }
                }

                if (!name.endsWith("/")) {
                    map[name] = ZipEntry(
                        name = name,
                        method = method,
                        compressedSize = compressedSize,
                        uncompressedSize = uncompressedSize,
                        localHeaderOffset = localOffset,
                        encrypted = flags and FLAG_ENCRYPTED != 0,
                    )
                }

                p = nameEnd + extraLength + commentLength
                parsed++
            }

            return if (map.isEmpty()) null else ZipArchive(source, map)
        }

        private fun findEocd(source: EpubSource): Long? {
            val tailLength = minOf(source.size, (MAX_COMMENT + EOCD_SIZE).toLong()).toInt()
            if (tailLength < EOCD_SIZE) return null
            val tail = source.read(source.size - tailLength, tailLength)
            for (i in tail.size - EOCD_SIZE downTo 0) {
                if (u32(tail, i) == EOCD_SIG) {
                    val commentLength = u16(tail, i + 20)
                    // A valid EOCD's comment runs to the exact end of the file.
                    if (i + EOCD_SIZE + commentLength == tail.size) {
                        return source.size - tailLength + i
                    }
                }
            }
            return null
        }

        private fun readZip64(source: EpubSource, eocdOffset: Long): Pair<Long, Int>? {
            val locatorOffset = eocdOffset - ZIP64_LOCATOR_SIZE
            if (locatorOffset < 0) return null
            val locator = source.read(locatorOffset, ZIP64_LOCATOR_SIZE)
            if (locator.size < ZIP64_LOCATOR_SIZE || u32(locator, 0) != ZIP64_LOCATOR_SIG) return null
            val zip64Offset = u64(locator, 8)
            if (zip64Offset < 0 || zip64Offset >= source.size) return null
            val record = source.read(zip64Offset, 56)
            if (record.size < 56 || u32(record, 0) != ZIP64_EOCD_SIG) return null
            val entryCount = u64(record, 32)
            val cdOffset = u64(record, 48)
            return cdOffset to entryCount.toInt()
        }

        /** Parses the ZIP64 extra field (id 0x0001): [uncompressed, compressed, offset]. */
        private fun parseZip64Extra(bytes: ByteArray, start: Int, end: Int): List<Long>? {
            var p = start
            while (p + 4 <= end) {
                val id = u16(bytes, p)
                val size = u16(bytes, p + 2)
                val body = p + 4
                if (body + size > end) return null
                if (id == 0x0001) {
                    val values = ArrayList<Long>(3)
                    var q = body
                    while (q + 8 <= body + size && values.size < 3) {
                        values += u64(bytes, q)
                        q += 8
                    }
                    return values
                }
                p = body + size
            }
            return null
        }

        private fun decodePercent(value: String): String {
            if (!value.contains('%')) return value
            return runCatching { java.net.URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
        }

        private const val FLAG_ENCRYPTED = 0x0001
    }
}

internal fun u16(b: ByteArray, i: Int): Int =
    (b[i].toInt() and 0xFF) or ((b[i + 1].toInt() and 0xFF) shl 8)

internal fun u32(b: ByteArray, i: Int): Long =
    (b[i].toLong() and 0xFF) or
        ((b[i + 1].toLong() and 0xFF) shl 8) or
        ((b[i + 2].toLong() and 0xFF) shl 16) or
        ((b[i + 3].toLong() and 0xFF) shl 24)

internal fun u64(b: ByteArray, i: Int): Long {
    var value = 0L
    for (k in 7 downTo 0) {
        value = (value shl 8) or (b[i + k].toLong() and 0xFF)
    }
    return value
}
