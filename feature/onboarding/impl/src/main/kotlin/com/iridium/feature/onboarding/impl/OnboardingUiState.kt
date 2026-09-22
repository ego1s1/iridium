package com.iridium.feature.onboarding.impl

import android.net.Uri
import com.iridium.core.model.AppColorScheme
import com.iridium.core.model.ThemeMode
import com.iridium.core.model.ThemePreferences

/**
 * Link-only wizard: Welcome → Folder → Appearance. It asks exactly one
 * question (where the books live) and copies nothing; picking a folder
 * persists its tree URI and advances, and the library indexes it lazily.
 */
sealed interface OnboardingUiState {
    data object Welcome : OnboardingUiState

    /**
     * Folder step: [pickerHintVisible] turns on after a dismissed picker or
     * denied grant, so the step explains itself instead of sitting silent.
     */
    data class Folder(
        val pickerHintVisible: Boolean = false,
    ) : OnboardingUiState

    data class Appearance(
        val theme: ThemePreferences,
    ) : OnboardingUiState
}

sealed interface OnboardingAction {
    /** Welcome CTA. */
    data object GetStarted : OnboardingAction

    /** Leave the wizard (an empty library is a valid start). */
    data object Skip : OnboardingAction

    /** Back one step. */
    data object BackStep : OnboardingAction

    /** The user picked the folder to read from. */
    data class FolderSelected(val uri: Uri) : OnboardingAction

    /** The picker was dismissed or its grant denied: explain, don't stall. */
    data object FolderPickerDismissed : OnboardingAction

    data class SetThemeMode(val mode: ThemeMode) : OnboardingAction

    data class SetDynamicColor(val enabled: Boolean) : OnboardingAction

    data class SetColorScheme(val scheme: AppColorScheme) : OnboardingAction

    data class SetAmoled(val enabled: Boolean) : OnboardingAction

    /** Mark onboarding complete and continue to the library. */
    data object Finish : OnboardingAction
}
