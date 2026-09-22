@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.iridium.app

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.designsystem.LocalSharedTransitionScope
import com.iridium.core.designsystem.rememberSystemReduceMotion
import com.iridium.core.designsystem.resolveExpressiveMotionEnabled
import com.iridium.core.model.ThemeMode
import com.iridium.feature.detail.api.navigateToDetail
import com.iridium.feature.detail.impl.detailScreen
import com.iridium.feature.library.impl.LibraryRoute
import com.iridium.feature.library.impl.libraryScreen
import com.iridium.feature.reader.api.navigateToReader
import com.iridium.feature.reader.impl.readerScreen
import com.iridium.feature.settings.impl.SettingsRoute
import com.iridium.feature.settings.impl.settingsScreen

@Composable
fun IridiumApp(
    viewModel: IridiumAppViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
        // SharedTransitionLayout lets the book cover morph between screens;
        // the scope is exposed so feature modules can register shared elements.
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = LibraryRoute) {
                    libraryScreen(
                        onBookClick = { navController.navigateToDetail(it) },
                        onBookLongClick = { navController.navigateToDetail(it) },
                        onSettingsClick = { navController.navigate(SettingsRoute) },
                    )
                    detailScreen(
                        onBackClick = { navController.popBackStack() },
                        onReadClick = { navController.navigateToReader(it) },
                    )
                    readerScreen(
                        onBackClick = { navController.popBackStack() },
                    )
                    settingsScreen(
                        onBackClick = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
