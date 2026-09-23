package com.iridium.feature.settings.impl

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val preferences = TestPreferencesDataSource()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        viewModel = SettingsViewModel(preferences)
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
    fun `crash reporting toggle persists and records consent`() = runTest {
        viewModel.onAction(SettingsAction.SetCrashReporting(true))
        assertTrue(viewModel.uiState.first { it.crashReportingEnabled }.crashReportingEnabled)
        assertTrue(preferences.crashReportingAsked.first())
    }
}
