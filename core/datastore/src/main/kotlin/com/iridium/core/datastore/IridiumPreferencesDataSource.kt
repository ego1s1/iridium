package com.iridium.core.datastore

import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemePreferences
import kotlinx.coroutines.flow.Flow

/**
 * Persisted user preferences. Implemented by DataStore; faked in tests.
 */
interface IridiumPreferencesDataSource {
    /** True once the user completes onboarding (first folder linked). */
    val onboardingCompleted: Flow<Boolean>

    /** The last-linked SAF source tree URI, if the user granted one for rescans. */
    val sourceTreeUri: Flow<String?>

    /** Reader preferences (flow, font scale, align, theme, brightness). */
    val readerPreferences: Flow<ReaderPreferences>

    /** App theme preferences (mode, dynamic color, AMOLED black). */
    val themePreferences: Flow<ThemePreferences>

    /** Motion personality (spring physics vs calm fades). */
    val motionStyle: Flow<MotionStyle>

    /** Persisted library display options (sort, filter, error visibility). */
    val libraryDisplay: Flow<LibraryDisplay>

    /** True when the user opted in to sending crash reports. Defaults to off. */
    val crashReportingEnabled: Flow<Boolean>

    /** True once the user has answered the crash-reporting prompt. */
    val crashReportingAsked: Flow<Boolean>

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setSourceTreeUri(uri: String?)

    suspend fun updateReaderPreferences(transform: (ReaderPreferences) -> ReaderPreferences)

    suspend fun updateThemePreferences(transform: (ThemePreferences) -> ThemePreferences)

    suspend fun updateMotionStyle(style: MotionStyle)

    suspend fun updateLibraryDisplay(transform: (LibraryDisplay) -> LibraryDisplay)

    suspend fun setCrashReporting(enabled: Boolean)

    suspend fun setCrashReportingAsked(asked: Boolean)
}
