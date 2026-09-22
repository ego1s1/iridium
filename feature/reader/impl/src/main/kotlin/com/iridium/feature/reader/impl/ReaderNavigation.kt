package com.iridium.feature.reader.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.iridium.feature.reader.api.ReaderRoute

fun NavGraphBuilder.readerScreen(onBackClick: () -> Unit) {
    composable<ReaderRoute> { entry ->
        val route = entry.toRoute<ReaderRoute>()
        ReaderRoute(bookId = route.bookId, onBackClick = onBackClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderRoute(bookId: String, onBackClick: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Reader") }) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            // Readium EpubNavigator lands here in Phase 3 (paged/scrolled + settings sheet).
            Text("Book: $bookId")
        }
    }
}
