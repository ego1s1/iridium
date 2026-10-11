package com.iridium.core.data

import java.security.MessageDigest

/**
 * Linked rows address user documents by URI; legacy rows by absolute path.
 * The user's originals are never copied into the app.
 */
internal fun isLinkedSourcePath(sourcePath: String): Boolean =
    sourcePath.startsWith("content://")

/** Stable hex digest for cache keys and cover ids (never a security boundary). */
internal fun sha256Hex(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

/** Stable cover file key for a linked document URI. */
internal fun linkedCoverId(documentUri: String): String =
    "linked-${sha256Hex(documentUri).take(24)}"

/** EPUB is the only linked format for now. */
internal fun isSupportedBook(name: String): Boolean =
    name.substringAfterLast('.', "").lowercase() == "epub"

/** Display name for an imported file: safe, bounded, always *.epub. */
internal fun sanitizeImportDisplayName(raw: String?): String {
    val cleaned = raw.orEmpty().trim()
        .replace(Regex("[/\\\\]+"), "-")
        .replace(Regex("\\p{Cntrl}"), "")
        .trim('.', ' ', '-')
        .take(MAX_IMPORT_NAME_CHARS)
        .trim('.', ' ', '-')
    val base = cleaned.ifEmpty { "book" }
    return if (base.substringAfterLast('.', "").lowercase() == "epub") base else "$base.epub"
}

/** Upper bound on one imported EPUB (512 MiB); larger shares are rejected. */
internal const val MAX_IMPORT_BYTES = 512L * 1024L * 1024L

/** Display-name cap so one hostile filename cannot bloat the row. */
internal const val MAX_IMPORT_NAME_CHARS = 120

/** Directory under filesDir holding imported copies. */
internal const val IMPORT_DIR = "books"

/**
 * Stream copy that aborts past [maxBytes] instead of filling storage.
 * Throws [java.io.IOException] when the cap is exceeded.
 */
internal fun copyCapped(
    input: java.io.InputStream,
    output: java.io.OutputStream,
    maxBytes: Long,
) {
    val buffer = ByteArray(DEFAULT_COPY_BUFFER)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) return
        total += read
        if (total > maxBytes) throw java.io.IOException("File too large to import")
        output.write(buffer, 0, read)
    }
}

private const val DEFAULT_COPY_BUFFER = 8192
