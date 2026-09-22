package com.iridium.feature.detail.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.iridium.feature.detail.api.DetailRoute

fun NavGraphBuilder.detailScreen(onBackClick: () -> Unit, onReadClick: (String) -> Unit) {
    composable<DetailRoute> { entry ->
        val route = entry.toRoute<DetailRoute>()
        DetailScreen(bookId = route.bookId, onReadClick = { onReadClick(route.bookId) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailScreen(bookId: String, onReadClick: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Details") }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Button(onClick = onReadClick) { Text("Read $bookId") }
        }
    }
}
