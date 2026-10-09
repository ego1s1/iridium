package com.iridium.core.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.iridium.core.datastore.IridiumPreferencesDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

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
 * testability: tree walks are faked here instead of touching disk.
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

/** One node in a document tree (folder or file). */
internal data class TreeDoc(
    val uri: Uri,
    val name: String,
    val isDirectory: Boolean,
    val modified: Long,
)

/**
 * Pure tree collector: EPUBs under [root], skipping hidden directories; any
 * unreadable subtree sets `walkFailed` instead of pruning the library.
 * Pure over [childrenOf] so unit tests fake the tree without a provider.
 */
internal fun collectLinkedBooks(
    root: TreeDoc,
    childrenOf: (TreeDoc) -> List<TreeDoc>?,
    books: MutableList<LinkedDocument> = mutableListOf(),
): LinkedTreeListResult {
    var walkFailed = false
    val stack = ArrayDeque<TreeDoc>()
    stack.add(root)
    while (stack.isNotEmpty()) {
        val dir = stack.removeLast()
        val children = try {
            childrenOf(dir)
        } catch (_: Exception) {
            null
        }
        if (children == null) {
            walkFailed = true
            continue
        }
        for (doc in children) {
            if (doc.name.startsWith(".")) continue
            if (doc.isDirectory) {
                stack.add(doc)
            } else if (isSupportedBook(doc.name)) {
                books += LinkedDocument(doc.uri, doc.name, doc.modified)
            }
        }
    }
    return LinkedTreeListResult(books, walkFailed)
}

/**
 * Walks the user's SAF-linked folders (Storage Access Framework tree URIs
 * with persisted read permission). No storage permission is needed or
 * requested: every folder was explicitly picked by the user. Folders whose
 * permission was revoked are skipped without failing the walk.
 */
@Singleton
internal class SafLinkedTreeLister @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: IridiumPreferencesDataSource,
) : LinkedTreeLister {

    override suspend fun listBooks(): LinkedTreeListResult = withContext(Dispatchers.IO) {
        val folders = preferences.linkedFolders.first()
        if (folders.isEmpty()) return@withContext LinkedTreeListResult(emptyList(), walkFailed = false)
        val persisted = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .mapTo(mutableSetOf()) { it.uri }
        val books = mutableListOf<LinkedDocument>()
        var walkFailed = false
        for (folder in folders) {
            val treeUri = runCatching { Uri.parse(folder) }.getOrNull() ?: continue
            // A revoked grant leaves the stored URI behind but yields no
            // files: it must fail the walk, not read as an empty device, or
            // pruning would wipe the books that lived under that folder.
            if (treeUri !in persisted) {
                walkFailed = true
                continue
            }
            val rootDoc = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull()
            if (rootDoc == null || !rootDoc.isDirectory) {
                walkFailed = true
                continue
            }
            val root = TreeDoc(
                uri = rootDoc.uri,
                name = rootDoc.name.orEmpty(),
                isDirectory = true,
                modified = rootDoc.lastModified(),
            )
            // listFiles() only works on the DocumentFile instance itself, so
            // the traversal keeps uri -> DocumentFile handles instead of
            // re-resolving (fromSingleUri cannot list children).
            val handles = mutableMapOf(rootDoc.uri.toString() to rootDoc)
            val result = collectLinkedBooks(
                root,
                childrenOf = { dir ->
                    val doc = handles[dir.uri.toString()] ?: return@collectLinkedBooks null
                    doc.listFiles().map { child ->
                        handles[child.uri.toString()] = child
                        TreeDoc(
                            uri = child.uri,
                            name = child.name.orEmpty(),
                            isDirectory = child.isDirectory,
                            modified = child.lastModified(),
                        )
                    }
                },
            )
            books += result.documents
            walkFailed = walkFailed || result.walkFailed
        }
        LinkedTreeListResult(books, walkFailed)
    }

    override suspend fun resolve(documentUri: Uri): LinkedDocument? = withContext(Dispatchers.IO) {
        val doc = runCatching { DocumentFile.fromSingleUri(context, documentUri) }.getOrNull()
            ?: return@withContext null
        if (doc.isDirectory || !doc.canRead()) return@withContext null
        LinkedDocument(documentUri, doc.name.orEmpty(), doc.lastModified())
    }
}
