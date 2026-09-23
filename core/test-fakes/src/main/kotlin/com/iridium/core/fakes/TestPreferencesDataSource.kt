package com.iridium.core.fakes

import com.iridium.core.datastore.IridiumPreferencesDataSource
import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [IridiumPreferencesDataSource] with direct state hooks. */
class TestPreferencesDataSource : IridiumPreferencesDataSource {

    private val onboarding = MutableStateFlow(false)
    private val treeUri = MutableStateFlow<String?>(null)
    private val reader = MutableStateFlow(ReaderPreferences())
    private val theme = MutableStateFlow(ThemePreferences())
    private val motion = MutableStateFlow(MotionStyle.EXPRESSIVE)
    private val display = MutableStateFlow(LibraryDisplay())
    private val crashEnabled = MutableStateFlow(false)
    private val crashAsked = MutableStateFlow(false)

    override val onboardingCompleted: Flow<Boolean> = onboarding
    override val sourceTreeUri: Flow<String?> = treeUri
    override val readerPreferences: Flow<ReaderPreferences> = reader
    override val themePreferences: Flow<ThemePreferences> = theme
    override val motionStyle: Flow<MotionStyle> = motion
    override val libraryDisplay: Flow<LibraryDisplay> = display
    override val crashReportingEnabled: Flow<Boolean> = crashEnabled
    override val crashReportingAsked: Flow<Boolean> = crashAsked

    override suspend fun setCrashReporting(enabled: Boolean) {
        crashEnabled.value = enabled
        crashAsked.value = true
    }

    override suspend fun setCrashReportingAsked(asked: Boolean) {
        crashAsked.value = asked
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        onboarding.value = completed
    }

    override suspend fun setSourceTreeUri(uri: String?) {
        treeUri.value = uri
    }

    override suspend fun updateReaderPreferences(transform: (ReaderPreferences) -> ReaderPreferences) {
        reader.update(transform)
    }

    override suspend fun updateThemePreferences(transform: (ThemePreferences) -> ThemePreferences) {
        theme.update(transform)
    }

    override suspend fun updateMotionStyle(style: MotionStyle) {
        motion.value = style
    }

    override suspend fun updateLibraryDisplay(transform: (LibraryDisplay) -> LibraryDisplay) {
        display.update(transform)
    }
}
