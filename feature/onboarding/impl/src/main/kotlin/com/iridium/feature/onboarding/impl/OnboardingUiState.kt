package com.iridium.feature.onboarding.impl

import android.net.Uri
import android.provider.DocumentsContract
import com.iridium.core.model.AppColorScheme
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemeMode
import com.iridium.core.model.ThemePreferences

/**
 * Folder-first wizard: Welcome → Access → Reading → Appearance. Theme,
 * reading and folder choices persist immediately so quitting mid-wizard
 * never loses them; Skip finishes from anywhere. The library scans only
 * the linked SAF folders on arrival.
 */
sealed interface OnboardingUiState {
    data object Welcome : OnboardingUiState

    /** Folder-link step: which SAF folders the library scans for EPUBs. */
    data class Access(val folders: List<LinkedFolder>) : OnboardingUiState

    /** Reader defaults step: text size, line spacing, book colors. */
    data class Reading(
        val prefs: ReaderPreferences,
    ) : OnboardingUiState

    data class Appearance(
        val theme: ThemePreferences,
    ) : OnboardingUiState
}

/** One linked SAF tree plus its human-readable display name. */
data class LinkedFolder(
    val uri: String,
    val label: String,
) {
    companion object {
        fun of(uri: String): LinkedFolder = LinkedFolder(uri, linkedFolderLabel(uri))
    }
}

/**
 * Display name for a linked tree URI without a ContentResolver round trip.
 * Tree document ids look like `primary:Books` (external storage) so the
 * volume prefix is dropped; anything else falls back to the last segment.
 */
internal fun linkedFolderLabel(uri: String): String {
    val treeId = runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(uri)) }.getOrNull()
    val raw = treeId?.substringAfter(':', missingDelimiterValue = "")?.takeIf { it.isNotEmpty() }
        ?: Uri.parse(uri).lastPathSegment?.substringAfterLast(':')
    return raw?.trim()?.takeIf { it.isNotEmpty() } ?: uri
}

sealed interface OnboardingAction {
    /** Welcome CTA. */
    data object GetStarted : OnboardingAction

    /** Leave the wizard (defaults everywhere is a valid start). */
    data object Skip : OnboardingAction

    /** Back one step. */
    data object BackStep : OnboardingAction

    /** Forward one step (Access -> Reading -> Appearance). */
    data object Advance : OnboardingAction

    /** Reader text size multiplier. */
    data class SetFontScale(val scale: Float) : OnboardingAction

    /** Reader line-height multiplier. */
    data class SetLineHeight(val lineHeight: Float) : OnboardingAction

    /** Reader book-color theme. */
    data class SetReaderTheme(val theme: ColorSchemeChoice) : OnboardingAction

    data class SetThemeMode(val mode: ThemeMode) : OnboardingAction

    data class SetDynamicColor(val enabled: Boolean) : OnboardingAction

    data class SetColorScheme(val scheme: AppColorScheme) : OnboardingAction

    data class SetAmoled(val enabled: Boolean) : OnboardingAction

    /** Link one SAF folder for library scans. */
    data class AddLinkedFolder(val uri: String) : OnboardingAction

    /** Unlink one SAF folder (grant released, row removed). */
    data class RemoveLinkedFolder(val uri: String) : OnboardingAction

    /** Mark onboarding complete and continue to the library. */
    data object Finish : OnboardingAction
}
