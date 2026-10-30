package com.iridium.feature.reader.impl

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.iridium.core.designsystem.IridiumEmptyState
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumLoading
import com.iridium.core.designsystem.LocalNavAnimatedVisibilityScope
import com.iridium.core.designsystem.findActivity
import com.iridium.core.designsystem.readerEnter
import com.iridium.core.designsystem.readerExit
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.model.resolvedColors
import com.iridium.feature.reader.api.ReaderRoute
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

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
    val selectedText by viewModel.selectedText.collectAsStateWithLifecycle()
    val shownImage by viewModel.shownImage.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptics = rememberIridiumHaptics()

    // Tap-time turn pulses drive the page slide (see ReaderScreen).
    var turnPulse by remember { mutableStateOf(PageTurnPulse.Idle) }
    LaunchedEffect(Unit) {
        viewModel.pageTurns.collect { direction ->
            turnPulse = PageTurnPulse(turnPulse.id + 1, direction)
        }
    }

    // Selection colours come from the app's palette, so the highlight matches
    // the dynamic theme instead of the toolkit's fixed pale blue. The pair is
    // guaranteed to contrast because it is a container/on-container pair.
    val selectionBackground = MaterialTheme.colorScheme.primaryContainer
    val selectionForeground = MaterialTheme.colorScheme.onPrimaryContainer
    LaunchedEffect(selectionBackground, selectionForeground) {
        viewModel.setSelectionColors(
            selectionBackground.toArgb(),
            selectionForeground.toArgb(),
        )
    }

    // Crisp buzzy tick the moment text becomes selected, which is now the only
    // immediate feedback: the definition waits for the Dictionary action.
    LaunchedEffect(selectedText) {
        if (selectedText != null) haptics(IridiumHaptic.TextSelect)
    }

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
            turnPulse = turnPulse,
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
    turnPulse: PageTurnPulse,
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

    // Full-page cross-slide page turn: the outgoing page and the incoming page
    // move together one screen width — leftward for a forward turn, rightward
    // back. The incoming page is the live navigator, started fully off-screen
    // so the toolkit's instant swap is never seen; the outgoing page is a
    // snapshot taken while the view was at rest (paged layout snaps scroll
    // offsets to page boundaries, so the document itself cannot be animated).
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val slideShift = remember { Animatable(0f) }
    var slideDir by remember { mutableStateOf(1f) }
    var outgoingPage by remember { mutableStateOf<ImageBitmap?>(null) }
    var navigatorView by remember { mutableStateOf<View?>(null) }
    var sliding by remember { mutableStateOf(false) }
    // Paged only, so the page-turn slide is always active.
    val slideEnabled = true
    val pageFillArgb = state.prefs.resolvedColors().first
    val chromeTopPx = WindowInsets.statusBarsIgnoringVisibility.getTop(density).toInt() +
        with(density) { ChromeTopStrip.toPx() }.toInt()
    val chromeBottomPx = WindowInsets.navigationBarsIgnoringVisibility.getBottom(density).toInt() +
        with(density) { ChromeBottomStrip.toPx() }.toInt()

    suspend fun refreshSnapshot() {
        // Never capture mid-slide (the view is displaced) or in scroll flow.
        if (!slideEnabled || sliding) return
        outgoingPage = capturePage(
            activity = activity,
            view = navigatorView,
            blankTopPx = chromeTopPx,
            blankBottomPx = chromeBottomPx,
            fillArgb = pageFillArgb,
        )?.asImageBitmap()
    }

    LaunchedEffect(turnPulse.id) {
        if (!slideEnabled || turnPulse.id == 0L) return@LaunchedEffect
        sliding = true
        // A turn landing mid-slide has no valid snapshot (the view is
        // displaced), so it slides the incoming page alone: still reads as a
        // turn, and can never tear or stack.
        if (slideShift.isRunning) outgoingPage = null
        slideDir = if (turnPulse.direction >= 0) 1f else -1f
        try {
            slideShift.snapTo(slideDir * screenWidthPx)
            slideShift.animateTo(
                0f,
                // Ease-out tween on the frame clock: every vsync (16.7 ms at
                // 60 Hz, 8.3 ms at 120 Hz) produces one layer offset and no
                // recomposition, since the offsets are read inside
                // graphicsLayer blocks.
                tween(durationMillis = PageTurnDurationMs, easing = EaseOut),
            )
        } finally {
            // Rapid taps cancel this effect mid-glide: the page must never be
            // left displaced and the snapshot layer must never be left behind.
            withContext(NonCancellable) { slideShift.snapTo(0f) }
            outgoingPage = null
            sliding = false
        }
        // Re-arm for the next isolated turn.
        refreshSnapshot()
    }

    LaunchedEffect(sessionReady, navigatorView, slideEnabled, state.prefs, state.contentReady) {
        if (sessionReady && slideEnabled && navigatorView != null && state.contentReady) {
            // Let the settled frame paint before capturing it.
            kotlinx.coroutines.delay(PageSnapshotSettleMs)
            refreshSnapshot()
        }
    }

    // The bottom chrome stays clear of the navigation zone even in immersive
    // mode: visibility-ignoring insets are stable, so toggling the chrome
    // never resizes the navigator (no repagination). The top bar needs no
    // equivalent — it is full-bleed and pads its own content itself.
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
    // The root is painted the resolved page color so transparent system bars,
    // letterboxing and overscroll all match the book instead of leaking the
    // dark app surface as mismatched strips.
    val pageBg = Color(state.prefs.resolvedColors().first)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg),
    ) {
        // Readium content fills the entire window with zero reserved insets:
        // chrome (top bar, scrubber) is a pure overlay and may cover text.
        // No insets are applied here so toggling chrome can never resize the
        // WebView or repaginate the book; page margins come from the book's
        // own CSS/Readium preferences, not the container.
        NavigatorHost(
            bookId = state.book.id,
            sessionReady = sessionReady,
            onViewReady = { navigatorView = it },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = slideShift.value },
        )
        // The outgoing page. The two layers never overlap on screen (one
        // exits as the other enters), so a single snapshot beside the live
        // page reproduces the cross-slide.
        outgoingPage?.let { snapshot ->
            Image(
                bitmap = snapshot,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = slideShift.value - slideDir * screenWidthPx },
            )
        }
        // Until the navigator reports its first position the WebView has not
        // painted yet, which used to show as a blank page after the spinner
        // vanished. Cover that gap with the page colour and the loader so the
        // transition is spinner -> text, with nothing in between.
        //
        // contentReady is driven by the navigator's own onPageLoaded callback,
        // so this is a real paint signal rather than a guessed delay.
        if (!state.contentReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageBg)
                    .testTag(ReaderChromeTestTags.PageLoading),
                contentAlignment = Alignment.Center,
            ) {
                IridiumLoading()
            }
        }
        // Layer 1: night-light tint. Visual only, taps fall through to the book.
        // (The position counter lives only in the chrome scrubber island:
        // nothing overlays the page while chrome is hidden.)
        // Pure-black pages skip the scrim: any translucent warm overlay would
        // lift #000000 (OLED pixels on). Text warming rides the injected CSS
        // instead (see applyThemeOverride), so black stays black.
        if (state.prefs.nightLight &&
            !ReaderThemeOverride.isPureBlack(state.prefs.resolvedColors().first)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(nightLightColor(state.prefs.nightLightIntensity)),
            )
        }
        // Layer 3: the chrome — one overlay, top bar + bottom scrubber, both
        // sliding/fading together. Never affects layout: the navigator behind
        // it is full-window with zero insets, so text is static on every tap.
        ReaderChromeOverlay(
            visible = state.chromeVisible,
            topBar = {
                ReaderTopBar(
                    title = state.book.title,
                    author = state.book.author,
                    onBackClick = onBackClick,
                    onAction = onAction,
                    onAddHighlight = { showAddDialog = true },
                    // Full-bleed to the top: the bar's surface paints behind
                    // the status bar/cutout so the chrome reads as one piece,
                    // and the bar's own inset pads its content. Adding outer
                    // inset padding here left a tall empty band above it.
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
            bottomBar = {
                ReaderScrubberIsland(
                    positionIndex = state.positionIndex,
                    positionCount = state.positionCount,
                    chapterTitle = state.chapterTitle,
                    onSeek = { onAction(ReaderAction.ScrubFinished(it)) },
                    onScrubbingChanged = { scrubbing ->
                        if (scrubbing) onAction(ReaderAction.ScrubStarted)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(bottomSafeInsets)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            },
        )
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(bottomSafeInsets)
                .padding(16.dp),
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
