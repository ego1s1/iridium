package com.iridium.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** App-wide chrome state: theme + motion personality. */
data class AppUiState(
    val theme: ThemePreferences = ThemePreferences(),
    val motionStyle: MotionStyle = MotionStyle.EXPRESSIVE,
)

@HiltViewModel
class IridiumAppViewModel @Inject constructor(
    preferences: IridiumPreferencesDataSource,
) : ViewModel() {

    /**
     * Null until the first preference emission: the onboarding gate must not
     * flash the wizard for a returning user while DataStore is still loading.
     */
    val onboardingCompleted: StateFlow<Boolean?> = preferences.onboardingCompleted
        .map { it as Boolean? }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val uiState: StateFlow<AppUiState> = combine(
        preferences.themePreferences,
        preferences.motionStyle,
        ::AppUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )
}
