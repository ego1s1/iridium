@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.iridium.core.designsystem

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
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
    if (!LocalExpressiveMotionEnabled.current) return this
    val scope = LocalSharedTransitionScope.current ?: return this
    val animatedScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(scope) {
        sharedElement(
            rememberSharedContentState(key = bookCoverSharedKey(bookId)),
            animatedScope,
            // 650ms emphasized glide so the book visibly travels (Mori
            // cover-morph timing on this Compose version's spec lambda).
            { _, _ ->
                tween<Rect>(
                    durationMillis = IridiumMotion.CoverMorphMs,
                    easing = IridiumMotion.EmphasizedDecelerate,
                )
            },
        )
    }
}

/** Standard screen enter/exit: slide + fade, honoring the motion preference. */
fun screenEnter(expressive: Boolean = true): EnterTransition =
    if (!expressive) {
        fadeIn(animationSpec = IridiumMotion.calmFadeSpec())
    } else {
        // Full-width slide-in over fade (Mori push personality): entering
        // content travels the full width while the parent parallaxes out.
        fadeIn(animationSpec = IridiumMotion.screenEnterSpec()) + slideInHorizontally(
            animationSpec = IridiumMotion.screenEnterSpec(),
            initialOffsetX = { it },
        )
    }

fun screenExit(expressive: Boolean = true): ExitTransition =
    if (!expressive) {
        fadeOut(animationSpec = IridiumMotion.calmFadeSpec())
    } else {
        fadeOut(animationSpec = IridiumMotion.screenExitSpec()) + slideOutHorizontally(
            animationSpec = IridiumMotion.screenExitSpec(),
            targetOffsetX = { -it / 4 },
        )
    }

fun screenPopEnter(expressive: Boolean = true): EnterTransition =
    if (!expressive) {
        fadeIn(animationSpec = IridiumMotion.calmFadeSpec())
    } else {
        fadeIn(animationSpec = IridiumMotion.screenEnterSpec()) + slideInHorizontally(
            animationSpec = IridiumMotion.screenEnterSpec(),
            initialOffsetX = { -it / 4 },
        )
    }

fun screenPopExit(expressive: Boolean = true): ExitTransition =
    if (!expressive) {
        fadeOut(animationSpec = IridiumMotion.calmFadeSpec())
    } else {
        // No fade out on expressive pop exit: system predictive back scrubs
        // this transition directly, keeping the surface fully opaque as it
        // pulls away to reveal the parent beneath.
        slideOutHorizontally(
            animationSpec = IridiumMotion.screenExitSpec(),
            targetOffsetX = { it },
        )
    }

/**
 * Wizard-handoff enter: completing onboarding lands on Main with a fade, not
 * a lateral push — a forward completion reads as arrival.
 */
fun wizardEnter(expressive: Boolean = true): EnterTransition =
    fadeIn(
        animationSpec = if (expressive) {
            IridiumMotion.screenEnterSpec()
        } else {
            IridiumMotion.calmFadeSpec()
        },
    )

/** Wizard-handoff exit: the onboarding screen dissolves as Main arrives. */
fun wizardExit(expressive: Boolean = true): ExitTransition =
    fadeOut(
        animationSpec = if (expressive) {
            IridiumMotion.screenExitSpec()
        } else {
            IridiumMotion.calmFadeSpec()
        },
    )

/** Reader opens onto a full-bleed content bed: a fast fade reads as seamless. */
fun readerEnter(): EnterTransition = fadeIn(animationSpec = IridiumMotion.readerEnterSpec())
fun readerExit(): ExitTransition = fadeOut(animationSpec = IridiumMotion.readerExitSpec())
