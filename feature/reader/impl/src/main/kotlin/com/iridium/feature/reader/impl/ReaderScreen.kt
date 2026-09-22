package com.iridium.feature.reader.impl

import android.content.Intent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.iridium.core.designsystem.IridiumLoading
import com.iridium.feature.reader.api.ReaderRoute

fun NavGraphBuilder.readerScreen(onBackClick: () -> Unit) {
    composable<ReaderRoute> {
        ReaderRoute(onBackClick = onBackClick)
    }
}

@Composable
internal fun ReaderRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            when (message) {
                is ReaderMessage.Text -> snackbarHost.showSnackbar(message.text)
                is ReaderMessage.OpenUrl ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, message.url.toUri()))
                    }
                ReaderMessage.SelectTextFirst ->
                    snackbarHost.showSnackbar("Long-press text to select it first")
                ReaderMessage.HighlightSaved ->
                    snackbarHost.showSnackbar("Highlight saved")
                ReaderMessage.Pop -> onBackClick()
            }
        }
    }

    when (val state = uiState) {
        ReaderUiState.Loading -> IridiumLoading(modifier)
        ReaderUiState.Gone, ReaderUiState.OpenFailed -> {
            LaunchedEffect(Unit) {
                snackbarHost.showSnackbar(
                    if (state == ReaderUiState.OpenFailed) {
                        "Couldn't open this book"
                    } else {
                        "Book no longer in library"
                    },
                )
                onBackClick()
            }
        }
        is ReaderUiState.Ready -> ReaderScreen(
            state = state,
            onAction = viewModel::onAction,
            onBackClick = onBackClick,
            snackbarHost = snackbarHost,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderScreen(
    state: ReaderUiState.Ready,
    onAction: (ReaderAction) -> Unit,
    onBackClick: () -> Unit,
    snackbarHost: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var showAddDialog by remember { mutableStateOf(false) }

    // Per-book brightness override (-1 = system).
    DisposableEffect(state.prefs.brightness) {
        val window = activity?.window
        val previous = window?.attributes?.screenBrightness ?: -1f
        window?.let {
            val attrs = it.attributes
            attrs.screenBrightness = state.prefs.brightness
            it.attributes = attrs
        }
        onDispose {
            window?.let {
                val attrs = it.attributes
                attrs.screenBrightness = previous
                it.attributes = attrs
            }
        }
    }

    // Immersive reading: hide system bars with the chrome.
    DisposableEffect(state.chromeVisible) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        if (state.chromeVisible) {
            controller?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose { }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = state.chromeVisible,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = state.book.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            state.book.author?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onAction(ReaderAction.OpenToc) }) {
                            Icon(Icons.Rounded.List, contentDescription = "Contents")
                        }
                        IconButton(onClick = { onAction(ReaderAction.OpenHighlights) }) {
                            Icon(Icons.Rounded.Highlight, contentDescription = "Highlights")
                        }
                        IconButton(onClick = { onAction(ReaderAction.OpenSettings) }) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Reading settings")
                        }
                        var menuOpen by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Highlight selection") },
                                onClick = {
                                    menuOpen = false
                                    showAddDialog = true
                                },
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    ),
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.chromeVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                ReaderBottomBar(state = state, onAction = onAction)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        modifier = modifier,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // Readium content fills the whole area; chrome overlays it.
            NavigatorHost(bookId = state.book.id)
            if (!state.chromeVisible && state.prefs.showPageCounter) {
                state.positionText?.let {
                    androidx.compose.material3.Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }

    if (state.settingsOpen) {
        ReaderSettingsSheet(prefs = state.prefs, onAction = onAction)
    }
    if (state.tocOpen) {
        ReaderTocSheet(
            toc = state.toc,
            onEntryClick = { onAction(ReaderAction.GoTocEntry(it)) },
            onDismiss = { onAction(ReaderAction.CloseToc) },
        )
    }
    if (state.highlightsOpen) {
        ReaderHighlightsSheet(
            highlights = state.highlights,
            focusedId = state.focusedHighlightId,
            onDelete = { onAction(ReaderAction.DeleteHighlight(it)) },
            onDismiss = { onAction(ReaderAction.CloseHighlights) },
        )
    }
    if (showAddDialog) {
        AddHighlightDialog(
            onSave = { color, note ->
                showAddDialog = false
                onAction(ReaderAction.AddHighlight(color, note))
            },
            onDismiss = { showAddDialog = false },
        )
    }
}

/** Hosts the [ReaderHostFragment] inside Compose via FragmentContainerView. */
@Composable
private fun NavigatorHost(bookId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    if (activity == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Reader unavailable")
        }
        return
    }
    // Stable id so the FragmentManager can restore the navigator fragment
    // across configuration changes (a generated id would not be restorable).
    val containerId = R.id.reader_navigator_container
    val tag = "iridium_reader_$bookId"
    AndroidView(
        factory = { ctx -> FragmentContainerView(ctx).apply { id = containerId } },
        update = {},
        modifier = modifier.fillMaxSize(),
    )
    LaunchedEffect(bookId, containerId) {
        val fm = activity.supportFragmentManager
        if (fm.findFragmentById(containerId) == null) {
            fm.commitNow { add(containerId, ReaderHostFragment.newInstance(), tag) }
        }
    }
    DisposableEffect(bookId, containerId) {
        onDispose {
            // Leaving the reader: detach the navigator so a stale WebView never
            // lingers behind the library. commitAllowingStateLoss (not
            // commitNow) avoids throwing if the host already saved state.
            val fm = activity.supportFragmentManager
            fm.findFragmentByTag(tag)?.let {
                fm.beginTransaction().remove(it).commitAllowingStateLoss()
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    state: ReaderUiState.Ready,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scrub by remember(state.progression) { mutableFloatStateOf(state.progression) }
    var scrubbing by remember { mutableStateOf(false) }
    BottomAppBar(modifier = modifier) {
        IconButton(onClick = { onAction(ReaderAction.GoBackward()) }) {
            Icon(Icons.AutoMirrored.Rounded.NavigateBefore, contentDescription = "Previous")
        }
        Column(Modifier.weight(1f)) {
            Slider(
                value = if (scrubbing) scrub else state.progression,
                onValueChange = {
                    scrub = it
                    scrubbing = true
                },
                onValueChangeFinished = {
                    scrubbing = false
                    onAction(ReaderAction.SeekTo(scrub))
                },
            )
            state.positionText?.let {
                Text(
                    text = "${(state.progression * 100).toInt()}% · $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
        IconButton(onClick = { onAction(ReaderAction.GoForward()) }) {
            Icon(Icons.AutoMirrored.Rounded.NavigateNext, contentDescription = "Next")
        }
    }
}
