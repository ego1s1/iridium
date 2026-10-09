package com.iridium.feature.onboarding.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Permission-first step wizard: Welcome → Access → Reading → Appearance.
 * Theme and reading choices persist immediately so quitting mid-wizard never
 * loses them; Skip finishes from anywhere. Nothing is copied or indexed
 * here — the library scans all of shared storage on arrival.
 */
@HiltViewModel
internal class OnboardingViewModel @Inject constructor(
    private val preferences: IridiumPreferencesDataSource,
) : ViewModel() {

    private enum class Step { WELCOME, ACCESS, READING, APPEARANCE }

    private val step = MutableStateFlow(Step.WELCOME)

    val uiState: StateFlow<OnboardingUiState> = combine(
        step,
        preferences.themePreferences,
        preferences.readerPreferences,
        preferences.linkedFolders,
        ::toUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OnboardingUiState.Welcome,
    )

    private fun toUiState(
        step: Step,
        theme: ThemePreferences,
        reader: ReaderPreferences,
        folders: Set<String>,
    ): OnboardingUiState = when (step) {
        Step.WELCOME -> OnboardingUiState.Welcome
        Step.ACCESS -> OnboardingUiState.Access(folders.size)
        Step.READING -> OnboardingUiState.Reading(reader)
        Step.APPEARANCE -> OnboardingUiState.Appearance(theme)
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.GetStarted -> step.value = Step.ACCESS
            OnboardingAction.Skip -> finish()
            OnboardingAction.BackStep -> step.value = when (step.value) {
                Step.WELCOME -> Step.WELCOME
                Step.ACCESS -> Step.WELCOME
                Step.READING -> Step.ACCESS
                Step.APPEARANCE -> Step.READING
            }
            OnboardingAction.Advance -> step.value = when (step.value) {
                Step.WELCOME -> Step.ACCESS
                Step.ACCESS -> Step.READING
                Step.READING -> Step.APPEARANCE
                Step.APPEARANCE -> Step.APPEARANCE
            }
            is OnboardingAction.SetFontScale ->
                coalesceReaderWrite("fontScale") { it.copy(fontScale = action.scale.coerceIn(0.5f, 3f)) }
            is OnboardingAction.SetLineHeight ->
                coalesceReaderWrite("lineHeight") { it.copy(lineHeight = action.lineHeight.coerceIn(1f, 2.5f)) }
            is OnboardingAction.SetReaderTheme ->
                updateReader { it.copy(theme = action.theme) }
            is OnboardingAction.SetThemeMode -> updateTheme { it.copy(mode = action.mode) }
            is OnboardingAction.SetDynamicColor -> updateTheme { it.copy(dynamicColor = action.enabled) }
            is OnboardingAction.SetColorScheme -> updateTheme {
                it.copy(colorScheme = action.scheme, dynamicColor = false)
            }
            is OnboardingAction.SetAmoled -> updateTheme { it.copy(amoled = action.enabled) }
            is OnboardingAction.AddLinkedFolder -> viewModelScope.launch {
                preferences.addLinkedFolder(action.uri)
            }
            OnboardingAction.Finish -> finish()
        }
    }

    private fun updateTheme(transform: (ThemePreferences) -> ThemePreferences) {
        viewModelScope.launch { preferences.updateThemePreferences(transform) }
    }

    private fun updateReader(transform: (ReaderPreferences) -> ReaderPreferences) {
        viewModelScope.launch { preferences.updateReaderPreferences(transform) }
    }

    /** Latest pending slider write per field; slider drags persist once, on settle. */
    private val sliderJobs = mutableMapOf<String, Job>()

    private fun coalesceReaderWrite(key: String, transform: (ReaderPreferences) -> ReaderPreferences) {
        sliderJobs[key]?.cancel()
        sliderJobs[key] = viewModelScope.launch {
            delay(SLIDER_WRITE_DEBOUNCE_MS)
            preferences.updateReaderPreferences(transform)
        }
    }

    private fun finish() {
        // Navigate only after the flag commits: relaunching before the
        // DataStore write lands would replay onboarding. Guarded: a double
        // tap on Skip/Finish must not emit twice and navigate twice.
        if (!finishSent.compareAndSet(false, true)) return
        viewModelScope.launch {
            preferences.setOnboardingCompleted(true)
            _finished.emit(Unit)
        }
    }

    /** First-call-wins latch for [finish]. */
    private val finishSent = AtomicBoolean(false)

    private val _finished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emitted once [finish] has committed; the host navigates on this. */
    val finished: SharedFlow<Unit> = _finished.asSharedFlow()

    /** Slider writes settle before the single DataStore write. */
    private companion object {
        const val SLIDER_WRITE_DEBOUNCE_MS = 150L
    }
}
