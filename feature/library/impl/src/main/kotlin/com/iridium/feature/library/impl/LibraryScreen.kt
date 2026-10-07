package com.iridium.feature.library.impl

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iridium.core.designsystem.IridiumEmptyState
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.data.ContentHit
import com.iridium.core.model.Book
import com.iridium.core.model.LibraryDisplayMode

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
    var hasStorageAccess by remember { mutableStateOf(hasFullStorageAccess(context)) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val now = hasFullStorageAccess(context)
                // Grant arrived while away (onboarding, Settings): scan once.
                if (now && !hasStorageAccess) {
                    viewModel.onAction(LibraryAction.Rescan)
                }
                hasStorageAccess = now
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            val text = when (message) {
                is LibraryMessage.IndexFailed ->
                    context.getString(R.string.library_snack_index_failed, message.failed)
                LibraryMessage.ScanFailed ->
                    context.getString(R.string.library_snack_scan_failed)
                is LibraryMessage.IndexedForSearch ->
                    if (message.chapters > 0) {
                        context.getString(R.string.library_snack_indexed, message.chapters)
                    } else {
                        context.getString(R.string.library_snack_index_empty)
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
        hasStorageAccess = hasStorageAccess,
        onGrantAccess = { openStorageAccessSettings(context) },
        onAction = viewModel::onAction,
        onReadClick = { book ->
            if (book.error != null) onBookLongClick(book.id) else onBookClick(book.id)
        },
        // Long-press opens the quick-actions sheet; details live inside it.
        onDetailsClick = { viewModel.onAction(LibraryAction.OpenMenu(it.id)) },
        onOpenChapter = onOpenChapter,
        snackbarHost = snackbarHost,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LibraryScreen(
    uiState: LibraryUiState,
    hasStorageAccess: Boolean,
    onGrantAccess: () -> Unit,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (Book) -> Unit,
    onDetailsClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
    onOpenChapter: (bookId: String, href: String) -> Unit = { _, _ -> },
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    // Only the launching card registers a shared cover: tracking every card
    // costs a shared-transition overlay per scroll frame.
    var launchingId by remember { mutableStateOf<String?>(null) }
    val launchRead: (Book) -> Unit = { book ->
        launchingId = book.id
        onReadClick(book)
    }
    val launchDetails: (Book) -> Unit = { book ->
        launchingId = book.id
        onDetailsClick(book)
    }
    Scaffold(
        topBar = {
            LibraryCollapsingTopBar(
                title = stringResource(R.string.library_title),
                filterActive = uiState.query.hasActiveFilters(),
                onFilterClick = { onAction(LibraryAction.OpenFilter) },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.testTag("librarySnackbar"),
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            LibraryContent(
                state = uiState,
                hasStorageAccess = hasStorageAccess,
                onGrantAccess = onGrantAccess,
                onAction = onAction,
                onReadClick = launchRead,
                onDetailsClick = launchDetails,
                onOpenChapter = onOpenChapter,
                launchingId = launchingId,
            )
            if (uiState.filterOpen) {
                LibrarySortFilterSheet(
                    display = uiState.display,
                    onAction = onAction,
                )
            }
            val menuBook = uiState.menuBookId?.let { id ->
                uiState.books.firstOrNull { it.id == id }
            }
            if (menuBook != null) {
                LibraryMenuSheet(
                    book = menuBook,
                    deleteConfirm = uiState.menuDeleteConfirm,
                    onAction = onAction,
                    onReadClick = { id ->
                        uiState.books.firstOrNull { it.id == id }?.let(launchRead)
                    },
                    onDetailsClick = { id ->
                        uiState.books.firstOrNull { it.id == id }?.let(launchDetails)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryContent(
    state: LibraryUiState,
    hasStorageAccess: Boolean,
    onGrantAccess: () -> Unit,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (Book) -> Unit,
    onDetailsClick: (Book) -> Unit,
    onOpenChapter: (bookId: String, href: String) -> Unit,
    launchingId: String?,
    modifier: Modifier = Modifier,
) {
    val books = state.books
    val query = state.query
    val refreshing = state.refreshing
    val indexProgress = state.indexProgress
    val shelf = state.continueReading
    val contentHits = state.contentHits
    val indexing = state.indexing
    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    // Reset to top when the filter or text changes: switching Unread -> All
    // with a half-scrolled hero underneath reads as a layout glitch.
    LaunchedEffect(query.filter, query.text) {
        gridState.scrollToItem(0)
    }
    // The island floats once the grid moves: color + elevation animate so
    // the state change reads as lift, not a pop.
    val scrolled by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 ||
                gridState.firstVisibleItemScrollOffset > 0
        }
    }
    val islandColor by animateColorAsState(
        targetValue = if (scrolled) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        label = "searchIslandColor",
    )
    val islandElevation by animateDpAsState(
        targetValue = if (scrolled) 6.dp else 0.dp,
        label = "searchIslandElevation",
    )
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
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
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { onAction(LibraryAction.Rescan) },
                modifier = Modifier.weight(1f).fillMaxSize(),
            ) {
                if (books.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 76.dp)
                            .padding(horizontal = 12.dp),
                    ) {
                        LibraryQuickFilters(
                            selected = query.filter,
                            onSelect = { onAction(LibraryAction.FilterSelected(it)) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                        LibraryEmptyState(
                            searching = query.text.isNotBlank(),
                            hasStorageAccess = hasStorageAccess,
                            onRescan = { onAction(LibraryAction.Rescan) },
                            onGrantAccess = onGrantAccess,
                        )
                    }
                } else {
                    val display = state.display
                    val listMode = display.displayMode == LibraryDisplayMode.LIST
                    LazyVerticalGrid(
                        columns = if (listMode) {
                            GridCells.Fixed(1)
                        } else if (display.gridColumns > 0) {
                            GridCells.Fixed(display.gridColumns)
                        } else {
                            GridCells.Adaptive(128.dp)
                        },
                        state = gridState,
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = 76.dp,
                            end = 12.dp,
                            bottom = 176.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "quick_filters",
                            contentType = "quickFilters",
                        ) {
                            LibraryQuickFilters(
                                selected = query.filter,
                                onSelect = { onAction(LibraryAction.FilterSelected(it)) },
                            )
                        }
                        if (shelf.isNotEmpty() && query.text.isBlank()) {
                            val hero = shelf.first()
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "hero_now_reading_${hero.id}",
                                contentType = "nowReadingHero",
                            ) {
                                NowReadingHeroCard(
                                    book = hero,
                                    onResume = onReadClick,
                                    onDetails = onDetailsClick,
                                )                            }
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
                            val cardSharedCover = launchingId == book.id
                            when (display.displayMode) {
                                LibraryDisplayMode.COMFORTABLE -> ComfortableBookCard(
                                    book = book,
                                    onRead = onReadClick,
                                    onDetails = onDetailsClick,
                                    sharedCover = cardSharedCover,
                                )
                                LibraryDisplayMode.COVER_ONLY -> CoverOnlyBookCard(
                                    book = book,
                                    onRead = onReadClick,
                                    onDetails = onDetailsClick,
                                    sharedCover = cardSharedCover,
                                )
                                LibraryDisplayMode.LIST -> BookListRow(
                                    book = book,
                                    onRead = onReadClick,
                                    onDetails = onDetailsClick,
                                )
                                LibraryDisplayMode.COMPACT -> BookCard(
                                    book = book,
                                    onRead = onReadClick,
                                    onDetails = onDetailsClick,
                                    sharedCover = cardSharedCover,
                                )
                            }
                        }
                    }
                }
            }
        }
        // Floating search island: overlays the grid top so showing it never
        // pushes content down; the grid's 76dp top reserve is constant.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            LibrarySearchIsland(
                text = query.text,
                onTextChange = { onAction(LibraryAction.SearchTextChanged(it)) },
                placeholder = stringResource(R.string.library_search_label),
                containerColor = islandColor,
                shadowElevation = islandElevation,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        // Action island rides above the main navigator pill (which owns the
        // bottom ~92dp), so library mutations stay one tap away. Hidden on
        // empty screens: the empty state owns the CTA there, and a duplicate
        // "Link folder" would split the action.
        if (books.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 104.dp),
                contentAlignment = Alignment.Center,
            ) {
                LibraryActionIsland(
                    onRescan = { onAction(LibraryAction.Rescan) },
                    onIndex = { onAction(LibraryAction.IndexLibrary) },
                )
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    searching: Boolean,
    hasStorageAccess: Boolean,
    onRescan: () -> Unit,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Without full-storage access a scan is a dead end: offer the grant.
    // Otherwise offer a rescan (new files may have landed since launch).
    val grant = !searching && !hasStorageAccess
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
        actionLabel = if (grant) {
            stringResource(R.string.library_grant_access)
        } else {
            stringResource(R.string.library_rescan)
        },
        onAction = if (grant) onGrantAccess else onRescan,
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
