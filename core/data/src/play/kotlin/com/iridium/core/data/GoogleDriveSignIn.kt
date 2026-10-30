package com.iridium.core.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Account picker backed by Play services: email plus the appDataFolder scope,
 * which is all the sync transport needs.
 */
@Singleton
class GoogleDriveSignIn @Inject constructor(
    @ApplicationContext private val context: Context,
) : DriveSignIn {
    override val isSupported: Boolean = true

    override val appDataScope: String = DriveSyncManager.APPDATA_SCOPE

    override fun signInIntent(): Intent = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(appDataScope))
            .build(),
    ).signInIntent

    override fun emailFromResult(data: Intent?): String? =
        runCatching { GoogleSignIn.getSignedInAccountFromIntent(data).result?.email }.getOrNull()

    override fun signOut() {
        GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut()
    }
}
