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
