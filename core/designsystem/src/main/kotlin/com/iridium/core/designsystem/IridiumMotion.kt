package com.iridium.core.designsystem

import android.provider.Settings
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.iridium.core.model.MotionStyle

/**
 * Single source of truth for animation timing. Nothing outside this file
 * should construct a raw `tween`/`spring`: components ask for a semantic
 * spec instead, so the whole app stays on one motion language and honors
 * the user's "calm" preference and system reduce-motion setting.
 *
 * Specs follow M3 Expressive: spatial motion (size/position/shape) uses
 * springs with light bounce; effects motion (color/alpha) uses no-bounce
 * springs; enter/exit use emphasized easing.
 */
object IridiumMotion {

    // Emphasized easing (M3 Expressive curves).
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val EnterScreenMs = 400
    const val ExitScreenMs = 200
    const val ChromeAutoHideMs = 3000L
    const val PageTurnMs = 180
    const val CoverMorphMs = 650
    const val DoubleTapZoomMs = 500
    const val FlingGlideMs = 500
    const val ReaderFadeInMs = 180
    const val ReaderFadeOutMs = 150
    const val TabEnterMs = 450
    const val TabExitMs = 300

    /** Robert Penner ease-out quad: fast start, soft landing for pan flings. */
    val EaseOutQuad: Easing = Easing { fraction -> 1f - (1f - fraction) * (1f - fraction) }

    /** Robert Penner ease-in-out quad: gentle both ends for zoom glides. */
    val EaseInOutQuad: Easing = Easing { fraction ->
        if (fraction < 0.5f) {
            2f * fraction * fraction
        } else {
            -1f + (4f - 2f * fraction) * fraction
        }
    }

    // Spatial: size, position, shape.
    fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
        spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.6f)

    // Effects: color, alpha, elevation.
    fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
        spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioNoBouncy)

    /** Chrome (app bars, toolbars) sliding in and out. */
    fun <T> chromeSpec(): FiniteAnimationSpec<T> = defaultSpatialSpec()

    /** Hero/shared-element morphs; a touch of bounce for delight. */
    fun <T> heroSpec(): FiniteAnimationSpec<T> =
        spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)

    /** Playful expressive spring for hero moments (alias kept for parity). */
    fun <T> heroSpring(): FiniteAnimationSpec<T> = heroSpec()

    /** Accordion expand: spring physics with gentle bounce for sub-controls. */
    fun <T> accordionEnterSpec(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Accordion collapse: snappy physics without bounce. */
    fun <T> accordionExitSpec(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** Accordion expand transition with expressive bounce and fade. */
    fun accordionEnter(): EnterTransition =
        expandVertically(accordionEnterSpec()) + fadeIn(defaultEffectsSpec())

    /** Accordion collapse transition with clean physics and fade. */
    fun accordionExit(): ExitTransition =
        shrinkVertically(accordionExitSpec()) + fadeOut(defaultEffectsSpec())

    /** In-out quad tween for combined zoom state driven as one Animatable. */
    fun <T> zoomStateSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = DoubleTapZoomMs, easing = EaseInOutQuad)

    /** Pan fling glide: fixed-time ease-out quad over the projected target. */
    fun flingSpec(): FiniteAnimationSpec<Offset> =
        tween(durationMillis = FlingGlideMs, easing = EaseOutQuad)

    /** Reader route fades: the fullscreen bed makes slides read as lag. */
    fun readerEnterSpec(): FiniteAnimationSpec<Float> =
        tween(durationMillis = ReaderFadeInMs, easing = EmphasizedDecelerate)

    /** Reader route fades: the fullscreen bed makes slides read as lag. */
    fun readerExitSpec(): FiniteAnimationSpec<Float> =
        tween(durationMillis = ReaderFadeOutMs, easing = EmphasizedAccelerate)

    /**
     * Cover launch morph: the shared element glides a beat slower than
     * screen chrome, so the book visibly travels instead of snapping.
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    fun coverMorphTransform(): BoundsTransform =
        BoundsTransform { _, _ ->
            tween(durationMillis = CoverMorphMs, easing = EmphasizedDecelerate)
        }

    /** Tab travel glide: fixed-time tweens so rapid hops retarget cleanly. */
    fun <T> tabEnterSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = TabEnterMs, easing = EmphasizedDecelerate)

    /** Tab travel exit: quicker than enter so the arrival leads. */
    fun <T> tabExitSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = TabExitMs, easing = EmphasizedAccelerate)

    /** Reader page turns stay crisp and quick. */
    fun <T> pageTurnSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = PageTurnMs, easing = EmphasizedDecelerate)

    fun <T> screenEnterSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = EnterScreenMs, easing = EmphasizedDecelerate)

    fun <T> screenExitSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = ExitScreenMs, easing = EmphasizedAccelerate)

    /** Calm fallback: a plain fade, used when reduce-motion is on. */
    fun <T> calmFadeSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = 200, easing = Emphasized)
}

/**
 * True when spring/expressive motion should be used. False when the user
 * picked Calm, or the system animation scale is disabled.
 */
val LocalExpressiveMotionEnabled = compositionLocalOf { true }

/** System-wide reduce-motion (Settings.Global.ANIMATOR_DURATION_SCALE == 0). */
@Composable
fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(isSystemReduceMotion(context)) }
    DisposableEffect(context) {
        val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = isSystemReduceMotion(context)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return enabled
}

private fun isSystemReduceMotion(context: android.content.Context): Boolean =
    runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }.getOrDefault(false)

/** Resolves the effective motion personality from prefs + system setting. */
fun resolveExpressiveMotionEnabled(style: MotionStyle, systemReduceMotion: Boolean): Boolean =
    style == MotionStyle.EXPRESSIVE && !systemReduceMotion
