package com.iridium.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.iridium.core.model.AppColorScheme

// Static fallback palette (used when dynamic color is off or unavailable on API < 31).
// Seeds chosen for a deep-ink gallery feel; components must still use colorScheme roles.

internal val IridiumSeedLight = Color(0xFFD6C2FF)
internal val IridiumOnSeedDark = Color(0xFF2E1B5E)
internal val IridiumSeedContainerDark = Color(0xFF44307E)
internal val IridiumOnSeedContainerDark = Color(0xFFE8DEFF)
internal val IridiumSecondaryDark = Color(0xFFC9C0D9)
internal val IridiumTertiaryDark = Color(0xFFEFB8C8)

internal val IridiumSeedDark = Color(0xFF6445A8)
internal val IridiumOnSeedLight = Color(0xFFFFFFFF)
internal val IridiumSeedContainerLight = Color(0xFFE6D9FF)
internal val IridiumOnSeedContainerLight = Color(0xFF1E1042)
internal val IridiumSecondaryLight = Color(0xFF585B93)
internal val IridiumTertiaryLight = Color(0xFF8F4958)

// Preset schemes (primary / secondary / tertiary accents, light + dark).
// Hand-tuned M3-style tonal sets for when dynamic (wallpaper) color is off;
// presetScheme derives every container/error/surface role from these accents.
internal object OceanColors {
    val PrimaryLight = Color(0xFF00696B)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFF9CF0F0)
    val OnContainerLight = Color(0xFF002020)
    val SecondaryLight = Color(0xFF4A6363)
    val TertiaryLight = Color(0xFF4D5F7C)
    val PrimaryDark = Color(0xFF80D5D6)
    val OnPrimaryDark = Color(0xFF003737)
    val ContainerDark = Color(0xFF004F50)
    val OnContainerDark = Color(0xFF9CF0F0)
    val SecondaryDark = Color(0xFFB0CCCB)
    val TertiaryDark = Color(0xFFB9C6E4)
}

internal object ForestColors {
    val PrimaryLight = Color(0xFF406836)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFFC0F0BE)
    val OnContainerLight = Color(0xFF0A2007)
    val SecondaryLight = Color(0xFF54634D)
    val TertiaryLight = Color(0xFF6B6B22)
    val PrimaryDark = Color(0xFFA4D39A)
    val OnPrimaryDark = Color(0xFF12370F)
    val ContainerDark = Color(0xFF294E22)
    val OnContainerDark = Color(0xFFC0F0BE)
    val SecondaryDark = Color(0xFFB9CCB2)
    val TertiaryDark = Color(0xFFD4C489)
}

internal object SunsetColors {
    val PrimaryLight = Color(0xFF8C4E00)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFFFFDDBA)
    val OnContainerLight = Color(0xFF2E1500)
    val SecondaryLight = Color(0xFF6F5B40)
    val TertiaryLight = Color(0xFF7E525E)
    val PrimaryDark = Color(0xFFFFB870)
    val OnPrimaryDark = Color(0xFF4A2800)
    val ContainerDark = Color(0xFF6B3D00)
    val OnContainerDark = Color(0xFFFFDDBA)
    val SecondaryDark = Color(0xFFD8C5A0)
    val TertiaryDark = Color(0xFFEFB8C8)
}

/** Catppuccin Mocha (dark) / Latte (light) accents. */
internal object CatppuccinColors {
    val PrimaryLight = Color(0xFF8839EF)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFFE9DCFF)
    val OnContainerLight = Color(0xFF241B3D)
    val SecondaryLight = Color(0xFF5C5F77)
    val TertiaryLight = Color(0xFF179299)
    val PrimaryDark = Color(0xFFCBA6F7)
    val OnPrimaryDark = Color(0xFF3A2A5E)
    val ContainerDark = Color(0xFF4A3A75)
    val OnContainerDark = Color(0xFFE9DEF8)
    val SecondaryDark = Color(0xFFBAC2DE)
    val TertiaryDark = Color(0xFF94E2D5)
}

/** Nord frost accents on polar-night surfaces. */
internal object NordColors {
    val PrimaryLight = Color(0xFF5E81AC)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFFD8E4F0)
    val OnContainerLight = Color(0xFF1A2B3D)
    val SecondaryLight = Color(0xFF4C566A)
    val TertiaryLight = Color(0xFF4A7D78)
    val PrimaryDark = Color(0xFF88C0D0)
    val OnPrimaryDark = Color(0xFF12242C)
    val ContainerDark = Color(0xFF2A4A56)
    val OnContainerDark = Color(0xFFD8EEF3)
    val SecondaryDark = Color(0xFF81A7C9)
    val TertiaryDark = Color(0xFF8FBCBB)
}

/** Gruvbox warm retro accents. */
internal object GruvboxColors {
    val PrimaryLight = Color(0xFFB57614)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFFF2DCAE)
    val OnContainerLight = Color(0xFF3D2A00)
    val SecondaryLight = Color(0xFF076678)
    val TertiaryLight = Color(0xFFAF3A03)
    val PrimaryDark = Color(0xFFFABD2F)
    val OnPrimaryDark = Color(0xFF3A2700)
    val ContainerDark = Color(0xFF6E5200)
    val OnContainerDark = Color(0xFFFFE7B3)
    val SecondaryDark = Color(0xFF83A598)
    val TertiaryDark = Color(0xFFFE8019)
}

/** Dracula purple/pink/green accents. */
internal object DraculaColors {
    val PrimaryLight = Color(0xFF6440A5)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFFE4D7FB)
    val OnContainerLight = Color(0xFF241640)
    val SecondaryLight = Color(0xFFA24D8F)
    val TertiaryLight = Color(0xFF1F7A3D)
    val PrimaryDark = Color(0xFFBD93F9)
    val OnPrimaryDark = Color(0xFF2E1B4D)
    val ContainerDark = Color(0xFF4A3573)
    val OnContainerDark = Color(0xFFE9DCFF)
    val SecondaryDark = Color(0xFFFF79C6)
    val TertiaryDark = Color(0xFF50FA7B)
}

/** Tokyo Night neon accents. */
internal object TokyoNightColors {
    val PrimaryLight = Color(0xFF34548A)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFFD9E4F5)
    val OnContainerLight = Color(0xFF16294A)
    val SecondaryLight = Color(0xFF5A6C8D)
    val TertiaryLight = Color(0xFF0F7B9C)
    val PrimaryDark = Color(0xFFBB9AF7)
    val OnPrimaryDark = Color(0xFF241B4D)
    val ContainerDark = Color(0xFF443A75)
    val OnContainerDark = Color(0xFFE6DEFF)
    val SecondaryDark = Color(0xFF7AA2F7)
    val TertiaryDark = Color(0xFF7DCFFF)
}

/** Everforest muted green accents. */
internal object EverforestColors {
    val PrimaryLight = Color(0xFF4C7A5D)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFFD5E8D5)
    val OnContainerLight = Color(0xFF14291C)
    val SecondaryLight = Color(0xFF5E6F5B)
    val TertiaryLight = Color(0xFF8C5A72)
    val PrimaryDark = Color(0xFFA7C080)
    val OnPrimaryDark = Color(0xFF1E2A12)
    val ContainerDark = Color(0xFF3A4A2A)
    val OnContainerDark = Color(0xFFDFECCD)
    val SecondaryDark = Color(0xFF7FBBB3)
    val TertiaryDark = Color(0xFFD699B6)
}

/** Pure grayscale accents, zero hue. */
internal object MonochromeColors {
    val PrimaryLight = Color(0xFF212121)
    val OnPrimaryLight = Color(0xFFFFFFFF)
    val ContainerLight = Color(0xFFE2E2E2)
    val OnContainerLight = Color(0xFF141414)
    val SecondaryLight = Color(0xFF5A5A5A)
    val TertiaryLight = Color(0xFF8A8A8A)
    val PrimaryDark = Color(0xFFE0E0E0)
    val OnPrimaryDark = Color(0xFF1A1A1A)
    val ContainerDark = Color(0xFF333333)
    val OnContainerDark = Color(0xFFEDEDED)
    val SecondaryDark = Color(0xFFB0B0B0)
    val TertiaryDark = Color(0xFF8A8A8A)
}

/** Swatch color identifying a scheme choice in pickers (light primary). */
fun AppColorScheme.previewColor(): Color = when (this) {
    AppColorScheme.IRIDIUM -> IridiumSeedDark
    AppColorScheme.OCEAN -> OceanColors.PrimaryLight
    AppColorScheme.FOREST -> ForestColors.PrimaryLight
    AppColorScheme.SUNSET -> SunsetColors.PrimaryLight
    AppColorScheme.CATPPUCCIN -> CatppuccinColors.PrimaryLight
    AppColorScheme.NORD -> NordColors.PrimaryLight
    AppColorScheme.GRUVBOX -> GruvboxColors.PrimaryLight
    AppColorScheme.DRACULA -> DraculaColors.PrimaryLight
    AppColorScheme.TOKYO_NIGHT -> TokyoNightColors.PrimaryLight
    AppColorScheme.EVERFOREST -> EverforestColors.PrimaryLight
    AppColorScheme.MONOCHROME -> MonochromeColors.PrimaryLight
}

// Tonal derivation for scheme roles M3 baseline would otherwise supply.
// Preset schemes only hand-tune accents; every container/error role below is
// derived from those accents so non-dynamic presets never render baseline
// containers.

/** M3 baseline error ramp (scheme-independent red). */
internal val ErrorLight = Color(0xFFBA1A1A)
internal val OnErrorLight = Color(0xFFFFFFFF)
internal val ErrorContainerLight = Color(0xFFFFDAD6)
internal val OnErrorContainerLight = Color(0xFF410002)
internal val ErrorDark = Color(0xFFFFB4AB)
internal val OnErrorDark = Color(0xFF690005)
internal val ErrorContainerDark = Color(0xFF93000A)
internal val OnErrorContainerDark = Color(0xFFFFDAD6)

/** M3 baseline dark surface ramp (neutral violet-gray) containers whisper into. */
private val DarkLowest = Color(0xFF0F0D13)
private val DarkLow = Color(0xFF1D1B20)
private val DarkContainer = Color(0xFF211F26)
private val DarkHigh = Color(0xFF2B2930)
private val DarkHighest = Color(0xFF36343B)

/** M3 baseline light surface ramp (neutral light-gray) containers whisper into. */
private val LightLowest = Color(0xFFFFFFFF)
private val LightLow = Color(0xFFF7F2FA)
private val LightContainer = Color(0xFFF3EDF7)
private val LightHigh = Color(0xFFECE6F0)
private val LightHighest = Color(0xFFE6E0E9)

/** Source-over blend of [foreground] onto this color. */
internal fun Color.blend(foreground: Color, alpha: Float): Color {
    val a = alpha.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - a) + foreground.red * a,
        green = green * (1f - a) + foreground.green * a,
        blue = blue * (1f - a) + foreground.blue * a,
        alpha = 1f,
    )
}

/** Darkens toward black by [fraction] (AMOLED depth without hue loss). */
internal fun Color.darkened(fraction: Float): Color {
    val f = (1f - fraction.coerceIn(0f, 1f))
    return copy(red = red * f, green = green * f, blue = blue * f)
}

/** Returns this hue/saturation at absolute [lightness] (M3-style tone targeting). */
internal fun Color.atLightness(lightness: Float): Color {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val l = lightness.coerceIn(0f, 1f)
    if (max == min) return Color(l, l, l, alpha)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        red -> ((green - blue) / d + (if (green < blue) 6f else 0f)) / 6f
        green -> ((blue - red) / d + 2f) / 6f
        else -> ((red - green) / d + 4f) / 6f
    }
    fun hue2rgb(p: Float, q: Float, t: Float): Float {
        var tt = t
        if (tt < 0f) tt += 1f
        if (tt > 1f) tt -= 1f
        return when {
            tt < 1f / 6f -> p + (q - p) * 6f * tt
            tt < 1f / 2f -> q
            tt < 2f / 3f -> p + (q - p) * (2f / 3f - tt) * 6f
            else -> p
        }
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    return Color(hue2rgb(p, q, h + 1f / 3f), hue2rgb(p, q, h), hue2rgb(p, q, h - 1f / 3f), alpha)
}

/** Container role pair (container + on-container) derived from an accent. */
internal data class ContainerPair(val container: Color, val onContainer: Color)

/**
 * Tonal container derived from [accent]: light schemes land near tone 94
 * (dark on-container), dark schemes near tone 28 (light on-container).
 */
internal fun containerFor(accent: Color, darkTheme: Boolean): ContainerPair =
    if (darkTheme) {
        ContainerPair(
            container = accent.atLightness(0.28f),
            onContainer = accent.atLightness(0.93f),
        )
    } else {
        ContainerPair(
            container = accent.atLightness(0.94f),
            onContainer = accent.atLightness(0.10f),
        )
    }

/** Surface ramp carrying a whisper of [seed] over the neutral baseline. */
internal data class SurfaceRamp(
    val lowest: Color,
    val low: Color,
    val container: Color,
    val high: Color,
    val highest: Color,
)

internal fun darkSurfaceRamp(seed: Color, whisper: Float = 0.12f): SurfaceRamp = SurfaceRamp(
    lowest = DarkLowest.blend(seed, whisper),
    low = DarkLow.blend(seed, whisper),
    container = DarkContainer.blend(seed, whisper),
    high = DarkHigh.blend(seed, whisper),
    highest = DarkHighest.blend(seed, whisper),
)

internal fun lightSurfaceRamp(seed: Color, whisper: Float = 0.12f): SurfaceRamp = SurfaceRamp(
    lowest = LightLowest.blend(seed, whisper),
    low = LightLow.blend(seed, whisper),
    container = LightContainer.blend(seed, whisper),
    high = LightHigh.blend(seed, whisper),
    highest = LightHighest.blend(seed, whisper),
)

/** Full dark preset from accents: containers + error ramp derived, never baseline. */
private fun presetDarkScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    tertiary: Color,
): ColorScheme {
    val ramp = darkSurfaceRamp(primary)
    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        secondaryContainer = containerFor(secondary, darkTheme = true).container,
        onSecondaryContainer = containerFor(secondary, darkTheme = true).onContainer,
        tertiary = tertiary,
        tertiaryContainer = containerFor(tertiary, darkTheme = true).container,
        onTertiaryContainer = containerFor(tertiary, darkTheme = true).onContainer,
        error = ErrorDark,
        onError = OnErrorDark,
        errorContainer = ErrorContainerDark,
        onErrorContainer = OnErrorContainerDark,
        surfaceContainerLowest = ramp.lowest,
        surfaceContainerLow = ramp.low,
        surfaceContainer = ramp.container,
        surfaceContainerHigh = ramp.high,
        surfaceContainerHighest = ramp.highest,
    )
}

/** Full light preset from accents: containers + error ramp derived, never baseline. */
private fun presetLightScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    tertiary: Color,
): ColorScheme {
    val ramp = lightSurfaceRamp(primary)
    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        secondaryContainer = containerFor(secondary, darkTheme = false).container,
        onSecondaryContainer = containerFor(secondary, darkTheme = false).onContainer,
        tertiary = tertiary,
        tertiaryContainer = containerFor(tertiary, darkTheme = false).container,
        onTertiaryContainer = containerFor(tertiary, darkTheme = false).onContainer,
        error = ErrorLight,
        onError = OnErrorLight,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight,
        surfaceContainerLowest = ramp.lowest,
        surfaceContainerLow = ramp.low,
        surfaceContainer = ramp.container,
        surfaceContainerHigh = ramp.high,
        surfaceContainerHighest = ramp.highest,
    )
}

private fun presetScheme(choice: AppColorScheme, darkTheme: Boolean): ColorScheme =
    if (darkTheme) {
        presetDarkChoice(choice)
    } else {
        presetLightChoice(choice)
    }

private fun presetDarkChoice(choice: AppColorScheme): ColorScheme {
    return when (choice) {
            AppColorScheme.IRIDIUM -> presetDarkScheme(
                primary = IridiumSeedLight,
                onPrimary = IridiumOnSeedDark,
                primaryContainer = IridiumSeedContainerDark,
                onPrimaryContainer = IridiumOnSeedContainerDark,
                secondary = IridiumSecondaryDark,
                tertiary = IridiumTertiaryDark,
            )
            AppColorScheme.OCEAN -> presetDarkScheme(
                primary = OceanColors.PrimaryDark,
                onPrimary = OceanColors.OnPrimaryDark,
                primaryContainer = OceanColors.ContainerDark,
                onPrimaryContainer = OceanColors.OnContainerDark,
                secondary = OceanColors.SecondaryDark,
                tertiary = OceanColors.TertiaryDark,
            )
            AppColorScheme.FOREST -> presetDarkScheme(
                primary = ForestColors.PrimaryDark,
                onPrimary = ForestColors.OnPrimaryDark,
                primaryContainer = ForestColors.ContainerDark,
                onPrimaryContainer = ForestColors.OnContainerDark,
                secondary = ForestColors.SecondaryDark,
                tertiary = ForestColors.TertiaryDark,
            )
            AppColorScheme.SUNSET -> presetDarkScheme(
                primary = SunsetColors.PrimaryDark,
                onPrimary = SunsetColors.OnPrimaryDark,
                primaryContainer = SunsetColors.ContainerDark,
                onPrimaryContainer = SunsetColors.OnContainerDark,
                secondary = SunsetColors.SecondaryDark,
                tertiary = SunsetColors.TertiaryDark,
            )
            AppColorScheme.CATPPUCCIN -> presetDarkScheme(
                primary = CatppuccinColors.PrimaryDark,
                onPrimary = CatppuccinColors.OnPrimaryDark,
                primaryContainer = CatppuccinColors.ContainerDark,
                onPrimaryContainer = CatppuccinColors.OnContainerDark,
                secondary = CatppuccinColors.SecondaryDark,
                tertiary = CatppuccinColors.TertiaryDark,
            )
            AppColorScheme.NORD -> presetDarkScheme(
                primary = NordColors.PrimaryDark,
                onPrimary = NordColors.OnPrimaryDark,
                primaryContainer = NordColors.ContainerDark,
                onPrimaryContainer = NordColors.OnContainerDark,
                secondary = NordColors.SecondaryDark,
                tertiary = NordColors.TertiaryDark,
            )
            AppColorScheme.GRUVBOX -> presetDarkScheme(
                primary = GruvboxColors.PrimaryDark,
                onPrimary = GruvboxColors.OnPrimaryDark,
                primaryContainer = GruvboxColors.ContainerDark,
                onPrimaryContainer = GruvboxColors.OnContainerDark,
                secondary = GruvboxColors.SecondaryDark,
                tertiary = GruvboxColors.TertiaryDark,
            )
            AppColorScheme.DRACULA -> presetDarkScheme(
                primary = DraculaColors.PrimaryDark,
                onPrimary = DraculaColors.OnPrimaryDark,
                primaryContainer = DraculaColors.ContainerDark,
                onPrimaryContainer = DraculaColors.OnContainerDark,
                secondary = DraculaColors.SecondaryDark,
                tertiary = DraculaColors.TertiaryDark,
            )
            AppColorScheme.TOKYO_NIGHT -> presetDarkScheme(
                primary = TokyoNightColors.PrimaryDark,
                onPrimary = TokyoNightColors.OnPrimaryDark,
                primaryContainer = TokyoNightColors.ContainerDark,
                onPrimaryContainer = TokyoNightColors.OnContainerDark,
                secondary = TokyoNightColors.SecondaryDark,
                tertiary = TokyoNightColors.TertiaryDark,
            )
            AppColorScheme.EVERFOREST -> presetDarkScheme(
                primary = EverforestColors.PrimaryDark,
                onPrimary = EverforestColors.OnPrimaryDark,
                primaryContainer = EverforestColors.ContainerDark,
                onPrimaryContainer = EverforestColors.OnContainerDark,
                secondary = EverforestColors.SecondaryDark,
                tertiary = EverforestColors.TertiaryDark,
            )
            AppColorScheme.MONOCHROME -> presetDarkScheme(
                primary = MonochromeColors.PrimaryDark,
                onPrimary = MonochromeColors.OnPrimaryDark,
                primaryContainer = MonochromeColors.ContainerDark,
                onPrimaryContainer = MonochromeColors.OnContainerDark,
                secondary = MonochromeColors.SecondaryDark,
                tertiary = MonochromeColors.TertiaryDark,
            )
        }
    }

private fun presetLightChoice(choice: AppColorScheme): ColorScheme {
    return when (choice) {
        AppColorScheme.IRIDIUM -> presetLightScheme(
            primary = IridiumSeedDark,
            onPrimary = IridiumOnSeedLight,
            primaryContainer = IridiumSeedContainerLight,
            onPrimaryContainer = IridiumOnSeedContainerLight,
            secondary = IridiumSecondaryLight,
            tertiary = IridiumTertiaryLight,
        )
        AppColorScheme.OCEAN -> presetLightScheme(
            primary = OceanColors.PrimaryLight,
            onPrimary = OceanColors.OnPrimaryLight,
            primaryContainer = OceanColors.ContainerLight,
            onPrimaryContainer = OceanColors.OnContainerLight,
            secondary = OceanColors.SecondaryLight,
            tertiary = OceanColors.TertiaryLight,
        )
        AppColorScheme.FOREST -> presetLightScheme(
            primary = ForestColors.PrimaryLight,
            onPrimary = ForestColors.OnPrimaryLight,
            primaryContainer = ForestColors.ContainerLight,
            onPrimaryContainer = ForestColors.OnContainerLight,
            secondary = ForestColors.SecondaryLight,
            tertiary = ForestColors.TertiaryLight,
        )
        AppColorScheme.SUNSET -> presetLightScheme(
            primary = SunsetColors.PrimaryLight,
            onPrimary = SunsetColors.OnPrimaryLight,
            primaryContainer = SunsetColors.ContainerLight,
            onPrimaryContainer = SunsetColors.OnContainerLight,
            secondary = SunsetColors.SecondaryLight,
            tertiary = SunsetColors.TertiaryLight,
        )
        AppColorScheme.CATPPUCCIN -> presetLightScheme(
            primary = CatppuccinColors.PrimaryLight,
            onPrimary = CatppuccinColors.OnPrimaryLight,
            primaryContainer = CatppuccinColors.ContainerLight,
            onPrimaryContainer = CatppuccinColors.OnContainerLight,
            secondary = CatppuccinColors.SecondaryLight,
            tertiary = CatppuccinColors.TertiaryLight,
        )
        AppColorScheme.NORD -> presetLightScheme(
            primary = NordColors.PrimaryLight,
            onPrimary = NordColors.OnPrimaryLight,
            primaryContainer = NordColors.ContainerLight,
            onPrimaryContainer = NordColors.OnContainerLight,
            secondary = NordColors.SecondaryLight,
            tertiary = NordColors.TertiaryLight,
        )
        AppColorScheme.GRUVBOX -> presetLightScheme(
            primary = GruvboxColors.PrimaryLight,
            onPrimary = GruvboxColors.OnPrimaryLight,
            primaryContainer = GruvboxColors.ContainerLight,
            onPrimaryContainer = GruvboxColors.OnContainerLight,
            secondary = GruvboxColors.SecondaryLight,
            tertiary = GruvboxColors.TertiaryLight,
        )
        AppColorScheme.DRACULA -> presetLightScheme(
            primary = DraculaColors.PrimaryLight,
            onPrimary = DraculaColors.OnPrimaryLight,
            primaryContainer = DraculaColors.ContainerLight,
            onPrimaryContainer = DraculaColors.OnContainerLight,
            secondary = DraculaColors.SecondaryLight,
            tertiary = DraculaColors.TertiaryLight,
        )
        AppColorScheme.TOKYO_NIGHT -> presetLightScheme(
            primary = TokyoNightColors.PrimaryLight,
            onPrimary = TokyoNightColors.OnPrimaryLight,
            primaryContainer = TokyoNightColors.ContainerLight,
            onPrimaryContainer = TokyoNightColors.OnContainerLight,
            secondary = TokyoNightColors.SecondaryLight,
            tertiary = TokyoNightColors.TertiaryLight,
        )
        AppColorScheme.EVERFOREST -> presetLightScheme(
            primary = EverforestColors.PrimaryLight,
            onPrimary = EverforestColors.OnPrimaryLight,
            primaryContainer = EverforestColors.ContainerLight,
            onPrimaryContainer = EverforestColors.OnContainerLight,
            secondary = EverforestColors.SecondaryLight,
            tertiary = EverforestColors.TertiaryLight,
        )
        AppColorScheme.MONOCHROME -> presetLightScheme(
            primary = MonochromeColors.PrimaryLight,
            onPrimary = MonochromeColors.OnPrimaryLight,
            primaryContainer = MonochromeColors.ContainerLight,
            onPrimaryContainer = MonochromeColors.OnContainerLight,
            secondary = MonochromeColors.SecondaryLight,
            tertiary = MonochromeColors.TertiaryLight,
        )
    }
}

/**
 * Static color presets used when dynamic color is unavailable or disabled.
 * Every container/error/surface role is derived from the preset accents so
 * non-dynamic schemes never render baseline containers.
 */
internal object IridiumColors {

    fun scheme(preset: AppColorScheme, dark: Boolean): ColorScheme =
        presetScheme(preset, dark)

    /**
     * True-black override for dark mode: backgrounds go pure black for OLED
     * power savings while containers deepen toward black instead of
     * flattening to gray, so the scheme's tint survives.
     */
    fun ColorScheme.amoled(): ColorScheme = copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceBright = surfaceBright.darkened(0.45f),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = surfaceContainerLow.darkened(0.45f),
        surfaceContainer = surfaceContainer.darkened(0.35f),
        surfaceContainerHigh = surfaceContainerHigh.darkened(0.25f),
        surfaceContainerHighest = surfaceContainerHighest.darkened(0.12f),
        surfaceVariant = surfaceVariant.darkened(0.35f),
        scrim = Color.Black,
    )
}
