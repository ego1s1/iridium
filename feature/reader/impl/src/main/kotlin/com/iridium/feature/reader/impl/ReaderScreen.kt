package com.iridium.feature.reader.impl

import android.content.Intent
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumLoading
import com.iridium.core.designsystem.IridiumScrimPill
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.designsystem.LocalNavAnimatedVisibilityScope
import com.iridium.core.designsystem.readerEnter
import com.iridium.core.designsystem.readerExit
import com.iridium.feature.reader.api.ReaderKeyInterceptor
import com.iridium.feature.reader.api.ReaderRoute

fun NavGraphBuilder.readerScreen(onBackClick: () -> Unit) {
    composable<ReaderRoute>(
        enterTransition = { readerEnter() },
        exitTransition = { readerExit() },
        popEnterTransition = { readerEnter() },
        popExitTransition = { readerExit() },
    ) {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            ReaderRoute(onBackClick = onBackClick)
        }
    }
}

@Composable
internal fun ReaderRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionReady by viewModel.sessionReady.collectAsStateWithLifecycle()
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
            sessionReady = sessionReady,
            volumePagingActive = viewModel.volumePagingActive.collectAsStateWithLifecycle().value,
            dictionaryState = viewModel.dictionaryUi.collectAsStateWithLifecycle().value,
            onAction = viewModel::onAction,
            onVolumeKeyEvent = viewModel::onVolumeKeyEvent,
            onBackClick = onBackClick,
            snackbarHost = snackbarHost,
            modifier = modifier,
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
)
@Composable
internal fun ReaderScreen(
    state: ReaderUiState.Ready,
    sessionReady: Boolean,
    volumePagingActive: Boolean,
    dictionaryState: DictionaryUiState?,
    onAction: (ReaderAction) -> Unit,
    onBackClick: () -> Unit,
    snackbarHost: SnackbarHostState,
    onVolumeKeyEvent: (KeyEvent) -> Boolean = { false },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var showAddDialog by remember { mutableStateOf(false) }
    val haptics = rememberIridiumHaptics()

    // Book text always stays clear of the status and navigation zones, even
    // in immersive mode: visibility-ignoring insets are stable, so toggling
    // the chrome never resizes the navigator (no repagination).
    val topSafeInsets = WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top)
    val bottomSafeInsets =
        WindowInsets.navigationBarsIgnoringVisibility.only(WindowInsetsSides.Bottom)

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
        // Leaving the reader with hidden chrome must not leave the library
        // with hidden system bars.
        onDispose {
            controller?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    // Seamless system bars: tint status + navigation bars to the page
    // background of the active reader theme so no black strips frame the
    // page, with icon contrast to match. Unrelated to the visibility effect
    // above (that one only shows/hides); restored when the theme changes.
    DisposableEffect(state.prefs.theme) {
        val window = activity?.window
        val controller =
            activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        if (window == null || controller == null) return@DisposableEffect onDispose { }
        val previousStatus = window.statusBarColor
        val previousNav = window.navigationBarColor
        val previousLightStatus = controller.isAppearanceLightStatusBars
        val previousLightNav = controller.isAppearanceLightNavigationBars
        val pageBg = state.prefs.theme.pageBackground()
        window.statusBarColor = pageBg.toArgb()
        window.navigationBarColor = pageBg.toArgb()
        controller.isAppearanceLightStatusBars = state.prefs.theme.lightSystemBars()
        controller.isAppearanceLightNavigationBars = state.prefs.theme.lightSystemBars()
        onDispose {
            window.statusBarColor = previousStatus
            window.navigationBarColor = previousNav
            controller.isAppearanceLightStatusBars = previousLightStatus
            controller.isAppearanceLightNavigationBars = previousLightNav
        }
    }

    // Keep the screen awake while reading, when the user asked for it.
    DisposableEffect(state.prefs.keepScreenOn) {
        val window = activity?.window
        if (state.prefs.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    /**
     * Volume-key paging. The handler is installed only while the gate holds
     * (single source: [ReaderViewModel.volumePagingActive]), so volume always
     * works normally everywhere else; it consumes both the down and up events
     * so the system volume panel never appears during a page turn.
     */
    DisposableEffect(volumePagingActive) {
        // Mirrors the ViewModel gate (hidden-chrome + invert swap); installing
        // only while active keeps volume normal everywhere else.
        ReaderKeyInterceptor.handler = if (volumePagingActive) onVolumeKeyEvent else null
        onDispose { ReaderKeyInterceptor.handler = null }
    }

    // Full-bleed reader: the book fills the whole window and the chrome floats
    // transparently above it. The old Scaffold slots + content padding resized
    // the navigator on every chrome toggle, making Readium repaginate (text jump).
    Box(modifier = modifier.fillMaxSize()) {
        // Readium content fills the whole area; chrome overlays it.
        NavigatorHost(
            bookId = state.book.id,
            sessionReady = sessionReady,
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(topSafeInsets)
                .windowInsetsPadding(bottomSafeInsets),
        )
        if (!state.chromeVisible && state.prefs.showPageCounter) {
            state.positionText?.let {
                IridiumScrimPill(
                    text = it,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                )
            }
        }
        // Night light: warm scrim above the page, below the chrome. Purely
        // visual — it never resizes or repaginates content, and it carries
        // no touch handling so taps fall through to the book.
        if (state.prefs.nightLight) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(nightLightColor(state.prefs.nightLightIntensity)),
            )
        }
        AnimatedVisibility(
            visible = state.chromeVisible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
                TopAppBar(
                    // Stable top offset: ignoring-visibility insets never change
                    // when the system bars show/hide, so the slide/fade toggle
                    // animation never jumps mid-flight (was: statusBarsPadding).
                    // Opaque surface: no blur means text must not show through.
                    modifier = Modifier.windowInsetsPadding(topSafeInsets),
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
                            Icon(IridiumIcons.Back, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onAction(ReaderAction.OpenToc) }) {
                            Icon(IridiumIcons.List, contentDescription = "Contents")
                        }
                        IconButton(onClick = { onAction(ReaderAction.OpenHighlights) }) {
                            Icon(IridiumIcons.Highlight, contentDescription = "Highlights")
                        }
                        IconButton(onClick = { onAction(ReaderAction.OpenSettings) }) {
                            Icon(IridiumIcons.Settings, contentDescription = "Reading settings")
                        }
                        var menuOpen by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(IridiumIcons.More, contentDescription = "More")
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
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
        }
        AnimatedVisibility(
            visible = state.chromeVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                // Stable bottom offset for the same reason as the top bar.
                // The pills themselves are opaque surfaces; no blur needed.
                modifier = Modifier.windowInsetsPadding(bottomSafeInsets)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                ReaderScrubberIsland(
                    positionIndex = state.positionIndex,
                    positionCount = state.positionCount,
                    direction = ChromeReadingDirection.LEFT_TO_RIGHT,
                    onSeek = { onAction(ReaderAction.SeekTo(it)) },
                    onPrevious = { onAction(ReaderAction.GoBackward()) },
                    onNext = { onAction(ReaderAction.GoForward()) },
                )
                ReaderEpubDock(
                    onFlowCycle = { onAction(ReaderAction.SetFlow(nextReadingFlow(state.prefs.flow))) },
                    onThemeClick = { onAction(ReaderAction.OpenThemeSheet) },
                    onTocClick = { onAction(ReaderAction.OpenToc) },
                    onHighlightsClick = { onAction(ReaderAction.OpenHighlights) },
                    onSettingsClick = { onAction(ReaderAction.OpenSettings) },
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }

    if (state.settingsOpen) {
        ReaderSettingsSheet(prefs = state.prefs, onAction = onAction)
    }
    if (state.themeSheetOpen) {
        ReaderThemeSheet(selected = state.prefs.theme, onAction = onAction)
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
    dictionaryState?.let { dict ->
        // Crisp Pulsar tick the moment a word is selected. This fires from
        // the user's selection gesture (not idle composition); the framework
        // fallback guarantees feedback even where Pulsar can't play.
        LaunchedEffect(dict.word) { haptics(IridiumHaptic.Tick) }
        // Keyed by word so each fresh selection replays the popup's
        // entrance after the lookup grace period.
        key(dict.word) {
            DictionaryPopup(
                state = dict,
                onDismiss = { onAction(ReaderAction.DismissDictionary) },
            )
        }
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


private fun nextReadingFlow(flow: com.iridium.core.model.ReadingFlow) =
    com.iridium.core.model.ReadingFlow.entries[
        (com.iridium.core.model.ReadingFlow.entries.indexOf(flow) + 1) %
            com.iridium.core.model.ReadingFlow.entries.size,
    ]

/**
 * Page background per reader theme (mirrors the settings swatches): system
 * bars tint to this so they read as a seamless frame around the page.
 */
internal fun ColorSchemeChoice.pageBackground(): Color = when (this) {
    ColorSchemeChoice.LIGHT -> Color(0xFFFFFFFF)
    ColorSchemeChoice.SEPIA -> Color(0xFFF5E6C8)
    ColorSchemeChoice.GREY -> Color(0xFF444444)
    ColorSchemeChoice.DARK -> Color(0xFF121212)
    ColorSchemeChoice.BLACK -> Color(0xFF000000)
}

/** Dark status/nav icons on the light page themes, light icons otherwise. */
internal fun ColorSchemeChoice.lightSystemBars(): Boolean =
    this == ColorSchemeChoice.LIGHT || this == ColorSchemeChoice.SEPIA

/** Hosts the [ReaderHostFragment] inside Compose via FragmentContainerView. */
@Composable
private fun NavigatorHost(bookId: String, sessionReady: Boolean, modifier: Modifier = Modifier) {
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

    // The container is always composed so FragmentManager has somewhere to
    // restore into; the host fragment itself is only added once the session
    // is published. Adding it earlier handed Readium a null navigator factory
    // and collided with an in-flight composition on the first open.
    AndroidView(
        factory = { ctx -> FragmentContainerView(ctx).apply { id = containerId } },
        update = {},
        modifier = modifier.fillMaxSize(),
    )

    if (!sessionReady) {
        IridiumLoading(modifier)
        return
    }

    LaunchedEffect(bookId, containerId, sessionReady) {
        val fm = activity.supportFragmentManager
        if (fm.findFragmentByTag(tag) != null || fm.findFragmentById(containerId) != null) {
            return@LaunchedEffect
        }
        val fragment = ReaderHostFragment.newInstance()
        if (fm.isStateSaved) {
            // The activity already saved state (e.g. opening during a
            // restore): commitNow would throw, so allow state loss.
            fm.beginTransaction().add(containerId, fragment, tag).commitAllowingStateLoss()
        } else {
            fm.commitNow { add(containerId, fragment, tag) }
        }
    }

    DisposableEffect(bookId, containerId) {
        onDispose {
            // Leaving the reader: detach the navigator so a stale WebView never
            // lingers behind the library. commitAllowingStateLoss (not
            // commitNow) avoids throwing if the host already saved state.
            val fm = activity.supportFragmentManager
            fm.findFragmentByTag(tag)?.takeIf { it.isAdded }?.let {
                fm.beginTransaction().remove(it).commitAllowingStateLoss()
            }
        }
    }
}
