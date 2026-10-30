package com.iridium.feature.settings.impl

import com.iridium.core.fakes.FakeDriveSignIn
import com.iridium.core.fakes.FakeSafGrantStore
import com.iridium.core.data.DriveGatewayFactory
import com.iridium.core.data.DriveSyncManager
import com.iridium.core.data.LibraryBackupManager
import com.iridium.core.fakes.TestBooksRepository
import com.iridium.core.fakes.TestPreferencesDataSource
import com.iridium.core.model.AppColorScheme
import com.iridium.core.model.LibrarySortOrder
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ThemeMode
import com.iridium.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val preferences = TestPreferencesDataSource()
    private val backup = LibraryBackupManager(TestBooksRepository())
    private val sync = DriveSyncManager(
        backup,
        DriveGatewayFactory { throw UnsupportedOperationException("no network in settings tests") },
        ioDispatcher = Dispatchers.Unconfined,
    )
    private val driveSignIn = FakeDriveSignIn()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        viewModel = SettingsViewModel(preferences, FakeSafGrantStore(), backup, sync, driveSignIn)
    }

    @Test
    fun `starts from persisted defaults`() = runTest {
        val state = viewModel.uiState.first()
        assertEquals(ThemeMode.SYSTEM, state.theme.mode)
        assertEquals(MotionStyle.EXPRESSIVE, state.motionStyle)
        assertFalse(state.crashReportingEnabled)
    }

    @Test
    fun `theme mode writes through`() = runTest {
        viewModel.onAction(SettingsAction.SetThemeMode(ThemeMode.DARK))
        assertEquals(ThemeMode.DARK, viewModel.uiState.first { it.theme.mode == ThemeMode.DARK }.theme.mode)
    }

    @Test
    fun `choosing a preset turns dynamic colour off`() = runTest {
        viewModel.onAction(SettingsAction.SetColorScheme(AppColorScheme.FOREST))
        val theme = viewModel.uiState.first { it.theme.colorScheme == AppColorScheme.FOREST }.theme
        assertFalse("a manual preset must not be overridden by wallpaper colour", theme.dynamicColor)
    }

    @Test
    fun `motion style writes through`() = runTest {
        viewModel.onAction(SettingsAction.SetMotionStyle(MotionStyle.CALM))
        assertEquals(MotionStyle.CALM, viewModel.uiState.first { it.motionStyle == MotionStyle.CALM }.motionStyle)
    }

    @Test
    fun `haptics toggle writes through`() = runTest {
        viewModel.onAction(SettingsAction.SetHapticsEnabled(false))
        assertFalse(viewModel.uiState.first { !it.theme.hapticsEnabled }.theme.hapticsEnabled)
    }

    @Test
    fun `library sort writes through`() = runTest {
        viewModel.onAction(SettingsAction.SetSortOrder(LibrarySortOrder.TITLE))
        assertEquals(
            LibrarySortOrder.TITLE,
            viewModel.uiState.first { it.libraryDisplay.sortOrder == LibrarySortOrder.TITLE }
                .libraryDisplay.sortOrder,
        )
    }

    @Test
    fun `reading toggles write through`() = runTest {
        viewModel.onAction(SettingsAction.SetKeepScreenOn(false))
        viewModel.onAction(SettingsAction.SetVolumeKeys(true))
        val reader = viewModel.uiState.first { it.reader.volumeKeys }.reader
        assertFalse(reader.keepScreenOn)
        assertTrue(reader.volumeKeys)
    }

    @Test
    fun `reading sliders write through and persist values`() = runTest {
        viewModel.onAction(SettingsAction.SetFontSize(1.25f))
        assertEquals(1.25f, preferences.readerPreferences.first { it.fontScale == 1.25f }.fontScale)
        viewModel.onAction(SettingsAction.SetMargins(1.8f))
        assertEquals(1.8f, preferences.readerPreferences.first { it.pageMargins == 1.8f }.pageMargins)
        viewModel.onAction(SettingsAction.SetLineHeight(2.1f))
        assertEquals(2.1f, preferences.readerPreferences.first { it.lineHeight == 2.1f }.lineHeight)
    }

    @Test
    fun `rapid slider ticks coalesce to the last value`() = runTest {
        viewModel.onAction(SettingsAction.SetFontSize(1.1f))
        viewModel.onAction(SettingsAction.SetFontSize(1.2f))
        viewModel.onAction(SettingsAction.SetFontSize(1.3f))

        // Superseded ticks never reach DataStore: only the settle persists.
        assertEquals(1.3f, preferences.readerPreferences.first { it.fontScale == 1.3f }.fontScale)
    }

    @Test
    fun `crash reporting toggle persists and records consent`() = runTest {
        viewModel.onAction(SettingsAction.SetCrashReporting(true))
        assertTrue(viewModel.uiState.first { it.crashReportingEnabled }.crashReportingEnabled)
        assertTrue(preferences.crashReportingAsked.first())
    }

    @Test
    fun `revoked folders surface with live grants hidden`() = runTest {
        val grants = FakeSafGrantStore(mutableSetOf("content://trees/kept"))
        val scoped = SettingsViewModel(preferences, grants, backup, sync, driveSignIn)
        preferences.addLinkedFolder("content://trees/kept")
        preferences.addLinkedFolder("content://trees/lost")

        val state = scoped.uiState.first { it.revokedFolders == setOf("content://trees/lost") }
        assertEquals(setOf("content://trees/lost"), state.revokedFolders)
    }

    @Test
    fun `drive support follows the sign-in capability`() {
        assertTrue(viewModel.driveSupported)
        assertNotNull(viewModel.driveSignInIntent())

        val unsupported = SettingsViewModel(
            preferences,
            FakeSafGrantStore(),
            backup,
            sync,
            FakeDriveSignIn(isSupported = false),
        )
        assertFalse(unsupported.driveSupported)
        assertNull(unsupported.driveSignInIntent())
    }

    @Test
    fun `signing out clears the client and the stored account`() = runTest {
        viewModel.onDriveSignedIn("user@example.com")
        assertEquals("user@example.com", preferences.driveAccount.first())

        viewModel.onDriveSignOut()

        assertNull(preferences.driveAccount.first())
        assertEquals(1, driveSignIn.signOuts)
    }

    @Test
    fun `forgetting a folder releases its grant and drops it`() = runTest {
        val grants = FakeSafGrantStore(mutableSetOf("content://trees/old"))
        val scoped = SettingsViewModel(preferences, grants, backup, sync, driveSignIn)
        preferences.addLinkedFolder("content://trees/old")

        scoped.onAction(SettingsAction.RemoveLinkedFolder("content://trees/old"))

        assertEquals(listOf("content://trees/old"), grants.released)
        assertTrue(preferences.linkedFolders.first().isEmpty())
    }
}
