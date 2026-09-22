package com.iridium.feature.detail.api

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data class DetailRoute(val bookId: String)

fun NavController.navigateToDetail(bookId: String) {
    navigate(DetailRoute(bookId)) {
        launchSingleTop = true
    }
}
