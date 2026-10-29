package com.iridium.feature.settings.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iridium.core.data.DriveSyncManager
import com.iridium.core.data.DriveSyncResult
import com.iridium.core.data.LibraryBackupManager
import com.iridium.core.data.RestoreReport
import com.iridium.core.datastore.IridiumPreferencesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.iridium.core.datastore.SafGrantStore
import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ThemeMode
import com.iridium.core.model.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: IridiumPreferencesDataSource,
    private val grants: SafGrantStore,
    private val backup: LibraryBackupManager,
    private val sync: DriveSyncManager,
) : ViewModel() {

    /** Serializes the whole library's metadata for export. */
    suspend fun exportSnapshot(): String = backup.snapshot()

    /** Applies a backup's metadata; returns how many books were restored. */
    suspend fun importSnapshot(json: String): RestoreReport = backup.restore(json)

    /** Signed-in Drive account for sync, or null. */
    val driveAccount: StateFlow<String?> = preferences.driveAccount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun onDriveSignedIn(email: String) {
        viewModelScope.launch { preferences.setDriveAccount(email) }
    }

    fun onDriveSignOut() {
        viewModelScope.launch { preferences.setDriveAccount(null) }
    }

    fun onSyncNow() {
        val email = driveAccount.value ?: return
        viewModelScope.launch {
            _syncMessage.value = when (val result = sync.syncNow(email)) {
                DriveSyncResult.SignedOut -> null
                DriveSyncResult.UpToDate -> "Already up to date"
                is DriveSyncResult.Uploaded -> "Backed up ${result.books} book(s)"
                is DriveSyncResult.Downloaded -> "Restored ${result.restored} book(s)"
                is DriveSyncResult.Failed -> "Sync failed: ${result.reason}"
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    /** Folders whose persisted SAF grant died: scans skip them, the UI
     * offers re-link or forget. Refreshes whenever the linked set changes. */
    private val revokedFolders: StateFlow<Set<String>> = preferences.linkedFolders
        .mapLatest { folders -> folders - grants.persistedReadUris() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.themePreferences,
        preferences.motionStyle,
        preferences.readerPreferences,
        preferences.libraryDisplay,
        preferences.crashReportingEnabled,
        preferences.linkedFolders,
        revokedFolders,
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        SettingsUiState(
            theme = args[0] as ThemePreferences,
            motionStyle = args[1] as MotionStyle,
            reader = args[2] as ReaderPreferences,
            libraryDisplay = args[3] as LibraryDisplay,
            crashReportingEnabled = args[4] as Boolean,
            linkedFolders = args[5] as Set<String>,
            revokedFolders = args[6] as Set<String>,
        )
    }.catch { error ->
        // A failed read would otherwise cancel the state flow and leave the
        // screen on its initial value forever. Settings has no dedicated error
        // state, so fall back to the documented defaults: the screen stays
        // usable and writes still reach DataStore.
        if (error is CancellationException) throw error
        emit(SettingsUiState())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    /** Latest pending slider write per field; cancelled-and-replaced per drag tick so
     * scrubbing persists once, on settle, instead of hammering DataStore. */
    private val sliderJobs = mutableMapOf<String, Job>()

    private fun coalesceSliderWrite(key: String, transform: (ReaderPreferences) -> ReaderPreferences) {
        sliderJobs[key]?.cancel()
        sliderJobs[key] = viewModelScope.launch {
            delay(SLIDER_WRITE_DEBOUNCE_MS)
            preferences.updateReaderPreferences(transform)
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetThemeMode -> updateTheme {
                // Pure black needs a dark canvas: leaving for LIGHT clears a
                // stale flag instead of resurrecting it later.
                it.copy(
                    mode = action.mode,
                    amoled = if (action.mode == ThemeMode.LIGHT) false else it.amoled,
                )
            }
            is SettingsAction.SetDynamicColor -> updateTheme { it.copy(dynamicColor = action.enabled) }
            is SettingsAction.SetColorScheme ->
                updateTheme { it.copy(colorScheme = action.scheme, dynamicColor = false) }
            is SettingsAction.SetAmoled -> updateTheme { it.copy(amoled = action.enabled) }
            is SettingsAction.SetHapticsEnabled ->
                updateTheme { it.copy(hapticsEnabled = action.enabled) }
            is SettingsAction.SetMotionStyle -> viewModelScope.launch {
                preferences.updateMotionStyle(action.style)
            }
            is SettingsAction.SetSortOrder ->
                updateDisplay { it.copy(sortOrder = action.order) }
            is SettingsAction.SetFilter -> updateDisplay { it.copy(filter = action.filter) }
            is SettingsAction.SetHideErrors -> updateDisplay { it.copy(hideErrors = action.hide) }
            is SettingsAction.SetKeepScreenOn -> updateReader { it.copy(keepScreenOn = action.enabled) }
            is SettingsAction.SetVolumeKeys -> updateReader { it.copy(volumeKeys = action.enabled) }
            is SettingsAction.SetFontSize -> coalesceSliderWrite("fontScale") { it.copy(fontScale = action.scale) }
            is SettingsAction.SetMargins -> coalesceSliderWrite("margins") { it.copy(pageMargins = action.margins) }
            is SettingsAction.SetLineHeight -> coalesceSliderWrite("lineHeight") { it.copy(lineHeight = action.lineHeight) }
            is SettingsAction.AddLinkedFolder -> viewModelScope.launch {
                preferences.addLinkedFolder(action.uri)
            }
            is SettingsAction.RemoveLinkedFolder -> viewModelScope.launch {
                grants.releaseReadUri(action.uri)
                preferences.removeLinkedFolder(action.uri)
            }
            is SettingsAction.SetCrashReporting -> viewModelScope.launch {
                preferences.setCrashReporting(action.enabled)
            }
        }
    }

    private fun updateTheme(transform: (ThemePreferences) -> ThemePreferences) {
        viewModelScope.launch { preferences.updateThemePreferences(transform) }
    }

    private fun updateReader(transform: (ReaderPreferences) -> ReaderPreferences) {
        viewModelScope.launch { preferences.updateReaderPreferences(transform) }
    }

    private fun updateDisplay(transform: (LibraryDisplay) -> LibraryDisplay) {
        viewModelScope.launch { preferences.updateLibraryDisplay(transform) }
    }

    private companion object {
        /** Settles slider drags before the single DataStore write. */
        const val SLIDER_WRITE_DEBOUNCE_MS = 150L
    }
}
