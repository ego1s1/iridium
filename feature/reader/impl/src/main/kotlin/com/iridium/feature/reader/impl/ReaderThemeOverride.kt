package com.iridium.feature.reader.impl

import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.pageBackgroundArgb
import com.iridium.core.model.pageForegroundArgb

/**
 * Forced-override theming for the reader page (on Readium).
 *
 * This re-colors arbitrary book CSS by injecting a stylesheet that wins
 * every specificity fight: a body class repeated enough to beat the book's
 * own rules, `!important` on every declaration, and
 * `-webkit-text-fill-color: currentcolor !important` so WebKit gives up the
 * text color even where `color` would be overridden.
 *
 * We mirror that technique: the reader navigator injects a `<style>` (built by
 * [injectOverrideScript]) whose CSS forces the page's exact background, text
 * and link colors from [ColorSchemeChoice]. Contains no Android deps — pure
 * String generation, unit-tested.
 */
internal object ReaderThemeOverride {

    /** Body class suffix repeated to win specificity over the book's CSS. */
    private const val SPECIFICITY_CLASS = "__Ir_Theme"

    /**
     * The forced-override CSS for [theme], using its exact background and
     * foreground colors. Links reuse the foreground (Iridium has no separate
     * link color in [ColorSchemeChoice]).
     */
    fun overrideCss(theme: ColorSchemeChoice): String {
        val (bg, fg) = theme.pageBackgroundArgb() to theme.pageForegroundArgb()
        return overrideCssColors(hex(bg), hex(fg))
    }

    /** Forced-override CSS for explicit bare-hex colors (custom palettes). */
    fun overrideCssColors(bgHex: String, fgHex: String): String {
        val bg = "#$bgHex"
        val fg = "#$fgHex"
        // `body.__Ir_Theme.__Ir_Theme...` — 16x repetition beats any book rule.
        val bodySelector = (0 until 16).joinToString(separator = "", prefix = "body") {
            ".$SPECIFICITY_CLASS"
        }
        val allSelector = (0 until 15).joinToString(separator = "", prefix = ".$SPECIFICITY_CLASS") {
            ".$SPECIFICITY_CLASS"
        } + " *"
        return buildString {
            append("$bodySelector { background-color: $bg !important; }\n")
            append("$allSelector {")
            append(" background-color: transparent !important;\n")
            append(" color: $fg !important;\n")
            append(" -webkit-text-fill-color: currentcolor !important;")
            append(" }\n")
            append("a, a:link, a:visited, a:hover, a * {")
            append(" color: $fg !important; -webkit-text-fill-color: currentcolor !important; }")
        }
    }

    /**
     * A JavaScript statement that injects (or updates) a `<style>` carrying
     * [overrideCss] for [theme] into the current document. Safe to embed in a
     * single `evaluateJavascript` string.
     */
    fun injectOverrideScript(theme: ColorSchemeChoice): String {
        val (bg, fg) = theme.pageBackgroundArgb() to theme.pageForegroundArgb()
        return injectOverrideScriptColors(hex(bg), hex(fg))
    }

    /**
     * A JavaScript statement that injects (or updates) a `<style>` carrying
     * [overrideCssColors] into the current document. Safe to embed in a
     * single `evaluateJavascript` string.
     */
    fun injectOverrideScriptColors(bgHex: String, fgHex: String): String {
        val css = overrideCssColors(bgHex, fgHex)
        val escaped = css
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")
        return "(function(){var s=document.getElementById('iridiumThemeOverride');" +
            "if(!s){s=document.createElement('style');" +
            "s.id='iridiumThemeOverride';document.head.appendChild(s);}" +
            "s.textContent='$escaped';})()"
    }

    /** #RRGGBB from an ARGB int (alpha dropped for readability of page colors). */
    internal fun hex(argb: Int): String =
        Integer.toHexString(argb and 0x00FFFFFF).padStart(6, '0')

    /** True when [argb] is pure black: OLED pixels stay off, never warm it. */
    fun isPureBlack(argb: Int): Boolean = (argb and 0x00FFFFFF) == 0

    /**
     * Warms [fgArgb] toward amber by [intensity]: night-light must tint text,
     * never the background. [intensity] 0f returns [fgArgb] unchanged.
     */
    fun warmForeground(fgArgb: Int, intensity: Float): Int {
        val t = intensity.coerceIn(0f, 1f) * 0.55f
        if (t <= 0f) return fgArgb
        val r = ((fgArgb shr 16) and 0xFF)
        val g = ((fgArgb shr 8) and 0xFF)
        val b = (fgArgb and 0xFF)
        val nr = (r + (0xFF - r) * t).toInt().coerceIn(0, 255)
        val ng = (g + (0x9E - g) * t).toInt().coerceIn(0, 255)
        val nb = (b + (0x42 - b) * t).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
    }
}
