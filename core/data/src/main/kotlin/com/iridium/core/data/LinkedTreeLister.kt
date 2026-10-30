package com.iridium.core.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.iridium.core.common.IoDispatcher
import com.iridium.core.datastore.IridiumPreferencesDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LinkedTreeLister {

    // SAF walk guards (unparseable URI, revoked grant, unreadable child)
    // each skip the folder: several continues are the readable shape here.
    @Suppress("LoopWithTooManyJumpStatements")
    override suspend fun listBooks(): LinkedTreeListResult = withContext(ioDispatcher) {
        val folders = preferences.linkedFolders.first()
        if (folders.isEmpty()) return@withContext LinkedTreeListResult(emptyList(), walkFailed = false)
        val resolver = context.contentResolver
        val persisted = resolver.persistedUriPermissions
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
            val rootId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            val rootUri = rootId?.let {
                runCatching { DocumentsContract.buildDocumentUriUsingTree(treeUri, it) }.getOrNull()
            }
            if (rootId == null || rootUri == null) {
                walkFailed = true
                continue
            }
            // One cursor query per directory instead of a DocumentFile getter
            // per field per child: DocumentFile.name / isDirectory /
            // lastModified are each a separate provider round trip, and a
            // folder of 500 books paid ~2000 of them on every launch.
            val docIds = mutableMapOf(rootUri.toString() to rootId)
            val root = TreeDoc(
                uri = rootUri,
                name = "",
                isDirectory = true,
                modified = 0L,
            )
            val result = collectLinkedBooks(
                root = root,
                childrenOf = { dir ->
                    val parentId = docIds[dir.uri.toString()] ?: return@collectLinkedBooks null
                    queryChildren(resolver, treeUri, parentId, docIds)
                },
            )
            books += result.documents
            walkFailed = walkFailed || result.walkFailed
        }
        LinkedTreeListResult(books, walkFailed)
    }

    /**
     * Children of one directory in a single provider query, recording each
     * child's document id so its own children can be listed later.
     */
    private fun queryChildren(
        resolver: ContentResolver,
        treeUri: Uri,
        parentId: String,
        docIds: MutableMap<String, String>,
    ): List<TreeDoc>? = try {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        resolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val modifiedIndex =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idIndex) ?: continue
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                    docIds[childUri.toString()] = documentId
                    add(
                        TreeDoc(
                            uri = childUri,
                            name = cursor.getString(nameIndex).orEmpty(),
                            isDirectory = cursor.getString(mimeIndex) ==
                                DocumentsContract.Document.MIME_TYPE_DIR,
                            modified = if (cursor.isNull(modifiedIndex)) {
                                0L
                            } else {
                                cursor.getLong(modifiedIndex)
                            },
                        ),
                    )
                }
            }
        }
    } catch (_: Exception) {
        // Revoked grant, unreadable provider, malformed cursor: the tree read
        // as incomplete, which must never prune rows.
        null
    }

    private companion object {
        val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
    }

    override suspend fun resolve(documentUri: Uri): LinkedDocument? = withContext(ioDispatcher) {
        val doc = runCatching { DocumentFile.fromSingleUri(context, documentUri) }.getOrNull()
            ?: return@withContext null
        if (doc.isDirectory || !doc.canRead()) return@withContext null
        LinkedDocument(documentUri, doc.name.orEmpty(), doc.lastModified())
    }
}
