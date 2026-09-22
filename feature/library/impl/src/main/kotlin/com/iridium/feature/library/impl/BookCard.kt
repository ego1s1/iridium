package com.iridium.feature.library.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.BookCoverArt
import com.iridium.core.designsystem.sharedCover
import com.iridium.core.model.Book

/** Slightly rounded cover corners: enough to read as a shelf, not a card. */
private val CoverCorner = RoundedCornerShape(8.dp)

/**
 * Shelf card: 2:3 cover, title + author below, progress bar when started.
 * Tap reads (or opens details for errored rows); long-press opens details.
 */
@Composable
internal fun BookCard(
    book: Book,
    onRead: (Book) -> Unit,
    onDetails: ((Book) -> Unit)?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onRead(book) },
                onLongClick = { onDetails?.invoke(book) },
            ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(CoverCorner)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .sharedCover(book.id),
        ) {
            BookCoverArt(
                coverPath = book.coverPath,
                contentDescription = book.title,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = book.title,
            style = if (compact) {
                MaterialTheme.typography.bodyMedium
            } else {
                MaterialTheme.typography.titleSmall
            },
            maxLines = if (compact) 1 else 2,
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
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { book.progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
            )
        }
    }
}
