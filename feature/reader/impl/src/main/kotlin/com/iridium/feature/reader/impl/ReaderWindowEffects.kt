package com.iridium.feature.reader.impl

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.iridium.core.designsystem.findActivity
import com.iridium.core.model.ColorSchemeChoice

/**
 * All direct window manipulation for the reader, in one place. The screen
 * stays declarative; these effects own the platform side-effects:
 * per-book brightness, immersive bar visibility, seamless bar tinting,
 * and the keep-awake flag.
 */
/**
 * Grace period after a system-driven bar restore. Predictive-back abort
 * animations finish inside this window, so a deferred re-hide is not undone.
 */
private const val BAR_RESTORE_SETTLE_MS = 220L

@Composable
internal fun ReaderWindowEffects(
    chromeVisible: Boolean,
    brightness: Float,
    keepScreenOn: Boolean,
    theme: ColorSchemeChoice,
    pageBackgroundArgb: Int,
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    // Per-book brightness override (-1 = system).
    DisposableEffect(brightness) {
        val window = activity?.window
        val previous = window?.attributes?.screenBrightness ?: -1f
        window?.let {
            val attrs = it.attributes
            attrs.screenBrightness = brightness
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

    // Immersive reading: the bars fade out with the chrome.
    //
    // Behaviour is BEHAVIOR_DEFAULT on purpose. The transient-swipe behaviour
    // (BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE) turns the first edge swipe into
    // "reveal the bars" instead of performing the gesture, which is why back
    // needed two swipes and the swipe-up-to-home from the pill did nothing.
    // With the default behaviour the bars fade away, the gesture pill
    // disappears, and back/home still respond to the first swipe. The platform
    // animates hide()/show() on Android 11+, so the bars fade rather than
    // blink; a longer custom fade would need WindowInsetsAnimationController.
    DisposableEffect(chromeVisible) {
        val window = activity?.window
        val decor = window?.decorView
        // `decor` is only non-null when `window` is, but the compiler cannot
        // see through the smart cast across the nullable pair.
        val controller = if (window != null && decor != null) {
            WindowCompat.getInsetsController(window, decor)
        } else {
            null
        }
        if (window == null || decor == null || controller == null) {
            return@DisposableEffect onDispose { }
        }
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        if (chromeVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

        // The system brings the bars back on its own in several cases — a
        // predictive-back preview shows the destination with its bars, and
        // cancelling that gesture leaves them visible even though the chrome is
        // still hidden.
        //
        // Hiding synchronously inside the insets dispatch is not enough: the
        // abort animation restores the bars as it settles, so the request is
        // overridden in the same transaction. Posting it lands after that
        // restore, and a second pass after the animation settles covers the
        // remainder. Returning the insets unmodified keeps them flowing to the
        // content below.
        ViewCompat.setOnApplyWindowInsetsListener(decor) { view, insets ->
            if (!chromeVisible && insets.isVisible(WindowInsetsCompat.Type.systemBars())) {
                val bars = WindowInsetsCompat.Type.systemBars()
                view.post { controller.hide(bars) }
                view.postDelayed({ controller.hide(bars) }, BAR_RESTORE_SETTLE_MS)
            }
            insets
        }

        // Leaving the reader must not leave the library without its bars.
        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(decor, null)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Seamless system bars: on API 35+ (edge-to-edge enforced) the system
    // ignores statusBarColor/navigationBarColor, so transparent bars reveal
    // whatever is behind them. Paint the window + decor with the resolved
    // page background (custom themes included) so bars, letterbox and page
    // are one continuous surface instead of mismatched strips.
    DisposableEffect(theme, pageBackgroundArgb) {
        val window = activity?.window
        val controller =
            activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        if (window == null || controller == null) return@DisposableEffect onDispose { }
        val previousStatus = window.statusBarColor
        val previousNav = window.navigationBarColor
        val previousLightStatus = controller.isAppearanceLightStatusBars
        val previousLightNav = controller.isAppearanceLightNavigationBars
        val previousDecorBg = window.decorView.background
        val pageBg = Color(pageBackgroundArgb)
        window.statusBarColor = pageBg.toArgb()
        window.navigationBarColor = pageBg.toArgb()
        window.decorView.setBackgroundColor(pageBg.toArgb())
        controller.isAppearanceLightStatusBars = theme.lightSystemBars()
        controller.isAppearanceLightNavigationBars = theme.lightSystemBars()
        onDispose {
            window.statusBarColor = previousStatus
            window.navigationBarColor = previousNav
            controller.isAppearanceLightStatusBars = previousLightStatus
            controller.isAppearanceLightNavigationBars = previousLightNav
            window.decorView.background = previousDecorBg
        }
    }

    // Keep the screen awake while reading, when the user asked for it.
    DisposableEffect(keepScreenOn) {
        val window = activity?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Prefer the panel's fastest mode while reading so page-turn slides get a
    // frame per vsync on high-refresh displays (120 Hz = 8.3 ms). The system
    // default can pin a window to 60 Hz, which makes the same animation look
    // half as smooth. Restored on leave like every other reader effect.
    DisposableEffect(Unit) {
        val window = activity?.window ?: return@DisposableEffect onDispose { }
        val attrs = window.attributes
        val previous = attrs.preferredRefreshRate
        val maxRefresh = activity.display?.supportedModes
            ?.maxOfOrNull { it.refreshRate }
            ?: 0f
        if (maxRefresh > 0f) {
            attrs.preferredRefreshRate = maxRefresh
            window.attributes = attrs
        }
        onDispose {
            val restore = window.attributes
            restore.preferredRefreshRate = previous
            window.attributes = restore
        }
    }
}
