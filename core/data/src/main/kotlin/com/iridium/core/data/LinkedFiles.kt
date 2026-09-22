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
