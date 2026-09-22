package com.iridium.feature.library.impl

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.iridium.core.designsystem.IridiumEmptyState
import com.iridium.core.designsystem.IridiumLoading
import com.iridium.core.model.Book
import com.iridium.core.model.LibraryQuery
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

fun NavGraphBuilder.libraryScreen(
    onBookClick: (String) -> Unit,
    onBookLongClick: (String) -> Unit,
) {
    composable<LibraryRoute> {
        LibraryRoute(
            onBookClick = onBookClick,
            onBookLongClick = onBookLongClick,
        )
    }
}

@Composable
internal fun LibraryRoute(
    onBookClick: (String) -> Unit,
    onBookLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.onAction(LibraryAction.ImportSelected(uri))
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect {
            snackbarHost.showSnackbar("Couldn't import that EPUB")
        }
    }
    LibraryScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onReadClick = { book ->
            if (book.error != null) onBookLongClick(book.id) else onBookClick(book.id)
        },
        onDetailsClick = { onBookLongClick(it.id) },
        onImportClick = { importLauncher.launch(arrayOf("application/epub+zip", "application/epub")) },
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
    onImportClick: () -> Unit = {},
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            if (uiState is LibraryUiState.Success) {
                LibraryTopBar(
                    searchOpen = uiState.searchOpen,
                    onSearchClick = { onAction(LibraryAction.ToggleSearch) },
                    onAction = onAction,
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onImportClick) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.library_import))
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        modifier = modifier,
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            when (uiState) {
                LibraryUiState.Loading -> IridiumLoading()
                is LibraryUiState.Success -> {
                    LibraryContent(
                        books = uiState.books,
                        query = uiState.query,
                        refreshing = uiState.refreshing,
                        searchOpen = uiState.searchOpen,
                        shelf = uiState.continueReading,
                        onAction = onAction,
                        onReadClick = onReadClick,
                        onDetailsClick = onDetailsClick,
                        onImportClick = onImportClick,
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
                    imageVector = if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.library_action_search),
                )
            }
            IconButton(onClick = { onAction(LibraryAction.OpenFilter) }) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
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
    searchOpen: Boolean,
    shelf: List<Book>,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (Book) -> Unit,
    onDetailsClick: (Book) -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
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
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
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
                onRefresh = { onAction(LibraryAction.Refresh) },
                modifier = Modifier.weight(1f).fillMaxSize(),
            ) {
                if (books.isEmpty()) {
                    IridiumEmptyState(
                        icon = Icons.Rounded.MenuBook,
                        title = if (query.text.isNotBlank()) {
                            stringResource(R.string.library_empty_search_title)
                        } else {
                            stringResource(R.string.library_empty_title)
                        },
                        body = if (query.text.isNotBlank()) {
                            stringResource(R.string.library_empty_search_body)
                        } else {
                            stringResource(R.string.library_empty_body)
                        },
                        actionLabel = stringResource(R.string.library_empty_import),
                        onAction = onImportClick,
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(128.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 12.dp, top = 12.dp, end = 12.dp, bottom = 112.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (shelf.isNotEmpty() && query.text.isBlank()) {
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                contentType = "continueShelf",
                            ) {
                                ContinueShelf(
                                    books = shelf,
                                    onReadClick = onReadClick,
                                    onDetailsClick = onDetailsClick,
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
