package com.iridium.feature.library.impl.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.BookCoverArt
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumProgressBar
import com.iridium.core.designsystem.IridiumScrimPill
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.designsystem.sharedCover
import com.iridium.core.model.Book
import com.iridium.feature.library.impl.LibraryTestTags
import com.iridium.feature.library.impl.R

/** Cached singleton brush for the book spine crease highlight along cover edges. */
internal val BookSpineBrush = Brush.horizontalGradient(
    0.0f to Color.Black.copy(alpha = 0.22f),
    0.025f to Color.White.copy(alpha = 0.08f),
    0.06f to Color.Transparent,
)

/** 2:3 standard book-cover aspect ratio. */
internal const val COVER_ASPECT = 2f / 3f

/** Click + long-press handlers that read the latest book at tap time. */
internal data class BookCardClicks(
    val click: () -> Unit,
    val longClick: () -> Unit,
    val readLabel: String,
    val moreLabel: String,
)

/**
 * Latest-book reads at click time: metadata/bookmark updates must reach
 * taps without rebuilding handlers on every re-emission.
 */
@Composable
internal fun rememberBookCardClicks(
    book: Book,
    onRead: (Book) -> Unit,
    onDetails: (Book) -> Unit,
): BookCardClicks {
    val latestBook by rememberUpdatedState(book)
    val click = remember(book.id, book.progress, book.error, onRead) { { onRead(latestBook) } }
    val haptics = rememberIridiumHaptics()
    val longClick = remember(book.id, onDetails, haptics) {
        {
            haptics(IridiumHaptic.LongPress)
            onDetails(latestBook)
        }
    }
    return BookCardClicks(
        click = click,
        longClick = longClick,
        readLabel = stringResource(R.string.library_card_read, book.title),
        moreLabel = stringResource(R.string.library_card_more_actions),
    )
}

/**
 * Shared cover tile: artwork + spine crease + unreadable pill + bookmark
 * badge. Used by every grid variant; the list row uses a smaller plain
 * thumbnail instead. [overlay] hosts variant-specific content (e.g. the
 * cover-only title scrim) inside the tile.
 */
@Composable
internal fun BookCoverImage(
    book: Book,
    sharedCover: Boolean,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val bookmarkedLabel = stringResource(R.string.library_card_bookmarked)
    Box(
        modifier = modifier
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

        overlay()
    }
}

/** Wavy progress bar for started/finished books; nothing when untouched. */
@Composable
internal fun BookProgressIndicator(
    book: Book,
    modifier: Modifier = Modifier,
    topPadding: Dp = 6.dp,
) {
    if (book.isInProgress || book.isFinished) {
        IridiumProgressBar(
            progress = { book.progress },
            modifier = modifier
                .fillMaxWidth()
                .padding(top = topPadding),
        )
    }
}

/** Combined-click modifier shared by every card/row shell. */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.bookCardClickable(clicks: BookCardClicks): Modifier =
    combinedClickable(
        onClick = clicks.click,
        onClickLabel = clicks.readLabel,
        onLongClick = clicks.longClick,
        onLongClickLabel = clicks.moreLabel,
    )
