package com.iridium.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.iridium.core.model.AppColorScheme

/**
 * Static color presets used when dynamic color is unavailable or disabled.
 * Each preset overrides only the tonal roles the app actually consumes;
 * everything else falls back to the Material baseline so components stay
 * legible without hand-tuning 30 roles.
 */
internal object IridiumColors {

    fun scheme(preset: AppColorScheme, dark: Boolean): ColorScheme = when (preset) {
        AppColorScheme.IRIDIUM -> iridium(dark)
        AppColorScheme.OCEAN -> ocean(dark)
        AppColorScheme.FOREST -> forest(dark)
        AppColorScheme.SUNSET -> sunset(dark)
    }

    private fun iridium(dark: Boolean): ColorScheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFFD6C2FF),
            onPrimary = Color(0xFF3B2A66),
            primaryContainer = Color(0xFF523F7E),
            onPrimaryContainer = Color(0xFFEDDCFF),
            secondaryContainer = Color(0xFF4A4458),
            onSecondaryContainer = Color(0xFFE8DEF8),
            tertiaryContainer = Color(0xFF633B48),
            onTertiaryContainer = Color(0xFFFFD8E4),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6445A8),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE6D9FF),
            onPrimaryContainer = Color(0xFF1F0A4D),
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF1D192B),
            tertiaryContainer = Color(0xFFFFD8E4),
            onTertiaryContainer = Color(0xFF31101D),
        )
    }

    private fun ocean(dark: Boolean): ColorScheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF9CCFFF),
            onPrimary = Color(0xFF003353),
            primaryContainer = Color(0xFF004A75),
            onPrimaryContainer = Color(0xFFCEE5FF),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF00639B),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCEE5FF),
            onPrimaryContainer = Color(0xFF001D33),
        )
    }

    private fun forest(dark: Boolean): ColorScheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFFA5D6A7),
            onPrimary = Color(0xFF0B3813),
            primaryContainer = Color(0xFF26512C),
            onPrimaryContainer = Color(0xFFC8E6C9),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2E7D32),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFC8E6C9),
            onPrimaryContainer = Color(0xFF002106),
        )
    }

    private fun sunset(dark: Boolean): ColorScheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFFFFB68A),
            onPrimary = Color(0xFF552100),
            primaryContainer = Color(0xFF7A3200),
            onPrimaryContainer = Color(0xFFFFDCC7),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFB14A00),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDCC7),
            onPrimaryContainer = Color(0xFF3A0E00),
        )
    }

    /** True-black surfaces for AMOLED panels; only dark schemes. */
    fun ColorScheme.amoled(): ColorScheme = copy(
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0C0C0C),
        surfaceContainer = Color(0xFF131313),
        surfaceContainerHigh = Color(0xFF1B1B1B),
        surfaceContainerHighest = Color(0xFF232323),
    )
}
