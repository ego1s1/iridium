package com.iridium.core.data

import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drive transport for builds that ship no proprietary client libraries:
 * reports itself unavailable so [DriveSyncManager] fails fast instead of
 * building a gateway that could never authenticate.
 */
@Singleton
class UnavailableDriveGatewayFactory @Inject constructor() : DriveGatewayFactory {
    override val isAvailable: Boolean = false

    override fun create(accountEmail: String): DriveGateway =
        throw UnsupportedOperationException("No remote transport in this build")
}

/**
 * No-op account plumbing for builds without a vendor sign-in client. The
 * settings UI hides the Drive rows whenever [isSupported] is false, so the
 * throwing members are never reached from the UI.
 */
@Singleton
class UnavailableDriveSignIn @Inject constructor() : DriveSignIn {
    override val isSupported: Boolean = false

    override val appDataScope: String = DriveSyncManager.APPDATA_SCOPE

    override fun signInIntent(): Intent =
        throw UnsupportedOperationException("No account picker in this build")

    override fun emailFromResult(data: Intent?): String? = null

    override fun signOut() = Unit
}
