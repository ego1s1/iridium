package com.iridium.feature.reader.api

import androidx.navigation.NavController
import kotlinx.serialization.Serializable

@Serializable
data class ReaderRoute(val bookId: String, val href: String? = null)

fun NavController.navigateToReader(bookId: String, href: String? = null) {
    navigate(ReaderRoute(bookId, href)) {
        // Single reader instance: opening book B from reader A replaces it
        // instead of stacking readers (Back would otherwise land on A).
        // (MainRoute lives in app and is unreachable from api, so the pop
        // anchors on the reader destination itself.)
        popUpTo<ReaderRoute> { inclusive = true }
        launchSingleTop = true
    }
}
