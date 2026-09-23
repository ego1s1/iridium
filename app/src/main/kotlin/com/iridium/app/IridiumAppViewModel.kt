package com.iridium.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** App-wide chrome state: theme, motion personality and crash-report consent. */
data class AppUiState(
    val theme: ThemePreferences = ThemePreferences(),
    val motionStyle: MotionStyle = MotionStyle.EXPRESSIVE,
    val crashReportingEnabled: Boolean = false,
    /** True until we know otherwise, so the prompt never flashes mid-load. */
    val crashConsentAsked: Boolean = true,
    val hasPendingCrashReport: Boolean = false,
)

@HiltViewModel
class IridiumAppViewModel @Inject constructor(
    private val preferences: IridiumPreferencesDataSource,
    private val crashReporter: CrashReporter,
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

    private val pendingCrash = MutableStateFlow(false)

    val uiState: StateFlow<AppUiState> = combine(
        preferences.themePreferences,
        preferences.motionStyle,
        preferences.crashReportingEnabled,
        preferences.crashReportingAsked,
        pendingCrash,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        AppUiState(
            theme = values[0] as ThemePreferences,
            motionStyle = values[1] as MotionStyle,
            crashReportingEnabled = values[2] as Boolean,
            crashConsentAsked = values[3] as Boolean,
            hasPendingCrashReport = values[4] as Boolean,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(),
    )

    init {
        // The handler is installed only while consent is granted, so nothing
        // is ever captured before the user opts in.
        viewModelScope.launch {
            preferences.crashReportingEnabled.collect { enabled ->
                if (enabled) crashReporter.install() else crashReporter.uninstall()
                pendingCrash.value = crashReporter.pending().isNotEmpty()
            }
        }
    }

    fun setCrashReporting(enabled: Boolean) {
        viewModelScope.launch { preferences.setCrashReporting(enabled) }
    }

    fun dismissCrashConsent() {
        viewModelScope.launch { preferences.setCrashReportingAsked(true) }
    }

    /** Text of the newest pending report, for a user-initiated share. */
    suspend fun crashReportText(): String? =
        crashReporter.pending().firstOrNull()?.let { crashReporter.read(it) }

    fun markCrashReportShared() {
        viewModelScope.launch {
            crashReporter.pending().firstOrNull()?.let { crashReporter.clear(it) }
            pendingCrash.value = crashReporter.pending().isNotEmpty()
        }
    }

    fun discardCrashReports() {
        viewModelScope.launch {
            crashReporter.clearAll()
            pendingCrash.value = false
        }
    }
}
