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

    /**
     * Forced-override CSS for explicit bare-hex colors (custom palettes).
     *
     * [selectionBgHex]/[selectionFgHex] style the text selection. Without them
     * the page inherits the toolkit's fixed pale blue, which clashes with the
     * page palette and, because every element's colour is forced here, renders
     * pale text on pale blue - unreadable. Null falls back to a tint derived
     * from the page's own foreground so contrast is always high.
     */
    fun overrideCssColors(
        bgHex: String,
        fgHex: String,
        selectionBgHex: String? = null,
        selectionFgHex: String? = null,
    ): String {
        val bg = "#$bgHex"
        val fg = "#$fgHex"
        val selectionBg = "#${selectionBgHex ?: defaultSelectionBackground(bgHex)}"
        val selectionFg = "#${selectionFgHex ?: fgHex}"
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
            append(" -webkit-text-fill-color: $fg !important;")
            append(" }\n")
            append("a, a:link, a:visited, a:hover, a * {")
            append(" color: $fg !important; -webkit-text-fill-color: $fg !important; }\n")
            // Selection follows the theme, and repeats the body class so the
            // forced text colour above cannot override it.
            append("$bodySelector ::selection, $bodySelector::selection {")
            append(" background-color: $selectionBg !important;")
            append(" color: $selectionFg !important;")
            append(" -webkit-text-fill-color: $selectionFg !important; }")
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
    fun injectOverrideScriptColors(
        bgHex: String,
        fgHex: String,
        selectionBgHex: String? = null,
        selectionFgHex: String? = null,
    ): String {
        val css = overrideCssColors(bgHex, fgHex, selectionBgHex, selectionFgHex)
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

    /**
     * CSS `#RRGGBBAA` for an ARGB int. Note the channel order differs from
     * ARGB: CSS puts alpha last.
     */
    internal fun hexWithAlpha(argb: Int): String =
        hex(argb) + Integer.toHexString((argb ushr 24) and 0xFF).padStart(2, '0')

    /**
     * Drops the foreground alpha so the text itself reads dimmer. The
     * background is untouched: on a pure-black page the pixels must stay off.
     */
    fun dimForeground(argb: Int, intensity: Float): Int {
        val keep = 1f - intensity.coerceIn(0f, 1f) * MAX_DIM_ALPHA
        val alpha = ((((argb ushr 24) and 0xFF) * keep).toInt()).coerceIn(MIN_TEXT_ALPHA, 255)
        return (alpha shl 24) or (argb and 0x00FFFFFF)
    }

    /**
     * Dimming dark text over a light page turns it grey and loses contrast
     * fast, so light schemes get a much smaller budget than dark ones.
     */
    fun maxDimFor(backgroundArgb: Int): Float =
        if (isLightPage(backgroundArgb)) LIGHT_MAX_DIM else MAX_DIM_ALPHA

    /** True when [backgroundArgb] is a light page (dark text on it). */
    fun isLightPage(backgroundArgb: Int): Boolean {
        val r = (backgroundArgb shr 16) and 0xFF
        val g = (backgroundArgb shr 8) and 0xFF
        val b = backgroundArgb and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b) > 127
    }

    /**
     * Fallback selection tint when the caller has no themed colour: 22% of the
     * page foreground over its background. Guaranteed to differ from the page
     * by a fixed amount in both light and dark palettes.
     */
    internal fun defaultSelectionBackground(bgHex: String): String {
        val bg = bgHex.toLong(16)
        val fg = if (isLightPage(0xFF000000.toInt() or bg.toInt())) 0x000000 else 0xFFFFFF
        val mix = 0.22
        fun channel(shift: Int): Int {
            val b = ((bg shr shift) and 0xFF).toDouble()
            val f = ((fg shr shift) and 0xFF).toDouble()
            return (b + (f - b) * mix).toInt().coerceIn(0, 255)
        }
        return "%02x%02x%02x".format(channel(16), channel(8), channel(0))
    }

    /** Maximum extra dim as a fraction of the text's own alpha. */
    private const val MAX_DIM_ALPHA = 0.6f

    /** Light pages dim far less: grey-on-white loses legibility quickly. */
    private const val LIGHT_MAX_DIM = 0.3f

    /** Never fade text below a legible floor. */
    private const val MIN_TEXT_ALPHA = 64

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
