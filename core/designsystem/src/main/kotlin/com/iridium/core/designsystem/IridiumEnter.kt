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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

/**
 * Motion-aware entrance/exit pairs. Expressive motion glides on springs;
 * calm motion (or system reduced motion) fades quietly. Read from
 * [LocalExpressiveMotionEnabled] so the settings toggle applies everywhere
 * without threading flags through state.
 */
enum class IridiumEnterKind {
    /** Top chrome bars slide down into place. */
    CHROME_TOP,

    /** Bottom chrome bars slide up into place. */
    CHROME_BOTTOM,

    /** Toolbar rows slide up into place. */
    TOOLBAR,

    /** Search field expands downward. */
    SEARCH,

    /** Resume FAB scales in from its center. */
    FAB,

    /** Full-screen welcome content rises gently. */
    RISE,

    /**
     * Tab/step switches: incoming fades through with a breath of scale while
     * outgoing fades, so destination changes read as designed even when the
     * entering page spends its first frames composing.
     */
    FADE_THROUGH,

    /** Plain fade for content arrivals (grids, detail bodies, counters). */
    FADE,
}

/** Enter/exit transitions for a semantic [IridiumEnterKind]. */
object IridiumEnter {

    /** Entrance for [kind] under the current motion setting. */
    @Composable
    fun enter(kind: IridiumEnterKind): EnterTransition {
        val expressive = LocalExpressiveMotionEnabled.current
        return remember(expressive, kind) {
            if (!expressive) {
                return@remember fadeIn(animationSpec = IridiumMotion.calmFadeSpec())
            }
            when (kind) {
                IridiumEnterKind.CHROME_TOP -> fadeIn(IridiumMotion.defaultEffectsSpec()) +
                    slideInVertically(IridiumMotion.defaultSpatialSpec()) { -it / 2 }
                IridiumEnterKind.CHROME_BOTTOM, IridiumEnterKind.TOOLBAR ->
                    fadeIn(IridiumMotion.defaultEffectsSpec()) +
                        slideInVertically(IridiumMotion.defaultSpatialSpec()) { it / 2 }
                IridiumEnterKind.SEARCH -> fadeIn(IridiumMotion.defaultEffectsSpec()) +
                    expandVertically(IridiumMotion.defaultSpatialSpec())
                IridiumEnterKind.FAB -> fadeIn(IridiumMotion.defaultEffectsSpec()) +
                    scaleIn(animationSpec = IridiumMotion.heroSpring(), initialScale = 0.6f)
                IridiumEnterKind.RISE -> fadeIn(IridiumMotion.defaultEffectsSpec()) +
                    slideInVertically(IridiumMotion.defaultSpatialSpec()) { it / 4 }
                IridiumEnterKind.FADE_THROUGH -> fadeIn(IridiumMotion.defaultEffectsSpec()) +
                    scaleIn(animationSpec = IridiumMotion.defaultSpatialSpec(), initialScale = 0.98f)
                IridiumEnterKind.FADE -> fadeIn(IridiumMotion.defaultEffectsSpec())
            }
        }
    }

    /** Exit matching [enter]: reverse slide, quiet calm fade. */
    @Composable
    fun exit(kind: IridiumEnterKind): ExitTransition {
        val expressive = LocalExpressiveMotionEnabled.current
        return remember(expressive, kind) {
            if (!expressive) {
                return@remember fadeOut(animationSpec = IridiumMotion.calmFadeSpec())
            }
            when (kind) {
                IridiumEnterKind.CHROME_TOP -> fadeOut(IridiumMotion.defaultEffectsSpec()) +
                    slideOutVertically(IridiumMotion.defaultSpatialSpec()) { -it / 2 }
                IridiumEnterKind.CHROME_BOTTOM, IridiumEnterKind.TOOLBAR ->
                    fadeOut(IridiumMotion.defaultEffectsSpec()) +
                        slideOutVertically(IridiumMotion.defaultSpatialSpec()) { it / 2 }
                IridiumEnterKind.SEARCH -> fadeOut(IridiumMotion.defaultEffectsSpec()) +
                    shrinkVertically(IridiumMotion.defaultSpatialSpec())
                IridiumEnterKind.FAB -> fadeOut(IridiumMotion.defaultEffectsSpec()) +
                    // Mirrors the enter spring (same spec, reversed endpoints):
                    // exits shrink back through the arrival scale, never snap.
                    scaleOut(animationSpec = IridiumMotion.heroSpring(), targetScale = 0.6f)
                IridiumEnterKind.RISE -> fadeOut(IridiumMotion.defaultEffectsSpec()) +
                    slideOutVertically(IridiumMotion.defaultSpatialSpec()) { it / 4 }
                IridiumEnterKind.FADE_THROUGH -> fadeOut(IridiumMotion.defaultEffectsSpec())
                IridiumEnterKind.FADE -> fadeOut(IridiumMotion.defaultEffectsSpec())
            }
        }
    }
}

/** Bottom-sheet geometry: rounded top corners only. */
val Shapes.topSheet: RoundedCornerShape
    get() = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
