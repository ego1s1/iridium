package com.iridium.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.iridium.core.designsystem.IridiumColors.amoled
import com.iridium.core.model.AppColorScheme

/** True-black mode active: components flatten to plain surface. */
val LocalAmoled = staticCompositionLocalOf { false }

/**
 * App theme. Wraps [MaterialExpressiveTheme] so every Material component
 * gets the expressive motion scheme, and provides [LocalExpressiveMotionEnabled]
 * so our own animations honor the Calm preference.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IridiumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    colorScheme: AppColorScheme = AppColorScheme.IRIDIUM,
    amoled: Boolean = false,
    expressiveMotion: Boolean = true,
    hapticsEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val base: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> IridiumColors.scheme(colorScheme, darkTheme)
    }
    val scheme = if (amoled && darkTheme) base.amoled() else base

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = if (expressiveMotion) {
            MotionScheme.expressive()
        } else {
            MotionScheme.standard()
        },
        shapes = IridiumShapes,
        typography = IridiumTypography,
    ) {
        CompositionLocalProvider(
            LocalAppFonts provides appFonts(darkTheme),
            LocalAmoled provides (amoled && darkTheme),
            LocalHapticsEnabled provides hapticsEnabled,
            LocalExpressiveMotionEnabled provides expressiveMotion,
            content = content,
        )
    }
}
