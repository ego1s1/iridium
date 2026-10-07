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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.iridium.core.designsystem.BookCoverArt
import com.iridium.core.designsystem.IridiumCollapsingTopBar
import com.iridium.core.designsystem.IridiumConfirmDialog
import com.iridium.core.designsystem.IridiumContentWell
import com.iridium.core.designsystem.IridiumFilledTonalIconButton
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIconButton
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumLoading
import com.iridium.core.designsystem.IridiumPrimaryButton
import com.iridium.core.designsystem.rememberIridiumHaptics
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
    val haptics = rememberIridiumHaptics()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        topBar = {
            IridiumCollapsingTopBar(
                title = book.title,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IridiumFilledTonalIconButton(onClick = onBackClick) {
                        Icon(IridiumIcons.Back, contentDescription = stringResource(R.string.detail_back))
                    }
                },
                actions = {
                    IridiumIconButton(
                        onClick = {
                            haptics(
                                if (book.bookmarked) {
                                    IridiumHaptic.ToggleOff
                                } else {
                                    IridiumHaptic.ToggleOn
                                },
                            )
                            onAction(DetailAction.ToggleBookmark(!book.bookmarked))
                        },
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
                    IridiumIconButton(onClick = { onAction(DetailAction.AskRemove) }) {
                        Icon(IridiumIcons.Delete, contentDescription = stringResource(R.string.detail_remove))
                    }
                },
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        IridiumContentWell(Modifier.padding(padding)) {
            // Completion affordance only: TOC rows stay non-interactive.
            val completedToc = remember(state.book.lastLocator, state.book.progress, state.toc) {
                completedTocIndices(state.toc, state.book.lastLocator, state.book.isFinished)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            item(contentType = "hero") {
                DetailHero(book = book)
            }
            item(contentType = "actions") {
                IridiumPrimaryButton(
                    onClick = onReadClick,
                    haptic = IridiumHaptic.PrimaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detailRead"),
                ) {
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
            item(contentType = "metadata") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MetadataRow(
                        label = stringResource(R.string.detail_meta_format),
                        value = book.format.name,
                    )
                    MetadataRow(
                        label = stringResource(R.string.detail_meta_file),
                        value = book.sourceDisplayName,
                    )
                }
            }
            if (book.error != null) {
                item(contentType = "error") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.detail_unreadable_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.detail_unreadable_body,
                                    book.sourceDisplayName,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
            if (state.toc.isNotEmpty()) {
                item(contentType = "tocHeader") {
                    Text(
                        text = stringResource(R.string.detail_chapters, state.toc.size),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                // Completion affordance is hoisted above (remember is
                // not allowed inside the LazyColumn DSL scope).
                // Keys include the index: several entries may share one file
                // (fragment hrefs resolve to the same path), and href alone
                // is not unique — it crashed LazyColumn on real books.
                itemsIndexed(state.toc, key = { index, entry -> "$index:${entry.href}" }) { index, entry ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (index in completedToc) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = IridiumIcons.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("tocCompleted$index"),
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(top = 6.dp))
                    }
                }
            }
            if (state.bookmarks.isNotEmpty()) {
                item(contentType = "bmHeader") {
                    Text(
                        text = stringResource(R.string.detail_bookmarks, state.bookmarks.size),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(state.bookmarks, key = { it.id }) { bookmark ->
                    AnnotationRow(
                        label = bookmark.label.orEmpty().ifBlank {
                            stringResource(R.string.detail_bookmark_untitled)
                        },
                        secondary = null,
                        onDelete = { onAction(DetailAction.DeleteBookmark(bookmark.id)) },
                    )
                }
            }
            if (state.highlights.isNotEmpty()) {
                item(contentType = "hlHeader") {
                    Text(
                        text = stringResource(R.string.detail_highlights, state.highlights.size),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(state.highlights, key = { it.id }) { highlight ->
                    AnnotationRow(
                        label = highlight.note.orEmpty().ifBlank {
                            highlight.selectedText.ifBlank {
                                highlight.href.substringAfterLast('/')
                            }
                        },
                        secondary = highlight.note?.let { highlight.selectedText.takeIf { t -> t.isNotBlank() } },
                        onDelete = { onAction(DetailAction.DeleteHighlight(highlight.id)) },
                    )
                }
            }
        }
        if (state.confirmRemove) {
            IridiumConfirmDialog(
                title = stringResource(R.string.detail_remove_title),
                message = stringResource(R.string.detail_remove_body, book.title),
                confirmLabel = stringResource(R.string.detail_remove_confirm),
                onConfirm = { onAction(DetailAction.ConfirmRemove) },
                onDismiss = { onAction(DetailAction.DismissRemove) },
                dismissLabel = stringResource(R.string.detail_remove_cancel),
                icon = IridiumIcons.Delete,
                destructive = true,
                confirmTestTag = "detailConfirmRemove",
                dismissTestTag = "detailDismissRemove",
            )
        }
    }
    }
}

@Composable
private fun DetailHero(
    book: Book,
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
        }
    }
}

/** One metadata row: fixed label slot + two-line ellipsis value. */
@Composable
private fun MetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .widthIn(min = 88.dp)
                .padding(end = 8.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
/** One annotation (highlight or bookmark) with a delete action. */
@Composable
private fun AnnotationRow(
    label: String,
    secondary: String?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IridiumIconButton(onClick = onDelete) {
            Icon(IridiumIcons.Delete, contentDescription = stringResource(R.string.detail_delete))
        }
    }
    HorizontalDivider()
}
