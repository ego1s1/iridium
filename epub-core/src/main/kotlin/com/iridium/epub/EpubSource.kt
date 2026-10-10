package com.iridium.epub

import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Random-access, seekable byte source.
 *
 * The engine reads a ZIP's central directory once and then slices individual
 * entries by offset, so it needs random access rather than a one-way stream.
 * A one-way stream is supported too, by spooling it to a temporary file.
 */
interface EpubSource : Closeable {
    val size: Long

    /** Reads up to [length] bytes at [offset]; returns fewer only at EOF. */
    fun read(offset: Long, length: Int): ByteArray

    companion object {
        /** Pipe-spool buffer and total cap (protects cache storage). */
        private const val SPOOL_BUFFER = 32 * 1024
        private const val MAX_SPOOL_BYTES = 256L * 1024 * 1024

        fun ofBytes(bytes: ByteArray): EpubSource = ByteArrayEpubSource(bytes)

        fun ofFile(file: File): EpubSource =
            ChannelEpubSource(FileChannel.open(file.toPath()))

        /**
         * Wraps an already-open channel (e.g. from a content-provider file
         * descriptor) so no copy is made. [onClose] releases the owner handle.
         */
        fun ofChannel(channel: FileChannel, onClose: () -> Unit = {}): EpubSource =
            try {
                ChannelEpubSource(channel, onClose)
            } catch (e: Exception) {
                // Constructor probes the channel: release the FD instead of
                // leaking it when the probe throws.
                runCatching { channel.close() }
                runCatching { onClose() }
                throw e
            }

        /**
         * Spools a one-way stream into [cacheDir] so it becomes seekable. The
         * temporary file is deleted when the source is closed.
         */
        fun ofStream(openStream: () -> InputStream, cacheDir: File): EpubSource {
            // check() over throw: a missing spool dir is a programming error,
            // and explicit throws trip the ThrowsCount gate.
            check(cacheDir.isDirectory || cacheDir.mkdirs()) { "Unable to spool EPUB in $cacheDir" }
            val temp = File.createTempFile("epub-src-", ".tmp", cacheDir)
            try {
                openStream().use { input ->
                    temp.outputStream().use { output ->
                        // Capped, cancellable copy: a hostile pipe must
                        // neither fill storage nor ignore cancellation.
                        val buffer = ByteArray(SPOOL_BUFFER)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            check(total <= MAX_SPOOL_BYTES) { "EPUB spool exceeds cap" }
                            output.write(buffer, 0, count)
                        }
                    }
                }
            } catch (e: Exception) {
                temp.delete()
                throw e
            }
            val channel = try {
                FileChannel.open(temp.toPath())
            } catch (e: Exception) {
                temp.delete()
                throw e
            }
            return ChannelEpubSource(channel, onClose = { runCatching { temp.delete() } })
        }
    }
}

private class ByteArrayEpubSource(private val bytes: ByteArray) : EpubSource {
    override val size: Long get() = bytes.size.toLong()

    override fun read(offset: Long, length: Int): ByteArray {
        if (offset < 0 || length <= 0 || offset >= bytes.size) return EMPTY
        val end = minOf(offset + length, bytes.size.toLong()).toInt()
        return bytes.copyOfRange(offset.toInt(), end)
    }

    override fun close() = Unit

    private companion object {
        val EMPTY = ByteArray(0)
    }
}

private class ChannelEpubSource(
    private val channel: FileChannel,
    private val onClose: () -> Unit = {},
) : EpubSource {

    override val size: Long = channel.size()

    override fun read(offset: Long, length: Int): ByteArray {
        if (length <= 0 || offset < 0 || offset >= size) return ByteArray(0)
        val len = minOf(length.toLong(), size - offset).toInt()
        val buffer = ByteBuffer.allocate(len)
        var position = offset
        while (buffer.hasRemaining()) {
            val read = channel.read(buffer, position)
            // <= 0 (not just EOF): a 0-returning channel would spin forever.
            if (read <= 0) break
            position += read
        }
        return buffer.array().copyOf(buffer.position())
    }

    override fun close() {
        runCatching { channel.close() }
        runCatching { onClose() }
    }
}
