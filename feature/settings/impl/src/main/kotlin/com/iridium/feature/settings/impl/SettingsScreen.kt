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
import androidx.compose.material3.SnackbarHostState
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

private const val MAX_BACKUP_IMPORT_CHARS = 8 * 1024 * 1024

/** Public tab content for the main viewport. */
@Composable
fun SettingsTabContent(
    appVersion: String,
    modifier: Modifier = Modifier,
    onLicensesClick: () -> Unit = {},
) {
    SettingsRoute(appVersion = appVersion, modifier = modifier, onLicensesClick = onLicensesClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsRoute(
    appVersion: String,
    modifier: Modifier = Modifier,
    onLicensesClick: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launchPicker = rememberLinkedFolderPicker {
        viewModel.onAction(SettingsAction.AddLinkedFolder(it))
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val account by viewModel.driveAccount.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching { task.result }.getOrNull()?.email?.let(viewModel::onDriveSignedIn)
    }
    syncMessage?.let { message ->
        androidx.compose.runtime.LaunchedEffect(message) {
            snackbarHost.showSnackbar(message)
            viewModel.clearSyncMessage()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            val json = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().use { reader ->
                        val capped = CharArray(MAX_BACKUP_IMPORT_CHARS)
                        val read = reader.read(capped)
                        if (read <= 0) return@launch
                        String(capped, 0, read)
                    }
                } ?: return@launch
            } catch (_: Exception) {
                snackbarHost.showSnackbar(
                    context.getString(R.string.settings_backup_import_failed),
                )
                return@launch
            }
            val report = try {
                viewModel.importSnapshot(json)
            } catch (_: Exception) {
                snackbarHost.showSnackbar(
                    context.getString(R.string.settings_backup_import_failed),
                )
                return@launch
            }
            snackbarHost.showSnackbar(
                context.getString(R.string.settings_backup_imported, report.restored),
            )
        }
    }
    SettingsScreen(
        state = state,
        appVersion = appVersion,
        snackbarHost = snackbarHost,
        onAction = viewModel::onAction,
        onLicensesClick = onLicensesClick,
        onAddFolder = launchPicker,
        onExportBackup = {
            scope.launch {
                val json = try {
                    viewModel.exportSnapshot()
                } catch (_: Exception) {
                    snackbarHost.showSnackbar(
                        context.getString(R.string.settings_backup_export_failed),
                    )
                    return@launch
                }
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "Iridium backup")
                    putExtra(Intent.EXTRA_TEXT, json)
                }
                try {
                    context.startActivity(Intent.createChooser(share, context.getString(R.string.settings_backup_export)))
                } catch (_: Exception) {
                    snackbarHost.showSnackbar(
                        context.getString(R.string.settings_backup_export_failed),
                    )
                }
            }
        },
        onImportBackup = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
        driveAccount = account,
        onDriveSignIn = {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveSyncManager.APPDATA_SCOPE))
                .build()
            val client = GoogleSignIn.getClient(context, options)
            signInLauncher.launch(client.signInIntent)
        },
        onDriveSignOut = {
            val client = GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN)
            client.signOut()
            viewModel.onDriveSignOut()
        },
        onSyncNow = viewModel::onSyncNow,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    appVersion: String,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    onLicensesClick: () -> Unit = {},
    onAddFolder: () -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    driveAccount: String? = null,
    onDriveSignIn: () -> Unit = {},
    onDriveSignOut: () -> Unit = {},
    onSyncNow: () -> Unit = {},
    snackbarHost: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = IridiumEmphasized.headlineSmall,
                    )
                },
                actions = { Icon(IridiumIcons.Settings, contentDescription = null) },
            )
        },
        modifier = modifier,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppearanceSection(state = state, onAction = onAction)
            LibrarySection(state = state, onAction = onAction)
            ReaderSection(state = state, onAction = onAction)
            StorageSection(
                state = state,
                onAction = onAction,
                onAddFolder = onAddFolder,
                onExportBackup = onExportBackup,
                onImportBackup = onImportBackup,
                driveAccount = driveAccount,
                onDriveSignIn = onDriveSignIn,
                onDriveSignOut = onDriveSignOut,
                onSyncNow = onSyncNow,
            )
            AboutSection(
                appVersion = appVersion,
                state = state,
                onAction = onAction,
                onLicensesClick = onLicensesClick,
            )

            Spacer(Modifier.height(96.dp))
        }
    }
}
