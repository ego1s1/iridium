package com.iridium.feature.onboarding.impl

import android.net.Uri
import com.iridium.core.fakes.TestPreferencesDataSource
import com.iridium.core.model.AppColorScheme
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val preferences = TestPreferencesDataSource()
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        viewModel = OnboardingViewModel(preferences)
    }

    @Test
    fun `starts on the welcome step`() = runTest {
        assertEquals(OnboardingUiState.Welcome, viewModel.uiState.first())
    }

    @Test
    fun `get started advances to the folder step`() = runTest {
        viewModel.onAction(OnboardingAction.GetStarted)
        assertTrue(viewModel.uiState.first() is OnboardingUiState.Folder)
    }

    @Test
    fun `a dismissed picker explains itself instead of stalling`() = runTest {
        viewModel.onAction(OnboardingAction.GetStarted)
        viewModel.onAction(OnboardingAction.FolderPickerDismissed)
        val state = viewModel.uiState.first() as OnboardingUiState.Folder
        assertTrue(state.pickerHintVisible)
    }

    @Test
    fun `picking a folder persists the tree and advances to appearance`() = runTest {
        viewModel.onAction(OnboardingAction.GetStarted)
        viewModel.onAction(OnboardingAction.FolderSelected(Uri.parse("content://tree/books")))

        assertEquals("content://tree/books", preferences.sourceTreeUri.first())
        assertTrue(viewModel.uiState.first() is OnboardingUiState.Appearance)
    }

    @Test
    fun `back steps through the wizard`() = runTest {
        viewModel.onAction(OnboardingAction.GetStarted)
        viewModel.onAction(OnboardingAction.FolderSelected(Uri.parse("content://tree")))
        viewModel.onAction(OnboardingAction.BackStep)
        assertTrue(viewModel.uiState.first() is OnboardingUiState.Folder)

        viewModel.onAction(OnboardingAction.BackStep)
        assertEquals(OnboardingUiState.Welcome, viewModel.uiState.first())
    }

    @Test
    fun `theme choices persist immediately so quitting mid-wizard keeps them`() = runTest {
        viewModel.onAction(OnboardingAction.SetThemeMode(ThemeMode.DARK))
        viewModel.onAction(OnboardingAction.SetColorScheme(AppColorScheme.OCEAN))
        viewModel.onAction(OnboardingAction.SetAmoled(true))

        val theme = preferences.themePreferences.first()
        assertEquals(ThemeMode.DARK, theme.mode)
        assertEquals(AppColorScheme.OCEAN, theme.colorScheme)
        assertFalse("a preset choice disables wallpaper colour", theme.dynamicColor)
        assertTrue(theme.amoled)
    }

    @Test
    fun `skip finishes without linking a folder`() = runTest {
        viewModel.onAction(OnboardingAction.Skip)

        assertTrue(preferences.onboardingCompleted.first())
        assertEquals(null, preferences.sourceTreeUri.first())
    }

    @Test
    fun `finish marks onboarding complete`() = runTest {
        viewModel.onAction(OnboardingAction.Finish)
        assertTrue(preferences.onboardingCompleted.first())
    }
}
