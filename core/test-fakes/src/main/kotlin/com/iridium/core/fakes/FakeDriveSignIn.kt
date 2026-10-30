package com.iridium.core.fakes

import android.content.Intent
import com.iridium.core.data.DriveSignIn
import com.iridium.core.data.DriveSyncManager

/**
 * Fake account plumbing: [supported] lets a test stand in for a channel that
 * ships no Drive, and [email] is what the picker "returns".
 */
class FakeDriveSignIn(
    override val isSupported: Boolean = true,
    var email: String? = "user@example.com",
) : DriveSignIn {

    var signOuts = 0
        private set

    override val appDataScope: String = DriveSyncManager.APPDATA_SCOPE

    override fun signInIntent(): Intent = Intent(ACTION_PICKER)

    override fun emailFromResult(data: Intent?): String? = if (data == null) null else email

    override fun signOut() {
        signOuts++
    }

    private companion object {
        const val ACTION_PICKER = "com.iridium.test.ACTION_PICK_ACCOUNT"
    }
}
