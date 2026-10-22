package com.iridium.feature.reader.api

import android.view.KeyEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reader-owned hardware keys.
 *
 * The reader installs a handler while it is on screen and wants volume-key
 * paging; the Activity consults it before the system does, so a handled press
 * never also changes the system volume. A single app-wide instance is enough:
 * only one reader is ever foreground, and the owning ViewModel clears its
 * handler when it is cleared.
 */
@Singleton
class ReaderKeyInterceptor @Inject constructor() {

    @Volatile
    private var handler: ((KeyEvent) -> Boolean)? = null

    /** Returns true when a handler consumed the event. */
    fun consume(event: KeyEvent): Boolean = handler?.invoke(event) == true

    fun install(handler: (KeyEvent) -> Boolean) {
        this.handler = handler
    }

    fun clear() {
        handler = null
    }
}
