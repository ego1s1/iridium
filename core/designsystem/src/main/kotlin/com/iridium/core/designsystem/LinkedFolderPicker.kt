package com.iridium.core.designsystem

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Shared SAF folder picker: launches the system folder chooser, persists
 * read permission on the chosen tree, and reports its URI string. One
 * implementation for onboarding, library, and settings — the ViewModels all
 * persist through `IridiumPreferencesDataSource.addLinkedFolder` and the
 * lister independently filters against persisted permissions, so revoked
 * trees never fail a scan.
 *
 * The launcher itself is intentionally untested (Robolectric cannot drive
 * SAF): keep this a thin delegate and cover the persistence + ViewModel
 * wiring instead.
 */
@Composable
fun rememberLinkedFolderPicker(onFolderPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        // Only record trees we actually hold: a failed take would otherwise
        // guarantee failing scans for a folder that was never granted.
        if (uri != null && context.persistSafReadPermission(uri)) {
            onFolderPicked(uri.toString())
        }
    }
    return { launcher.launch(null) }
}

/**
 * Persists SAF read permission on a picked tree. Returns whether the grant
 * holds (failures are swallowed: the lister independently skips trees
 * without persisted permission).
 */
fun android.content.Context.persistSafReadPermission(uri: Uri): Boolean =
    runCatching {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        true
    }.getOrDefault(false)
