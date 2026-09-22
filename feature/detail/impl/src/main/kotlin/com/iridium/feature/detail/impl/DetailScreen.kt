package com.iridium.feature.detail.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.iridium.core.designsystem.BookCoverArt
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumLoading
import com.iridium.core.designsystem.LocalNavAnimatedVisibilityScope
import com.iridium.core.designsystem.screenEnter
import com.iridium.core.designsystem.screenExit
import com.iridium.core.designsystem.screenPopEnter
import com.iridium.core.designsystem.screenPopExit
import com.iridium.core.designsystem.sharedCover
import com.iridium.core.model.Book
import com.iridium.feature.detail.api.DetailRoute

fun NavGraphBuilder.detailScreen(
    onBackClick: () -> Unit,
    onReadClick: (String) -> Unit,
    onRemoved: () -> Unit = onBackClick,
) {
    composable<DetailRoute>(
        enterTransition = { screenEnter() },
        exitTransition = { screenExit() },
        popEnterTransition = { screenPopEnter() },
        popExitTransition = { screenPopExit() },
    ) {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            DetailRoute(
                onBackClick = onBackClick,
                onReadClick = onReadClick,
                onRemoved = onRemoved,
            )
        }
    }
}

@Composable
internal fun DetailRoute(
    onBackClick: () -> Unit,
    onReadClick: (String) -> Unit,
    onRemoved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (val state = uiState) {
        DetailUiState.Loading -> IridiumLoading(modifier)
        DetailUiState.Gone -> {
            // Book was removed: pop back to the library.
            LaunchedEffect(Unit) { onRemoved() }
        }
        is DetailUiState.Success -> DetailScreen(
            state = state,
            onAction = viewModel::onAction,
            onBackClick = onBackClick,
            onReadClick = { onReadClick(state.book.id) },
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailScreen(
    state: DetailUiState.Success,
    onAction: (DetailAction) -> Unit,
    onBackClick: () -> Unit,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val book = state.book
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(IridiumIcons.Back, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onAction(DetailAction.ToggleBookmark(!book.bookmarked)) },
                    ) {
                        Icon(
                            imageVector = if (book.bookmarked) {
                                IridiumIcons.Bookmark
                            } else {
                                IridiumIcons.BookmarkBorder
                            },
                            contentDescription = stringResource(R.string.detail_bookmark),
                        )
                    }
                    IconButton(onClick = { onAction(DetailAction.AskRemove) }) {
                        Icon(IridiumIcons.Delete, contentDescription = stringResource(R.string.detail_remove))
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(contentType = "hero") {
                DetailHero(book = book, onReadClick = onReadClick)
            }
            if (state.toc.isNotEmpty()) {
                item(contentType = "tocHeader") {
                    Text(
                        text = stringResource(R.string.detail_chapters, state.toc.size),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(state.toc, key = { it.href }) { entry ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        HorizontalDivider(Modifier.padding(top = 6.dp))
                    }
                }
            }
            if (state.highlights.isNotEmpty()) {
                item(contentType = "hlHeader") {
                    Text(
                        text = stringResource(R.string.detail_highlights, state.highlights.size),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        if (state.confirmRemove) {
            AlertDialog(
                onDismissRequest = { onAction(DetailAction.DismissRemove) },
                title = { Text(stringResource(R.string.detail_remove_title)) },
                text = { Text(stringResource(R.string.detail_remove_body, book.title)) },
                confirmButton = {
                    TextButton(onClick = { onAction(DetailAction.ConfirmRemove) }) {
                        Text(stringResource(R.string.detail_remove_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onAction(DetailAction.DismissRemove) }) {
                        Text(stringResource(R.string.detail_remove_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun DetailHero(
    book: Book,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .width(120.dp)
                .height(180.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .sharedCover(book.id),
        ) {
            BookCoverArt(coverPath = book.coverPath, contentDescription = book.title)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            book.author?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (book.progress > 0f) {
                    stringResource(R.string.detail_progress, (book.progress * 100).toInt())
                } else {
                    stringResource(R.string.detail_unread)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (book.isInProgress || book.isFinished) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onReadClick) {
                Icon(IridiumIcons.Play, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (book.isInProgress) {
                        stringResource(R.string.detail_resume)
                    } else {
                        stringResource(R.string.detail_start)
                    },
                )
            }
        }
    }
}
