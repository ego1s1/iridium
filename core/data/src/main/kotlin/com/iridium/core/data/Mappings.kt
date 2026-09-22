package com.iridium.core.data

import com.iridium.core.database.BookEntity
import com.iridium.core.database.BookmarkEntity
import com.iridium.core.database.HighlightEntity
import com.iridium.core.model.Book
import com.iridium.core.model.BookError
import com.iridium.core.model.BookFormat
import com.iridium.core.model.Bookmark
import com.iridium.core.model.Highlight
import com.iridium.core.model.HighlightColor
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.TocEntry
import com.iridium.core.model.applyQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class TocEntryDto(val href: String, val title: String)

private val tocFormat = Json { ignoreUnknownKeys = true }

internal fun BookEntity.toModel(): Book = Book(
    id = id,
    title = title,
    author = author,
    format = runCatching { BookFormat.valueOf(format) }.getOrDefault(BookFormat.EPUB),
    spineCount = spineCount,
    sourcePath = sourcePath,
    coverPath = coverPath,
    progress = progress,
    lastLocator = lastLocator,
    sourceDisplayName = sourceDisplayName,
    sourceModified = sourceModified,
    createdAt = createdAt,
    updatedAt = updatedAt,
    error = error?.let { runCatching { BookError.valueOf(it) }.getOrNull() },
    bookmarked = bookmarked,
)

internal fun Book.toEntity(toc: List<TocEntry> = emptyList()): BookEntity = BookEntity(
    id = id,
    title = title,
    author = author,
    format = format.name,
    spineCount = spineCount,
    sourcePath = sourcePath,
    coverPath = coverPath,
    progress = progress,
    lastLocator = lastLocator,
    sourceDisplayName = sourceDisplayName,
    sourceModified = sourceModified,
    tocJson = tocFormat.encodeToString(toc.map { TocEntryDto(it.href, it.title) }),
    error = error?.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    bookmarked = bookmarked,
)
internal fun encodeToc(toc: List<TocEntry>): String =
    tocFormat.encodeToString(
        kotlinx.serialization.builtins.ListSerializer(TocEntryDto.serializer()),
        toc.map { TocEntryDto(it.href, it.title) },
    )

internal fun BookEntity.tocEntries(): List<TocEntry> {
    val raw = tocJson ?: return emptyList()
    return runCatching {
        tocFormat.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(TocEntryDto.serializer()),
            raw,
        ).map { TocEntry(it.href, it.title) }
    }.getOrDefault(emptyList())
}

internal fun HighlightEntity.toModel(): Highlight = Highlight(
    id = id,
    bookId = bookId,
    href = href,
    startLocator = startLocator,
    endLocator = endLocator,
    selectedText = selectedText,
    color = runCatching { HighlightColor.valueOf(color) }.getOrDefault(HighlightColor.YELLOW),
    note = note,
    createdAt = createdAt,
)

internal fun Highlight.toEntity(): HighlightEntity = HighlightEntity(
    id = id,
    bookId = bookId,
    href = href,
    startLocator = startLocator,
    endLocator = endLocator,
    selectedText = selectedText,
    color = color.name,
    note = note,
    createdAt = createdAt,
)

internal fun BookmarkEntity.toModel(): Bookmark = Bookmark(
    id = id,
    bookId = bookId,
    locator = locator,
    label = label,
    createdAt = createdAt,
)

internal fun Bookmark.toEntity(): BookmarkEntity = BookmarkEntity(
    id = id,
    bookId = bookId,
    locator = locator,
    label = label,
    createdAt = createdAt,
)

/** Shared query helper: filters/sorts an observed entity list. */
internal fun List<Book>.forQuery(query: LibraryQuery): List<Book> = applyQuery(query)

internal fun Flow<List<BookEntity>>.toBooks(): Flow<List<Book>> = map { list ->
    list.map { it.toModel() }
}
