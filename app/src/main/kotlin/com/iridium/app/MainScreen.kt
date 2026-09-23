package com.iridium.app

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.iridium.core.designsystem.IridiumEnter
import com.iridium.core.designsystem.IridiumEnterKind
import com.iridium.core.designsystem.IridiumMotion
import com.iridium.core.designsystem.LocalExpressiveMotionEnabled
import com.iridium.core.designsystem.LocalNavAnimatedVisibilityScope
import com.iridium.core.model.Book
import com.iridium.feature.history.impl.HistoryTabContent
import com.iridium.feature.library.impl.LibraryTabContent
import com.iridium.feature.onboarding.api.OnboardingRoute
import com.iridium.feature.settings.impl.SettingsTabContent
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

/** Top-level main viewport: Library, History and Settings as tabs. */
@Serializable
object MainRoute

/** Survives process death so the resume button doesn't vanish after a kill. */
private val ResumeTargetSaver: Saver<Book?, Any> = Saver(
    save = { book -> book?.let { listOf(it.id, it.title, it.progress) } },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        (saved as? List<Any>)?.let { parts ->
            Book(
                id = parts[0] as String,
                title = parts[1] as String,
                sourcePath = "",
                progress = (parts[2] as Number).toFloat(),
                sourceDisplayName = "",
            )
        }
    },
)

fun NavController.navigateToMain() {
    navigate(MainRoute) {
        popUpTo(OnboardingRoute) { inclusive = true }
        launchSingleTop = true
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.mainScreen(
    onReadClick: (String) -> Unit,
    onOpenChapter: (bookId: String, href: String) -> Unit,
    onBookLongClick: (String) -> Unit,
    appVersion: String,
) {
    composable<MainRoute> {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            MainScreen(
                onReadClick = onReadClick,
                onOpenChapter = onOpenChapter,
                onBookLongClick = onBookLongClick,
                appVersion = appVersion,
            )
        }
    }
}

/**
 * Main viewport: tabs live under one floating navigator (plus a resume
 * circle), each keeping its own state so a settings visit never loses the
 * grid's scroll position. System back jumps home to the library.
 */
@Composable
internal fun MainScreen(
    onReadClick: (String) -> Unit,
    onOpenChapter: (bookId: String, href: String) -> Unit,
    onBookLongClick: (String) -> Unit,
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(LIBRARY_TAB) }
    val tabStateHolder = rememberSaveableStateHolder()

    val expressiveMotion = LocalExpressiveMotionEnabled.current
    val backPreview = remember { Animatable(0f) }
    PredictiveBackHandler(enabled = selectedTab != LIBRARY_TAB) { progress ->
        try {
            progress.collect { backPreview.snapTo(it.progress) }
            selectedTab = LIBRARY_TAB
            backPreview.snapTo(0f)
        } catch (e: CancellationException) {
            backPreview.animateTo(
                0f,
                animationSpec = if (expressiveMotion) {
                    IridiumMotion.defaultSpatialSpec()
                } else {
                    IridiumMotion.calmFadeSpec()
                },
            )
            throw e
        }
    }

    var resume by rememberSaveable(stateSaver = ResumeTargetSaver) {
        mutableStateOf<Book?>(null)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Top,
        ),
        modifier = modifier,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (!expressiveMotion) {
                        fadeIn(animationSpec = IridiumMotion.calmFadeSpec()) togetherWith
                            fadeOut(animationSpec = IridiumMotion.calmFadeSpec())
                    } else {
                        val forward = targetState > initialState
                        val sign = if (forward) 1 else -1
                        (fadeIn(animationSpec = IridiumMotion.screenEnterSpec()) +
                            slideInHorizontally(animationSpec = IridiumMotion.screenEnterSpec()) {
                                sign * it / 4
                            }) togetherWith
                            (fadeOut(animationSpec = IridiumMotion.screenExitSpec()) +
                                slideOutHorizontally(animationSpec = IridiumMotion.screenExitSpec()) {
                                    -sign * it / 4
                                })
                    }
                },
                label = "mainTabs",
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    val pull = backPreview.value
                    translationX = pull * size.width * 0.08f
                    val settle = 1f - 0.02f * pull
                    scaleX = settle
                    scaleY = settle
                },
            ) { tab ->
                tabStateHolder.SaveableStateProvider(tab) {
                    when (tab) {
                        LIBRARY_TAB -> LibraryTabContent(
                            onReadClick = onReadClick,
                            onBookLongClick = onBookLongClick,
                            onOpenChapter = onOpenChapter,
                            onResumeAvailable = { resume = it },
                        )
                        HISTORY_TAB -> HistoryTabContent(
                            onReadClick = onReadClick,
                            onBookLongClick = onBookLongClick,
                        )
                        SETTINGS_TAB -> SettingsTabContent(appVersion = appVersion)
                        else -> Unit
                    }
                }
            }
            AnimatedVisibility(
                visible = true,
                enter = IridiumEnter.enter(IridiumEnterKind.TOOLBAR),
                exit = IridiumEnter.exit(IridiumEnterKind.TOOLBAR),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
                    )
                    .padding(bottom = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MainNavigator(
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                    )
                    AnimatedVisibility(
                        visible = resume != null,
                        enter = IridiumEnter.enter(IridiumEnterKind.FAB),
                        exit = IridiumEnter.exit(IridiumEnterKind.FAB),
                    ) {
                        resume?.let { target ->
                            ResumeButton(
                                title = target.title,
                                onClick = { onReadClick(target.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val LIBRARY_TAB = 0
private const val HISTORY_TAB = 1
private const val SETTINGS_TAB = 2
