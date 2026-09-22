@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.iridium.core.designsystem

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

/** Shared-element scope, provided by the app's SharedTransitionLayout. */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** Nav animated-visibility scope, provided per NavHost destination. */
val LocalNavAnimatedVisibilityScope =
    compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Stable shared-element key for a book's cover. */
fun bookCoverSharedKey(bookId: String): String = "cover-$bookId"

/**
 * Registers a composable as the shared cover for [bookId], morphing between
 * the library grid, detail hero and reader. Degrades to a plain [Modifier]
 * when no shared scope is available (previews, tests).
 */
@Composable
fun Modifier.sharedCover(bookId: String): Modifier {
    val scope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(scope) {
        sharedElement(
            rememberSharedContentState(key = bookCoverSharedKey(bookId)),
            animatedScope,
            { _, _ -> IridiumMotion.heroSpec<Rect>() },
        )
    }
}

/** Standard screen enter/exit: slide + fade, honoring the motion preference. */
fun screenEnter(): EnterTransition =
    fadeIn(animationSpec = IridiumMotion.screenEnterSpec()) +
        slideInHorizontally(
            animationSpec = IridiumMotion.screenEnterSpec(),
            initialOffsetX = { it / 8 },
        )

fun screenExit(): ExitTransition =
    fadeOut(animationSpec = IridiumMotion.screenExitSpec()) +
        slideOutHorizontally(
            animationSpec = IridiumMotion.screenExitSpec(),
            targetOffsetX = { -it / 8 },
        )

fun screenPopEnter(): EnterTransition =
    fadeIn(animationSpec = IridiumMotion.screenEnterSpec()) +
        slideInHorizontally(
            animationSpec = IridiumMotion.screenEnterSpec(),
            initialOffsetX = { -it / 8 },
        )

fun screenPopExit(): ExitTransition =
    fadeOut(animationSpec = IridiumMotion.screenExitSpec()) +
        slideOutHorizontally(
            animationSpec = IridiumMotion.screenExitSpec(),
            targetOffsetX = { it / 8 },
        )

/** Reader opens onto a full-bleed content bed: a fast fade reads as seamless. */
fun readerEnter(): EnterTransition = fadeIn(animationSpec = IridiumMotion.screenEnterSpec())
fun readerExit(): ExitTransition = fadeOut(animationSpec = IridiumMotion.screenExitSpec())
