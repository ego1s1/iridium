package com.iridium.core.model

/** Reading flow for the EPUB navigator. Mirrors Lithium's Auto/Paged/Scrolled. */
enum class ReadingFlow { AUTO, PAGED, SCROLLED }

enum class BookFormat { EPUB }

enum class BookError { CORRUPT, EMPTY, UNSUPPORTED }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class MotionStyle { EXPRESSIVE, CALM }

data class Book(
    val id: String,
    val title: String,
    val author: String?,
    val coverPath: String?,
    val sourceUri: String,
    val progress: Float = 0f,
    val lastLocator: String? = null,
    val bookmarked: Boolean = false,
    val error: BookError? = null,
    val updatedAt: Long = 0L,
) {
    val isInProgress: Boolean get() = progress > 0f && progress < 1f
    val isFinished: Boolean get() = progress >= 1f
}
