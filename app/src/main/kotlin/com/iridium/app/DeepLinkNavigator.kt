package com.iridium.app

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bridges Activity intent handling and Compose navigation: MainActivity
 * emits imported book ids here; IridiumApp collects and navigates to the
 * reader. A replay-1 buffer survives the collect-lag on cold start.
 */
@Singleton
class DeepLinkNavigator @Inject constructor() {
    private val _openBook = MutableSharedFlow<String>(replay = 1)
    val openBook: SharedFlow<String> = _openBook.asSharedFlow()

    suspend fun openBook(bookId: String) {
        _openBook.emit(bookId)
    }
}
