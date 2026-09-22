package com.iridium.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.feature.detail.api.DetailRoute
import com.iridium.feature.detail.api.navigateToDetail
import com.iridium.feature.detail.impl.detailScreen
import com.iridium.feature.library.impl.LibraryRoute
import com.iridium.feature.library.impl.libraryScreen
import com.iridium.feature.reader.api.ReaderRoute
import com.iridium.feature.reader.api.navigateToReader
import com.iridium.feature.reader.impl.readerScreen

@Composable
fun IridiumApp() {
    IridiumTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = LibraryRoute) {
            libraryScreen(
                onBookClick = { navController.navigateToDetail(it) },
                onBookLongClick = { navController.navigateToDetail(it) },
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
