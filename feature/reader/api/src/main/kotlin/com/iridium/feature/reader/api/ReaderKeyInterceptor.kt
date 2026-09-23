package com.iridium.feature.reader.api

import android.view.KeyEvent

/**
 * Reader-owned hardware keys.
 *
 * The reader registers a handler while it is on screen and wants volume-key
 * paging; the Activity consults it before the system does, so a handled press
 * never also changes the system volume. A single volatile slot is enough: only
 * one reader is ever foreground, and a stale handler is cleared on dispose.
 */
object ReaderKeyInterceptor {

    @Volatile
    var handler: ((KeyEvent) -> Boolean)? = null
}
