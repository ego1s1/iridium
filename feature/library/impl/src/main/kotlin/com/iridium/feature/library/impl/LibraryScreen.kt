package com.iridium.feature.library.impl

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iridium.core.designsystem.IridiumEmptyState
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.data.ContentHit
import com.iridium.core.model.Book
import com.iridium.core.model.LibraryQuery

/** Public tab content for the main viewport. The ViewModel type never appears here. */
@Composable
fun LibraryTabContent(
    onReadClick: (String) -> Unit,
    onBookLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenChapter: (bookId: String, href: String) -> Unit = { _, _ -> },
    onResumeAvailable: (Book?) -> Unit = {},
) {
    LibraryRoute(
        onBookClick = onReadClick,
        onBookLongClick = onBookLongClick,
        modifier = modifier,
        onOpenChapter = onOpenChapter,
        onResumeAvailable = onResumeAvailable,
    )
}

@Composable
internal fun LibraryRoute(
    onBookClick: (String) -> Unit,
    onBookLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenChapter: (bookId: String, href: String) -> Unit = { _, _ -> },
    onResumeAvailable: (Book?) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            // Persist the grant so rescans keep working across restarts.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.onAction(LibraryAction.LinkFolder(uri))
        }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                is LibraryMessage.IndexFailed ->
                    "${message.failed} book(s) couldn't be read"
                LibraryMessage.ScanFailed -> "Couldn't scan that folder"
                is LibraryMessage.IndexedForSearch ->
                    if (message.chapters > 0) {
                        "Indexed ${message.chapters} chapters for search"
                    } else {
                        "Nothing to index yet"
                    }
            }
            snackbarHost.showSnackbar(text)
        }
    }
    // Resume rides its own cached flow so chrome-only changes never rescan.
    val resumeTarget by viewModel.resumeTarget.collectAsStateWithLifecycle()
    LaunchedEffect(resumeTarget) { onResumeAvailable(resumeTarget) }
    LibraryScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onReadClick = { book ->
            if (book.error != null) onBookLongClick(book.id) else onBookClick(book.id)
        },
        onDetailsClick = { onBookLongClick(it.id) },
        onLinkFolder = { folderLauncher.launch(null) },
        onOpenChapter = onOpenChapter,
        snackbarHost = snackbarHost,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LibraryScreen(
    uiState: LibraryUiState,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (Book) -> Unit,
    onDetailsClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
    onLinkFolder: () -> Unit = {},
    onOpenChapter: (bookId: String, href: String) -> Unit = { _, _ -> },
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            LibraryTopBar(
                searchOpen = uiState.searchOpen,
                onSearchClick = { onAction(LibraryAction.ToggleSearch) },
                onAction = onAction,
            )
        },
        floatingActionButton = {
            var expanded by remember { mutableStateOf(false) }
            FloatingActionButtonMenu(
                expanded = expanded,
                button = {
                    ToggleFloatingActionButton(
                        checked = expanded,
                        onCheckedChange = { expanded = it },
                    ) {
                        Icon(
                            imageVector = if (expanded) IridiumIcons.Close else IridiumIcons.Add,
                            contentDescription = stringResource(R.string.library_fab_actions),
                        )
                    }
                },
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        expanded = false
                        onLinkFolder()
                    },
                    icon = { Icon(IridiumIcons.ImportFolder, contentDescription = null) },
                    text = { Text(stringResource(R.string.library_link_folder)) },
                )
                if (uiState.linked) {
                    FloatingActionButtonMenuItem(
                        onClick = {
                            expanded = false
                            onAction(LibraryAction.Rescan)
                        },
                        icon = { Icon(IridiumIcons.Search, contentDescription = null) },
                        text = { Text(stringResource(R.string.library_rescan)) },
                    )
                    FloatingActionButtonMenuItem(
                        onClick = {
                            expanded = false
                            onAction(LibraryAction.IndexLibrary)
                        },
                        icon = { Icon(IridiumIcons.List, contentDescription = null) },
                        text = { Text(stringResource(R.string.library_index_for_search)) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        modifier = modifier,
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            LibraryContent(
                books = uiState.books,
                query = uiState.query,
                refreshing = uiState.refreshing,
                indexProgress = uiState.indexProgress,
                searchOpen = uiState.searchOpen,
                linked = uiState.linked,
                shelf = uiState.continueReading,
                contentHits = uiState.contentHits,
                indexing = uiState.indexing,
                onAction = onAction,
                onReadClick = onReadClick,
                onDetailsClick = onDetailsClick,
                onLinkFolder = onLinkFolder,
                onOpenChapter = onOpenChapter,
            )
            if (uiState.filterOpen) {
                LibrarySortFilterSheet(
                    display = queryToDisplay(uiState.query),
                    onAction = onAction,
                )
            }
        }
    }
}

private fun queryToDisplay(query: LibraryQuery) = com.iridium.core.model.LibraryDisplay(
    sortOrder = query.sortOrder,
    filter = query.filter,
    hideErrors = query.hideErrors,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    searchOpen: Boolean,
    onSearchClick: () -> Unit,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.library_title),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = if (searchOpen) IridiumIcons.Close else IridiumIcons.Search,
                    contentDescription = stringResource(R.string.library_action_search),
                )
            }
            IconButton(onClick = { onAction(LibraryAction.OpenFilter) }) {
                Icon(
                    imageVector = IridiumIcons.Tune,
                    contentDescription = stringResource(R.string.library_action_sort_filter),
                )
            }
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryContent(
    books: List<Book>,
    query: LibraryQuery,
    refreshing: Boolean,
    indexProgress: IndexProgress?,
    searchOpen: Boolean,
    linked: Boolean,
    shelf: List<Book>,
    contentHits: List<ContentHit>,
    indexing: Boolean,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (Book) -> Unit,
    onDetailsClick: (Book) -> Unit,
    onLinkFolder: () -> Unit,
    onOpenChapter: (bookId: String, href: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        // Thin determinate bar: scanning never hides the books already on
        // screen, and large rescans never read as a stuck spinner.
        val progress = indexProgress
        when {
            refreshing && progress != null && progress.total > 0 ->
                LinearProgressIndicator(
                    progress = {
                        progress.done.coerceAtMost(progress.total).toFloat() / progress.total
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            // Chapter indexing is indeterminate: the work is per-book, and a
            // fake percentage would be less honest than a sweeping bar.
            indexing -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        AnimatedVisibility(visible = searchOpen) {
            val searchFocus = remember { FocusRequester() }
            val keyboard = LocalSoftwareKeyboardController.current
            LaunchedEffect(searchOpen) {
                if (searchOpen) {
                    searchFocus.requestFocus()
                    keyboard?.show()
                } else {
                    keyboard?.hide()
                }
            }
            OutlinedTextField(
                value = query.text,
                onValueChange = { onAction(LibraryAction.SearchTextChanged(it)) },
                label = { Text(stringResource(R.string.library_search_label)) },
                leadingIcon = { Icon(IridiumIcons.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onAction(LibraryAction.ToggleSearch) }),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp)
                    .focusRequester(searchFocus)
                    .focusable(),
            )
        }
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { onAction(LibraryAction.Rescan) },
            modifier = Modifier.weight(1f).fillMaxSize(),
        ) {
            if (books.isEmpty()) {
                LibraryEmptyState(
                    searching = query.text.isNotBlank(),
                    linked = linked,
                    onRescan = { onAction(LibraryAction.Rescan) },
                    onLinkFolder = onLinkFolder,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(128.dp),
                    contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (shelf.isNotEmpty() && query.text.isBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "continueShelf") {
                            ContinueShelf(
                                books = shelf,
                                onReadClick = onReadClick,
                                onDetailsClick = onDetailsClick,
                            )
                        }
                    }
                    // Full-text matches sit above the shelf results: when the
                    // user typed a word they are usually looking inside books.
                    if (contentHits.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, contentType = "hitsHeader") {
                            Text(
                                text = stringResource(
                                    R.string.library_content_hits,
                                    contentHits.size,
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(
                            contentHits,
                            key = { "${it.bookId}#${it.href}" },
                            contentType = { "hit" },
                        ) { hit ->
                            ContentHitRow(
                                hit = hit,
                                onClick = { onOpenChapter(hit.bookId, hit.href) },
                            )
                        }
                    }
                    items(
                        books,
                        key = { it.id },
                        contentType = { book ->
                            (if (book.error != null) 4 else 0) +
                                (if (book.isInProgress) 2 else 0) +
                                (if (book.isFinished) 1 else 0)
                        },
                    ) { book ->
                        BookCard(
                            book = book,
                            onRead = onReadClick,
                            onDetails = onDetailsClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueShelf(
    books: List<Book>,
    onReadClick: (Book) -> Unit,
    onDetailsClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.library_continue_title),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(books, key = { it.id }) { book ->
                BookCard(
                    book = book,
                    onRead = onReadClick,
                    onDetails = onDetailsClick,
                    compact = true,
                    modifier = Modifier.fillParentMaxWidth(0.42f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun LibraryEmptyState(
    searching: Boolean,
    linked: Boolean,
    onRescan: () -> Unit,
    onLinkFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Unlinked and not searching, rescan is a dead end: offer folder linking.
    val link = !searching && !linked
    IridiumEmptyState(
        icon = IridiumIcons.MenuBook,
        title = if (searching) {
            stringResource(R.string.library_empty_search_title)
        } else {
            stringResource(R.string.library_empty_title)
        },
        body = if (searching) {
            stringResource(R.string.library_empty_search_body)
        } else {
            stringResource(R.string.library_empty_body)
        },
        actionLabel = if (link) {
            stringResource(R.string.library_link_folder)
        } else {
            stringResource(R.string.library_rescan)
        },
        onAction = if (link) onLinkFolder else onRescan,
        modifier = modifier,
        bottomPadding = 112.dp,
    )
}

/** One full-text match: book, chapter, and the sentence around the hit. */
@Composable
private fun ContentHitRow(
    hit: ContentHit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = hit.bookTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = hit.chapterTitle,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = hit.snippet,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
