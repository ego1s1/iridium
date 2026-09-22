package com.iridium.feature.reader.api

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data class ReaderRoute(val bookId: String)

fun NavController.navigateToReader(bookId: String) {
    navigate(ReaderRoute(bookId)) {
        launchSingleTop = true
    }
}
