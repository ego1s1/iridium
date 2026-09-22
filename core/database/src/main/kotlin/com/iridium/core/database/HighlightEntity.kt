package com.iridium.core.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A highlight anchored to a stable locator range, with optional note. */
@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("bookId")],
)
data class HighlightEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val href: String,
    val startLocator: String,
    val endLocator: String,
    val selectedText: String,
    /** [com.iridium.core.model.HighlightColor] name. */
    val color: String,
    val note: String?,
    val createdAt: Long,
)
