package com.iridium.feature.onboarding.impl

import com.iridium.core.model.AppColorScheme
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemeMode
import com.iridium.core.model.ThemePreferences

/**
 * Permission-first wizard: Welcome → Access → Reading → Appearance. Theme
 * and reading choices persist immediately so quitting mid-wizard never loses
 * them; Skip finishes from anywhere. Nothing is copied or indexed here — the
 * library scans all of shared storage on arrival.
 */
sealed interface OnboardingUiState {
    data object Welcome : OnboardingUiState

    /** All-files access step: the library reads every EPUB on the device. */
    data object Access : OnboardingUiState

    /** Reader defaults step: text size, line spacing, book colors. */
    data class Reading(
        val prefs: ReaderPreferences,
    ) : OnboardingUiState

    data class Appearance(
        val theme: ThemePreferences,
    ) : OnboardingUiState
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

    /** Mark onboarding complete and continue to the library. */
    data object Finish : OnboardingAction
}
