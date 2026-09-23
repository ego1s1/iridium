package com.iridium.feature.reader.api

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data class ReaderRoute(val bookId: String, val href: String? = null)

fun NavController.navigateToReader(bookId: String, href: String? = null) {
    navigate(ReaderRoute(bookId, href)) {
        launchSingleTop = true
    }
}
