package com.iridium.feature.onboarding.impl

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
    fun `get started advances to the access step`() = runTest {
        viewModel.onAction(OnboardingAction.GetStarted)
        assertTrue(viewModel.uiState.first() is OnboardingUiState.Access)
    }

    @Test
    fun `advance walks access reading appearance`() = runTest {
        viewModel.onAction(OnboardingAction.GetStarted)
        viewModel.onAction(OnboardingAction.Advance)
        val reading = viewModel.uiState.first()
        assertTrue(reading is OnboardingUiState.Reading)
        assertEquals(1f, (reading as OnboardingUiState.Reading).prefs.fontScale)
        viewModel.onAction(OnboardingAction.Advance)
        assertTrue(viewModel.uiState.first() is OnboardingUiState.Appearance)
    }

    @Test
    fun `reading choices persist immediately`() = runTest {
        viewModel.onAction(OnboardingAction.SetFontScale(1.5f))
        viewModel.onAction(OnboardingAction.SetLineHeight(2f))
        viewModel.onAction(OnboardingAction.SetReaderTheme(com.iridium.core.model.ColorSchemeChoice.DARK))

        val reader = preferences.readerPreferences.first()
        assertEquals(1.5f, reader.fontScale)
        assertEquals(2f, reader.lineHeight)
        assertEquals(com.iridium.core.model.ColorSchemeChoice.DARK, reader.theme)
    }

    @Test
    fun `font scale clamps to the readable range`() = runTest {
        viewModel.onAction(OnboardingAction.SetFontScale(99f))
        assertEquals(3f, preferences.readerPreferences.first().fontScale)
        viewModel.onAction(OnboardingAction.SetLineHeight(99f))
        assertEquals(2.5f, preferences.readerPreferences.first().lineHeight)
    }

    @Test
    fun `back steps through the wizard`() = runTest {
        viewModel.onAction(OnboardingAction.GetStarted)
        viewModel.onAction(OnboardingAction.Advance)
        viewModel.onAction(OnboardingAction.Advance)
        viewModel.onAction(OnboardingAction.BackStep)
        assertTrue(viewModel.uiState.first() is OnboardingUiState.Reading)

        viewModel.onAction(OnboardingAction.BackStep)
        assertTrue(viewModel.uiState.first() is OnboardingUiState.Access)

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
    fun `skip finishes from anywhere`() = runTest {
        viewModel.onAction(OnboardingAction.Skip)

        assertTrue(preferences.onboardingCompleted.first())
    }

    @Test
    fun `finish marks onboarding complete`() = runTest {
        viewModel.onAction(OnboardingAction.Finish)
        assertTrue(preferences.onboardingCompleted.first())
    }
}
