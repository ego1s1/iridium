package com.iridium.feature.library.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

fun NavGraphBuilder.libraryScreen(onBookClick: (String) -> Unit) {
    composable<LibraryRoute> {
        LibraryScreen(onBookClick = onBookClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LibraryScreen(onBookClick: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My books") },
                actions = { IconButton(onClick = {}) { Icon(Icons.Rounded.Search, null) } },
            )
        },
        floatingActionButton = {
            // Phase 2: expand to FloatingActionButtonMenu (import file/folder).
            FloatingActionButton(onClick = {}) { Icon(Icons.Rounded.Add, null) }
        },
    ) { padding ->
        // Phase 2: LazyVerticalGrid of BookCards + continue-reading shelf + empty state.
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("Empty library — import an EPUB to begin")
        }
    }
}
