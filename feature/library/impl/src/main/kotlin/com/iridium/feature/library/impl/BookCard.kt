package com.iridium.feature.library.impl

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.BookCoverArt
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumMotion
import com.iridium.core.designsystem.IridiumProgressBar
import com.iridium.core.designsystem.IridiumScrimPill
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.designsystem.sharedCover
import com.iridium.core.model.Book

/** Cached singleton brush for the book spine crease highlight along cover edges. */
internal val BookSpineBrush = Brush.horizontalGradient(
    0.0f to Color.Black.copy(alpha = 0.22f),
    0.025f to Color.White.copy(alpha = 0.08f),
    0.06f to Color.Transparent,
)

/** 2:3 standard book-cover aspect ratio. */
private const val COVER_ASPECT = 2f / 3f

/**
 * Compact grid cell: full-bleed 2:3 cover with the title set below it in
 * normal flow, plus a wavy progress bar for started books and an error pill
 * for failed rows. Tapping reads; long-press opens details.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BookCard(
    book: Book,
    onRead: (Book) -> Unit,
    onDetails: (Book) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    sharedCover: Boolean = true,
    cardTag: String = LibraryTestTags.cardFor(book.id),
) {
    // Wrappers keyed by click-relevant fields (identity, progress, error) —
    // not full-book equality: cover/metadata re-emissions no longer
    // invalidate the handler, while a real progress save still refreshes it.
    val click = remember(book.id, book.progress, book.error, onRead) { { onRead(book) } }
    val haptics = rememberIridiumHaptics()
    val longClick = remember(book.id, onDetails, haptics) {
        {
            haptics(IridiumHaptic.LongPress)
            onDetails(book)
        }
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = IridiumMotion.defaultSpatialSpec(),
        label = "bookCardScale",
    )
    val bookmarkedLabel = stringResource(R.string.library_card_bookmarked)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (isPressed) 2.dp else 0.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(cardTag)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = click,
                onClickLabel = stringResource(R.string.library_card_read, book.title),
                onLongClick = longClick,
                onLongClickLabel = stringResource(R.string.library_card_details),
            ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .aspectRatio(COVER_ASPECT)
                    .then(if (sharedCover) Modifier.sharedCover(book.id) else Modifier),
            ) {
                BookCoverArt(
                    coverPath = book.coverPath,
                    contentDescription = null,
                )

                // Subtle physical book spine crease/highlight along the left edge.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(BookSpineBrush),
                )

                if (book.error != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.library_card_unreadable),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                // Bookmark badge marks favorites; same scrim language as the
                // reader counter pill so it survives bright covers.
                if (book.bookmarked) {
                    IridiumScrimPill(
                        text = "",
                        icon = IridiumIcons.Bookmark,
                        shape = CircleShape,
                        contentPadding = PaddingValues(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .semantics {
                                stateDescription = bookmarkedLabel
                            }
                            .testTag(LibraryTestTags.bookmarkBadgeFor(book.id)),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(
                    text = book.title,
                    style = if (compact) {
                        MaterialTheme.typography.bodyMedium
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                book.author?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (book.isInProgress || book.isFinished) {
                    IridiumProgressBar(
                        progress = { book.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

/**
 * Comfortable grid cell: compact cover with an emphasized author subtitle
 * below the title.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ComfortableBookCard(
    book: Book,
    onRead: (Book) -> Unit,
    onDetails: (Book) -> Unit,
    modifier: Modifier = Modifier,
    sharedCover: Boolean = true,
    cardTag: String = LibraryTestTags.cardFor(book.id),
) {
    BookCardChrome(
        book = book,
        onRead = onRead,
        onDetails = onDetails,
        modifier = modifier,
        sharedCover = sharedCover,
        cardTag = cardTag,
        metaPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        titleMinLines = 1,
        barTopPadding = 8.dp,
    )
}

/**
 * Cover-only grid cell: edge-to-edge artwork with scrim title overlay.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CoverOnlyBookCard(
    book: Book,
    onRead: (Book) -> Unit,
    onDetails: (Book) -> Unit,
    modifier: Modifier = Modifier,
    sharedCover: Boolean = true,
    cardTag: String = LibraryTestTags.cardFor(book.id),
) {
    val click = remember(book.id, book.progress, book.error, onRead) { { onRead(book) } }
    val haptics = rememberIridiumHaptics()
    val longClick = remember(book.id, onDetails, haptics) {
        {
            haptics(IridiumHaptic.LongPress)
            onDetails(book)
        }
    }
    val bookmarkedLabel = stringResource(R.string.library_card_bookmarked)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .testTag(cardTag)
            .combinedClickable(
                onClick = click,
                onClickLabel = stringResource(R.string.library_card_read, book.title),
                onLongClick = longClick,
                onLongClickLabel = stringResource(R.string.library_card_details),
            ),
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(COVER_ASPECT)
                .then(if (sharedCover) Modifier.sharedCover(book.id) else Modifier),
        ) {
            BookCoverArt(
                coverPath = book.coverPath,
                contentDescription = null,
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(BookSpineBrush),
            )

            // Bottom title overlay with gradient scrim.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to Color.Black.copy(alpha = 0.5f),
                            1f to Color.Black.copy(alpha = 0.85f),
                        ),
                    )
                    .padding(8.dp),
            ) {
                Column {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (book.isInProgress || book.isFinished) {
                        IridiumProgressBar(
                            progress = { book.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                    }
                }
            }

            if (book.error != null) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.library_card_unreadable),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            if (book.bookmarked) {
                IridiumScrimPill(
                    text = "",
                    icon = IridiumIcons.Bookmark,
                    shape = CircleShape,
                    contentPadding = PaddingValues(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .semantics {
                            stateDescription = bookmarkedLabel
                        }
                        .testTag(LibraryTestTags.bookmarkBadgeFor(book.id)),
                )
            }
        }
    }
}

/**
 * List row: horizontal layout with thumbnail, title, author/progress line,
 * and progress bar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BookListRow(
    book: Book,
    onRead: (Book) -> Unit,
    onDetails: (Book) -> Unit,
    modifier: Modifier = Modifier,
    cardTag: String = LibraryTestTags.cardFor(book.id),
) {
    val click = remember(book.id, book.progress, book.error, onRead) { { onRead(book) } }
    val haptics = rememberIridiumHaptics()
    val longClick = remember(book.id, onDetails, haptics) {
        {
            haptics(IridiumHaptic.LongPress)
            onDetails(book)
        }
    }
    val bookmarkedLabel = stringResource(R.string.library_card_bookmarked)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .testTag(cardTag)
            .combinedClickable(
                onClick = click,
                onClickLabel = stringResource(R.string.library_card_read, book.title),
                onLongClick = longClick,
                onLongClickLabel = stringResource(R.string.library_card_details),
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .aspectRatio(COVER_ASPECT)
                    .clip(MaterialTheme.shapes.small),
            ) {
                BookCoverArt(
                    coverPath = book.coverPath,
                    contentDescription = null,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val info = listOfNotNull(
                    book.author?.takeIf { it.isNotBlank() },
                    if (book.isInProgress || book.isFinished) {
                        "${(book.progress * 100).toInt()}% read"
                    } else {
                        null
                    },
                ).joinToString(" • ")

                if (info.isNotBlank()) {
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (book.isInProgress || book.isFinished) {
                    IridiumProgressBar(
                        progress = { book.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
            }

            if (book.bookmarked) {
                Icon(
                    imageVector = IridiumIcons.Bookmark,
                    contentDescription = bookmarkedLabel,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(20.dp),
                )
            }
        }
    }
}

/** Shared cover + badges + meta column for the text-below variants. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCardChrome(
    book: Book,
    onRead: (Book) -> Unit,
    onDetails: (Book) -> Unit,
    modifier: Modifier = Modifier,
    sharedCover: Boolean = true,
    cardTag: String = LibraryTestTags.cardFor(book.id),
    metaPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
    titleMinLines: Int = 2,
    barTopPadding: androidx.compose.ui.unit.Dp = 6.dp,
) {
    val click = remember(book.id, book.progress, book.error, onRead) { { onRead(book) } }
    val haptics = rememberIridiumHaptics()
    val longClick = remember(book.id, onDetails, haptics) {
        {
            haptics(IridiumHaptic.LongPress)
            onDetails(book)
        }
    }
    val bookmarkedLabel = stringResource(R.string.library_card_bookmarked)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .testTag(cardTag)
            .combinedClickable(
                onClick = click,
                onClickLabel = stringResource(R.string.library_card_read, book.title),
                onLongClick = longClick,
                onLongClickLabel = stringResource(R.string.library_card_details),
            ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .aspectRatio(COVER_ASPECT)
                    .then(if (sharedCover) Modifier.sharedCover(book.id) else Modifier),
            ) {
                BookCoverArt(
                    coverPath = book.coverPath,
                    contentDescription = null,
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(BookSpineBrush),
                )

                if (book.error != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.library_card_unreadable),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                if (book.bookmarked) {
                    IridiumScrimPill(
                        text = "",
                        icon = IridiumIcons.Bookmark,
                        shape = CircleShape,
                        contentPadding = PaddingValues(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .semantics {
                                stateDescription = bookmarkedLabel
                            }
                            .testTag(LibraryTestTags.bookmarkBadgeFor(book.id)),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(metaPadding),
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    minLines = titleMinLines,
                    overflow = TextOverflow.Ellipsis,
                )
                book.author?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (book.isInProgress || book.isFinished) {
                    IridiumProgressBar(
                        progress = { book.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = barTopPadding),
                    )
                }
            }
        }
    }
}
