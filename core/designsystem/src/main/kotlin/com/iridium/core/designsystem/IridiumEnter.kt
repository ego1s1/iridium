package com.iridium.core.designsystem

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Semantic entrance kinds so chrome animates consistently app-wide. */
enum class IridiumEnterKind { CHROME_TOP, CHROME_BOTTOM, TOOLBAR, FAB, RISE, FADE, FADE_THROUGH }

/** Enter/exit transitions for a semantic [IridiumEnterKind]. */
object IridiumEnter {

    fun enter(kind: IridiumEnterKind): EnterTransition = when (kind) {
        IridiumEnterKind.CHROME_TOP ->
            slideInVertically(animationSpec = IridiumMotion.chromeSpec()) { -it } +
                fadeIn(animationSpec = IridiumMotion.defaultEffectsSpec())
        IridiumEnterKind.CHROME_BOTTOM, IridiumEnterKind.TOOLBAR ->
            slideInVertically(animationSpec = IridiumMotion.chromeSpec()) { it } +
                fadeIn(animationSpec = IridiumMotion.defaultEffectsSpec())
        IridiumEnterKind.FAB ->
            scaleIn(animationSpec = IridiumMotion.heroSpec(), initialScale = 0.8f) +
                fadeIn(animationSpec = IridiumMotion.defaultEffectsSpec())
        IridiumEnterKind.RISE ->
            expandVertically(animationSpec = IridiumMotion.defaultSpatialSpec()) +
                fadeIn(animationSpec = IridiumMotion.defaultEffectsSpec())
        IridiumEnterKind.FADE, IridiumEnterKind.FADE_THROUGH ->
            fadeIn(animationSpec = IridiumMotion.defaultEffectsSpec())
    }

    fun exit(kind: IridiumEnterKind): ExitTransition = when (kind) {
        IridiumEnterKind.CHROME_TOP ->
            slideOutVertically(animationSpec = IridiumMotion.chromeSpec()) { -it } +
                fadeOut(animationSpec = IridiumMotion.defaultEffectsSpec())
        IridiumEnterKind.CHROME_BOTTOM, IridiumEnterKind.TOOLBAR ->
            slideOutVertically(animationSpec = IridiumMotion.chromeSpec()) { it } +
                fadeOut(animationSpec = IridiumMotion.defaultEffectsSpec())
        IridiumEnterKind.FAB ->
            scaleOut(animationSpec = IridiumMotion.defaultSpatialSpec(), targetScale = 0.8f) +
                fadeOut(animationSpec = IridiumMotion.defaultEffectsSpec())
        IridiumEnterKind.RISE ->
            shrinkVertically(animationSpec = IridiumMotion.defaultSpatialSpec()) +
                fadeOut(animationSpec = IridiumMotion.defaultEffectsSpec())
        IridiumEnterKind.FADE, IridiumEnterKind.FADE_THROUGH ->
            fadeOut(animationSpec = IridiumMotion.defaultEffectsSpec())
    }
}

/** Bottom-sheet geometry: rounded top corners only. */
val Shapes.topSheet: RoundedCornerShape
    get() = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
