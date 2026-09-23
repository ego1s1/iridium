@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.iridium.app

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.iridium.core.designsystem.IridiumLoading
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.designsystem.LocalExpressiveMotionEnabled
import com.iridium.core.designsystem.LocalSharedTransitionScope
import com.iridium.core.designsystem.rememberSystemReduceMotion
import com.iridium.core.designsystem.resolveExpressiveMotionEnabled
import com.iridium.core.designsystem.screenEnter
import com.iridium.core.designsystem.screenExit
import com.iridium.core.designsystem.screenPopEnter
import com.iridium.core.designsystem.screenPopExit
import com.iridium.core.model.ThemeMode
import com.iridium.feature.detail.api.navigateToDetail
import com.iridium.feature.detail.impl.detailScreen
import com.iridium.feature.onboarding.api.OnboardingRoute
import com.iridium.feature.onboarding.impl.onboardingScreen
import com.iridium.feature.reader.api.navigateToReader
import com.iridium.feature.reader.impl.readerScreen

/**
 * App entry point: theme + top-level navigation. Onboarding shows until the
 * user finishes (or skips); afterwards Main hosts Library/History/Settings.
 */
@Composable
fun IridiumApp(
    modifier: Modifier = Modifier,
    viewModel: IridiumAppViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val completed by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val systemReduceMotion = rememberSystemReduceMotion()

    val darkTheme = when (state.theme.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val expressiveMotion = resolveExpressiveMotionEnabled(state.motionStyle, systemReduceMotion)

    IridiumTheme(
        darkTheme = darkTheme,
        dynamicColor = state.theme.dynamicColor,
        colorScheme = state.theme.colorScheme,
        amoled = state.theme.amoled,
        expressiveMotion = expressiveMotion,
    ) {
        CompositionLocalProvider(LocalExpressiveMotionEnabled provides expressiveMotion) {
            Surface(modifier = modifier.fillMaxSize()) {
                // Null until DataStore's first emission: never flash the wizard.
                if (completed == null) {
                    IridiumLoading()
                    return@Surface
                }
                val navController = rememberNavController()
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        NavHost(
                            navController = navController,
                            startDestination = if (completed == true) MainRoute else OnboardingRoute,
                            enterTransition = { screenEnter() },
                            exitTransition = { screenExit() },
                            popEnterTransition = { screenPopEnter() },
                            popExitTransition = { screenPopExit() },
                        ) {
                            onboardingScreen(
                                onOnboardingComplete = { navController.navigateToMain() },
                            )
                            mainScreen(
                                onReadClick = { navController.navigateToDetail(it) },
                                onOpenChapter = { bookId, href ->
                                    navController.navigateToReader(bookId, href)
                                },
                                onBookLongClick = { navController.navigateToDetail(it) },
                                appVersion = BuildConfig.VERSION_NAME,
                            )
                            detailScreen(
                                onBackClick = { navController.popBackStack() },
                                onReadClick = { navController.navigateToReader(it) },
                            )
                            readerScreen(
                                onBackClick = { navController.popBackStack() },
                            )
                        }
                    }
                }

                // Consent-first crash reporting prompts, only once onboarding is
                // behind us so they never compete with the wizard.
                CrashPrompts(
                    asked = state.crashConsentAsked,
                    enabled = state.crashReportingEnabled,
                    hasPendingReport = state.hasPendingCrashReport,
                    onEnable = { viewModel.setCrashReporting(true) },
                    onDecline = { viewModel.dismissCrashConsent() },
                    onShared = { viewModel.markCrashReportShared() },
                    onDiscard = { viewModel.discardCrashReports() },
                    reportText = { viewModel.crashReportText() },
                )
            }
        }
    }
}

@Suppress("unused")
private fun NavDestination.isOnboarding(): Boolean =
    route?.substringAfterLast('.') == "OnboardingRoute"
