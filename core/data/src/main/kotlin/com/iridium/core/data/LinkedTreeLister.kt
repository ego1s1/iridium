package com.iridium.core.data

import android.net.Uri
import android.os.Environment
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A book file addressable for indexing: identity, display name, and a
 * change marker for the fast path.
 */
internal data class LinkedDocument(
    val uri: Uri,
    val name: String,
    val modified: Long,
)

/**
 * Lists book files for indexing. Separated from the repository for
 * testability: filesystem walks are faked here instead of touching disk.
 */
internal interface LinkedTreeLister {
    suspend fun listBooks(): LinkedTreeListResult

    /** Resolves one document, or null when it is gone/unreadable. */
    suspend fun resolve(documentUri: Uri): LinkedDocument?
}

internal data class LinkedTreeListResult(
    val documents: List<LinkedDocument>,
    /**
     * True when any listing failed: the result must never read as an empty
     * device, or pruning would wipe rows the user still owns.
     */
    val walkFailed: Boolean,
)

/**
 * Walks shared storage for EPUBs (all-files access). Skips the private
 * `Android/` tree, hidden directories, and unreadable subtrees; any skipped
 * subtree sets [LinkedTreeListResult.walkFailed] so the repository never
 * prunes on a partial walk.
 */
@Singleton
internal class FilesystemLinkedTreeLister @Inject constructor() : LinkedTreeLister {

    override suspend fun listBooks(): LinkedTreeListResult {
        val root = Environment.getExternalStorageDirectory() ?: return LinkedTreeListResult(
            emptyList(),
            walkFailed = true,
        )
        return listBooks(root)
    }

    internal fun listBooks(root: File): LinkedTreeListResult {
        val out = mutableListOf<LinkedDocument>()
        var walkFailed = false
        try {
            root.walkTopDown()
                .onEnter { dir ->
                    val name = dir.name
                    // Private app data, thumbnails caches and dot-dirs never
                    // hold books; skipping them also bounds walk time.
                    if (dir != root && (name == "Android" || name.startsWith("."))) {
                        false
                    } else if (!dir.canRead()) {
                        walkFailed = true
                        false
                    } else {
                        true
                    }
                }
                .onFail { _, _ -> walkFailed = true }
                .filter { it.isFile && isSupportedBook(it.name) }
                .forEach { file ->
                    out += LinkedDocument(
                        Uri.fromFile(file),
                        file.name,
                        file.lastModified(),
                    )
                }
        } catch (_: SecurityException) {
            return LinkedTreeListResult(emptyList(), walkFailed = true)
        }
        return LinkedTreeListResult(out, walkFailed)
    }

    override suspend fun resolve(documentUri: Uri): LinkedDocument? {
        val path = documentUri.path ?: return null
        val file = File(path)
        if (!file.isFile || !file.canRead()) return null
        return LinkedDocument(documentUri, file.name, file.lastModified())
    }
}
