package com.iridium.core.data

import android.content.Intent

/**
 * Account plumbing for Drive sync, behind an interface so the sign-in client
 * stays confined to the flavors that can ship it. UI code talks to this and
 * never to a vendor SDK, and hides the Drive rows when [isSupported] is false.
 */
interface DriveSignIn {
    /** False when this build ships no Drive transport: no rows, no launcher. */
    val isSupported: Boolean

    /** OAuth scope requested at sign-in: the hidden appDataFolder only. */
    val appDataScope: String

    /** Intent that starts the account picker for [appDataScope]. */
    fun signInIntent(): Intent

    /** Email of the account chosen in a picker result, or null if cancelled. */
    fun emailFromResult(data: Intent?): String?

    /** Clears the persisted account from the sign-in client. */
    fun signOut()
}
