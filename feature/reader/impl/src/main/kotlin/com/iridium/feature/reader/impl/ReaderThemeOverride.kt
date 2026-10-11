package com.iridium.feature.reader.impl

import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.pageBackgroundArgb
import com.iridium.core.model.pageForegroundArgb

/**
 * Lithium-style forced-override theming for the reader page (on Readium).
 *
 * Lithium re-colors arbitrary book CSS by injecting a stylesheet that wins
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
object ReaderThemeOverride {

    /** Body class suffix repeated to win specificity over the book's CSS. */
    private const val SPECIFICITY_CLASS = "__Ir_Theme"

    /**
     * The forced-override CSS for [theme], using its exact background and
     * foreground colors. Links reuse the foreground (Iridium has no separate
     * link color in [ColorSchemeChoice]).
     */
    fun overrideCss(theme: ColorSchemeChoice): String {
        val bg = "#" + hex(theme.pageBackgroundArgb())
        val fg = "#" + hex(theme.pageForegroundArgb())
        // `body.__Ir_Theme.__Ir_Theme...` — 16x repetition beats any book rule.
        val bodySelector = (0 until 16).joinToString(separator = " ", prefix = "body") {
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
        val css = overrideCss(theme)
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
}