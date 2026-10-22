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

/** Public tab content for the main viewport. */
@Composable
internal fun LibrarySection(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
) {
        IridiumSectionCard(title = stringResource(R.string.settings_card_library)) {
            Text(
                stringResource(R.string.settings_library_sort),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ThemeChoice(
                    selected = state.libraryDisplay.sortOrder ==
                        LibrarySortOrder.RECENTLY_ADDED,
                    onClick = {
                        onAction(SettingsAction.SetSortOrder(LibrarySortOrder.RECENTLY_ADDED))
                    },
                    label = stringResource(R.string.settings_library_added),
                )
                ThemeChoice(
                    selected = state.libraryDisplay.sortOrder ==
                        LibrarySortOrder.RECENTLY_READ,
                    onClick = {
                        onAction(SettingsAction.SetSortOrder(LibrarySortOrder.RECENTLY_READ))
                    },
                    label = stringResource(R.string.settings_library_read),
                )
                ThemeChoice(
                    selected = state.libraryDisplay.sortOrder == LibrarySortOrder.TITLE,
                    onClick = {
                        onAction(SettingsAction.SetSortOrder(LibrarySortOrder.TITLE))
                    },
                    label = stringResource(R.string.settings_library_title),
                )
            }
            Spacer(Modifier.height(8.dp))
            IridiumSettingSwitch(
                title = stringResource(R.string.settings_library_hide_errors),
                checked = state.libraryDisplay.hideErrors,
                onCheckedChange = { onAction(SettingsAction.SetHideErrors(it)) },
            )
        }
}
