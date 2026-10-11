package com.iridium.core.data

import com.iridium.core.model.HighlightColor
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serialized library metadata for backup/restore/sync. Books are matched by
 * [BackupBook.sourcePath]; the EPUB files themselves live on device and are
 * never copied — only reading state, bookmarks and annotations are backed up.
 */
@Serializable
data class LibraryBackup(
    val version: Int = VERSION,
    val createdAt: Long = 0L,
    val books: List<BackupBook> = emptyList(),
) {
    companion object {
        const val VERSION = 1
    }
}

@Serializable
data class BackupBook(
    val sourcePath: String,
    val title: String,
    val author: String? = null,
    val progress: Float = 0f,
    val lastLocator: String? = null,
    val bookmarked: Boolean = false,
    val highlights: List<BackupHighlight> = emptyList(),
    val bookmarks: List<BackupBookmark> = emptyList(),
)

@Serializable
data class BackupHighlight(
    val id: String,
    val href: String,
    val startLocator: String,
    val endLocator: String,
    val selectedText: String,
    val color: String,
    val note: String? = null,
    val createdAt: Long = 0L,
)

@Serializable
data class BackupBookmark(
    val id: String,
    val locator: String,
    val label: String? = null,
    val createdAt: Long = 0L,
)

/** Deterministic JSON encode/decode of [LibraryBackup] (pure, round-trip). */
object LibraryBackupCodec {

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(backup: LibraryBackup): String = json.encodeToString(backup)

    fun decode(jsonString: String): LibraryBackup = json.decodeFromString(jsonString)
}

/** Maps a serialized color name back to a [HighlightColor] (default YELLOW). */
fun backupColor(name: String?): HighlightColor =
    runCatching { HighlightColor.valueOf(name.orEmpty()) }.getOrDefault(HighlightColor.YELLOW)
