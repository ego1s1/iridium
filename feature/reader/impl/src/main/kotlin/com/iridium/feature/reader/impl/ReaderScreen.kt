package com.iridium.feature.reader.impl

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import com.iridium.core.designsystem.IridiumEnter
import com.iridium.core.designsystem.IridiumEnterKind
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.iridium.core.designsystem.findActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.iridium.core.designsystem.IridiumEmptyState
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumLoading
import com.iridium.core.designsystem.IridiumScrimPill
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.pageBackgroundArgb
import com.iridium.core.model.isLightScheme
import com.iridium.core.model.resolvedColors
import com.iridium.core.designsystem.LocalNavAnimatedVisibilityScope
import com.iridium.core.designsystem.readerEnter
import com.iridium.core.designsystem.readerExit
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
    val dictionaryState by viewModel.dictionaryUi.collectAsStateWithLifecycle()
    val shownImage by viewModel.shownImage.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptics = rememberIridiumHaptics()

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message ->
            when (message) {
                ReaderMessage.ContentUnavailable -> {
                    haptics(IridiumHaptic.Reject)
                    snackbarHost.showSnackbar(context.getString(R.string.reader_content_unavailable))
                }
                is ReaderMessage.OpenUrl -> {
                    val uri = runCatching { android.net.Uri.parse(message.url) }.getOrNull()
                    val scheme = uri?.scheme?.lowercase()
                    if (uri != null && (scheme == "http" || scheme == "https")) {
                        val result = runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                        if (result.isFailure) {
                            haptics(IridiumHaptic.Reject)
                            snackbarHost.showSnackbar(context.getString(R.string.reader_share_failed))
                        }
                    } else {
                        haptics(IridiumHaptic.Reject)
                        snackbarHost.showSnackbar(context.getString(R.string.reader_link_blocked))
                    }
                }
                ReaderMessage.SelectTextFirst -> {
                    haptics(IridiumHaptic.Reject)
                    snackbarHost.showSnackbar(context.getString(R.string.reader_select_text_first))
                }
                ReaderMessage.HighlightSaved -> {
                    haptics(IridiumHaptic.Confirm)
                    snackbarHost.showSnackbar(context.getString(R.string.reader_highlight_saved))
                }
                is ReaderMessage.ShareExport -> {
                    val shared = shareAnnotations(context, message)
                    if (shared) {
                        haptics(IridiumHaptic.Confirm)
                    } else {
                        haptics(IridiumHaptic.Reject)
                        snackbarHost.showSnackbar(context.getString(R.string.reader_share_failed))
                    }
                }
                ReaderMessage.Pop -> onBackClick()
            }
        }
    }

    when (val state = uiState) {
        ReaderUiState.Loading -> IridiumLoading(modifier)
        ReaderUiState.Gone -> {
            // Gone books pop straight back: nothing to show and nothing
            // to retry.
            LaunchedEffect(Unit) { onBackClick() }
        }
        is ReaderUiState.OpenFailed -> {
            // A visible dead-end, never a void: the old snackbar+autopop
            // composed no host, so the message suspended forever and Back
            // never fired (permanent black screen). The user reads the
            // reason and leaves explicitly.
            val missing = state.fileMissing
            ReaderOpenFailed(
                fileMissing = missing,
                onBackClick = onBackClick,
                modifier = modifier,
            )
        }
        is ReaderUiState.Ready -> ReaderScreen(
            state = state,
            sessionReady = sessionReady,
            dictionaryState = dictionaryState,
            shownImage = shownImage,
            loadImageBytes = viewModel::loadImageBytes,
            onAction = viewModel::onAction,
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
    dictionaryState: DictionaryUiState?,
    shownImage: String?,
    loadImageBytes: suspend (String) -> ByteArray?,
    onAction: (ReaderAction) -> Unit,
    onBackClick: () -> Unit,
    snackbarHost: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var showAddDialog by remember { mutableStateOf(false) }
    val haptics = rememberIridiumHaptics()

    // Book text always stays clear of the status and navigation zones, even
    // in immersive mode: visibility-ignoring insets are stable, so toggling
    // the chrome never resizes the navigator (no repagination).
    val topSafeInsets = WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top)
    val bottomSafeInsets =
        WindowInsets.navigationBarsIgnoringVisibility.only(WindowInsetsSides.Bottom)

    // The navigator host uses the SAME ignoring-visibility insets: when the
    // system bars show/hide with the chrome, aware insets would collapse to
    // zero and resize the WebView, repaginating the book on every tap. A
    // stable container lets the chrome float above static text; page content
    // drawing under the translucent bars is standard edge-to-edge behavior.
    ReaderWindowEffects(
        chromeVisible = state.chromeVisible,
        brightness = state.prefs.brightness,
        keepScreenOn = state.prefs.keepScreenOn,
        theme = state.prefs.theme,
        pageBackgroundArgb = state.prefs.resolvedColors().first,
    )

    // Full-bleed reader: the book fills the whole window and the chrome floats
    // transparently above it. The old Scaffold slots + content padding resized
    // the navigator on every chrome toggle, making Readium repaginate (text jump).
    Box(modifier = modifier.fillMaxSize()) {
        // Readium content fills the whole area; chrome overlays it.
        // The container is stable across chrome toggles: ignoring-visibility
        // insets never change when the system bars show/hide, so the WebView
        // keeps its size and the text never repaginates on tap.
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
            enter = IridiumEnter.enter(IridiumEnterKind.CHROME_TOP),
            exit = IridiumEnter.exit(IridiumEnterKind.CHROME_TOP),
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
                        IconButton(onClick = {
                            haptics(IridiumHaptic.Select)
                            onBackClick()
                        }) {
                            Icon(IridiumIcons.Back, contentDescription = stringResource(R.string.reader_action_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            haptics(IridiumHaptic.Select)
                            onAction(ReaderAction.OpenToc)
                        }) {
                            Icon(IridiumIcons.List, contentDescription = stringResource(R.string.reader_toc_title))
                        }
                        IconButton(onClick = {
                            haptics(IridiumHaptic.Select)
                            onAction(ReaderAction.OpenHighlights)
                        }) {
                            Icon(
                                IridiumIcons.Highlight,
                                contentDescription = stringResource(R.string.reader_action_highlights),
                            )
                        }
                        IconButton(onClick = {
                            haptics(IridiumHaptic.Select)
                            onAction(ReaderAction.OpenSettings)
                        }) {
                            Icon(
                                IridiumIcons.Settings,
                                contentDescription = stringResource(R.string.reader_action_settings),
                            )
                        }
                        var menuOpen by remember { mutableStateOf(false) }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(IridiumIcons.More, contentDescription = stringResource(R.string.reader_action_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reader_action_highlight_selection)) },
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
            enter = IridiumEnter.enter(IridiumEnterKind.CHROME_BOTTOM),
            exit = IridiumEnter.exit(IridiumEnterKind.CHROME_BOTTOM),
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
                    onSeek = { onAction(ReaderAction.SeekTo(it)) },
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
        ReaderThemeSheet(prefs = state.prefs, onAction = onAction)
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
            onExport = { onAction(ReaderAction.ExportAnnotations) },
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
    if (shownImage != null) {
        ReaderImageShowcase(
            src = shownImage,
            loadImageBytes = loadImageBytes,
            onDismiss = { onAction(ReaderAction.DismissImage) },
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

/**
 * Dead-end for books that cannot be opened: the reason plus an explicit way
 * back. Never auto-pops — the user reads the failure first.
 */
@Composable
private fun ReaderOpenFailed(
    fileMissing: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IridiumEmptyState(
        icon = IridiumIcons.MenuBook,
        title = stringResource(R.string.reader_open_failed_title),
        body = stringResource(
            if (fileMissing) {
                R.string.reader_open_failed_body_missing
            } else {
                R.string.reader_open_failed_body_corrupt
            },
        ),
        actionLabel = stringResource(R.string.reader_open_failed_back),
        onAction = onBackClick,
        modifier = modifier,
    )
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
internal fun ColorSchemeChoice.pageBackground(): Color = Color(pageBackgroundArgb())

/** Dark status/nav icons on the light page themes, light icons otherwise. */
internal fun ColorSchemeChoice.lightSystemBars(): Boolean = isLightScheme()

/** Hosts the [ReaderHostFragment] inside Compose via FragmentContainerView. */
@Composable
private fun NavigatorHost(bookId: String, sessionReady: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context.findActivity()
    if (activity == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.reader_unavailable))
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
            // Config change (rotation) disposes effects but the FragmentManager
            // restores the navigator itself into the stable container id —
            // only detach when truly leaving the reader.
            if (activity.isChangingConfigurations || activity.isFinishing) return@onDispose
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

/**
 * Full-screen showcase for a tapped book image (image showcase). Loads the
 * tapped resource's bytes and draws them edge-to-edge; a tap anywhere
 * dismisses back to the book.
 */
@Composable
private fun ReaderImageShowcase(
    src: String,
    loadImageBytes: suspend (String) -> ByteArray?,
    onDismiss: () -> Unit,
) {
    var bitmap by remember(src) { mutableStateOf<ImageBitmap?>(null) }
    val context = LocalContext.current
    LaunchedEffect(src) {
        bitmap = runCatching {
            val bytes = loadImageBytes(src) ?: return@runCatching null
            decodeShowcaseBitmap(bytes, context)?.asImageBitmap()
        }.getOrNull()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onDismiss)
            .testTag("readerImageShowcase"),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp == null) {
            CircularProgressIndicator(color = Color.White)
        } else {
            key(bmp) {
                var entered by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { entered = true }
                AnimatedVisibility(
                    visible = entered,
                    enter = IridiumEnter.enter(IridiumEnterKind.DIALOG_POP),
                    exit = IridiumEnter.exit(IridiumEnterKind.DIALOG_POP),
                    label = "showcaseImageEnter",
                ) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/**
 * Bounded showcase decode: subsamples huge EPUB illustrations down to the
 * display size before allocating pixels. A 4000x6000 plate is ~96MB decoded
 * at full res — enough to OOM the reader — while the screen can only show
 * ~2MP of it anyway.
 */
internal fun decodeShowcaseBitmap(bytes: ByteArray, context: Context): android.graphics.Bitmap? {
    if (bytes.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val metrics = context.resources.displayMetrics
    val screenMax = maxOf(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(1)
    val imageMax = maxOf(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (imageMax / (sample * 2) >= screenMax) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
}

/** Shares an annotation export via Markdown + HTML intents (SAF/share). Returns false when undeliverable. */
private fun shareAnnotations(context: Context, msg: ReaderMessage.ShareExport): Boolean {
    val markdown = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, msg.filename + ".md")
        putExtra(Intent.EXTRA_TEXT, msg.markdown)
    }
    val html = Intent(Intent.ACTION_SEND).apply {
        type = "text/html"
        putExtra(Intent.EXTRA_HTML_TEXT, msg.html)
        putExtra(Intent.EXTRA_SUBJECT, msg.filename + ".html")
    }
    val chooser = Intent.createChooser(
        markdown,
        context.getString(R.string.reader_highlights_export),
    )
    chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(html))
    return runCatching { context.startActivity(chooser) }.isSuccess
}
