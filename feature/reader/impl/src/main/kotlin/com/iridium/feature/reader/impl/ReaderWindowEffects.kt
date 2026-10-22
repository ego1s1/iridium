package com.iridium.feature.reader.impl

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.iridium.core.designsystem.findActivity
import com.iridium.core.model.ColorSchemeChoice

/**
 * All direct window manipulation for the reader, in one place. The screen
 * stays declarative; these effects own the platform side-effects:
 * per-book brightness, immersive bar visibility, seamless bar tinting,
 * and the keep-awake flag.
 */
@Composable
internal fun ReaderWindowEffects(
    chromeVisible: Boolean,
    brightness: Float,
    keepScreenOn: Boolean,
    theme: ColorSchemeChoice,
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

    // Immersive reading: hide system bars with the chrome.
    DisposableEffect(chromeVisible) {
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        if (chromeVisible) {
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
    DisposableEffect(theme) {
        val window = activity?.window
        val controller =
            activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        if (window == null || controller == null) return@DisposableEffect onDispose { }
        val previousStatus = window.statusBarColor
        val previousNav = window.navigationBarColor
        val previousLightStatus = controller.isAppearanceLightStatusBars
        val previousLightNav = controller.isAppearanceLightNavigationBars
        val pageBg = theme.pageBackground()
        window.statusBarColor = pageBg.toArgb()
        window.navigationBarColor = pageBg.toArgb()
        controller.isAppearanceLightStatusBars = theme.lightSystemBars()
        controller.isAppearanceLightNavigationBars = theme.lightSystemBars()
        onDispose {
            window.statusBarColor = previousStatus
            window.navigationBarColor = previousNav
            controller.isAppearanceLightStatusBars = previousLightStatus
            controller.isAppearanceLightNavigationBars = previousLightNav
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
}
