package com.iridium.feature.settings.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.iridium.core.data.DriveSyncManager
import com.iridium.core.designsystem.IridiumAlertDialog
import com.iridium.core.designsystem.rememberLinkedFolderPicker
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumSectionCard
import com.iridium.core.designsystem.IridiumSettingRow
import com.iridium.core.designsystem.IridiumSettingSwitch
import com.iridium.core.designsystem.IridiumSliderRow
import com.iridium.core.designsystem.IridiumTonalButton
import com.iridium.core.designsystem.LocalAppFonts
import com.iridium.core.designsystem.SchemePickerRow
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.model.LibrarySortOrder
import com.iridium.core.model.MotionStyle
import kotlinx.coroutines.launch
import com.iridium.core.model.ThemeMode

/** Public tab content for the main viewport. */
@Composable
internal fun StorageSection(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onAddFolder: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    driveAccount: String?,
    onDriveSignIn: () -> Unit,
    onDriveSignOut: () -> Unit,
    onSyncNow: () -> Unit,
) {
        IridiumSectionCard(title = stringResource(R.string.settings_card_storage)) {
            val folderCount = state.linkedFolders.size
            IridiumSettingRow(
                title = stringResource(R.string.settings_storage_location_title),
                subtitle = if (folderCount == 0) {
                    stringResource(R.string.settings_storage_location_subtitle)
                } else {
                    pluralStringResource(
                        R.plurals.settings_storage_linked,
                        folderCount,
                        folderCount,
                    )
                },
                onClick = onAddFolder,
            )
            state.revokedFolders.forEach { uri ->
                IridiumSettingRow(
                    title = stringResource(R.string.settings_storage_revoked_title),
                    subtitle = stringResource(
                        R.string.settings_storage_revoked_body,
                        displayNameForTreeUri(uri),
                    ),
                    onClick = { onAction(SettingsAction.RemoveLinkedFolder(uri)) },
                )
            }
        }
        IridiumSectionCard(title = stringResource(R.string.settings_backup_title)) {
            IridiumSettingRow(
                title = stringResource(R.string.settings_backup_export),
                subtitle = stringResource(R.string.settings_backup_export_body),
                icon = com.iridium.core.designsystem.IridiumIcons.Share,
                onClick = onExportBackup,
            )
            IridiumSettingRow(
                title = stringResource(R.string.settings_backup_import),
                subtitle = stringResource(R.string.settings_backup_import_body),
                icon = com.iridium.core.designsystem.IridiumIcons.ImportFile,
                onClick = onImportBackup,
            )
            if (driveAccount == null) {
                IridiumSettingRow(
                    title = stringResource(R.string.settings_backup_sign_in),
                    subtitle = stringResource(R.string.settings_backup_sign_in_body),
                    icon = com.iridium.core.designsystem.IridiumIcons.Storage,
                    onClick = onDriveSignIn,
                )
            } else {
                IridiumSettingRow(
                    title = stringResource(R.string.settings_backup_sync),
                    subtitle = driveAccount,
                    icon = com.iridium.core.designsystem.IridiumIcons.Refresh,
                    onClick = onSyncNow,
                )
                IridiumSettingRow(
                    title = stringResource(R.string.settings_backup_sign_out),
                    subtitle = driveAccount,
                    icon = com.iridium.core.designsystem.IridiumIcons.Close,
                    onClick = onDriveSignOut,
                )
            }
        }
}


/** Human label for a SAF tree URI: the folder name after the volume prefix. */
internal fun displayNameForTreeUri(uri: String): String {
    val last = uri.substringAfterLast('/')
    val folder = last.substringAfter("%3A", last).substringAfter(':', last)
    return runCatching { java.net.URLDecoder.decode(folder, "UTF-8") }.getOrDefault(folder)
}

