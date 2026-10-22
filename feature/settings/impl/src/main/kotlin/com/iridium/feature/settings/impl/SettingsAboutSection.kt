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
internal fun AboutSection(
    appVersion: String,
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onLicensesClick: () -> Unit,
) {
        IridiumSectionCard(title = stringResource(R.string.settings_card_about)) {
            AboutHero(appVersion = appVersion)
            AboutLinkRows(
                onLicensesClick = onLicensesClick,
                crashReportingEnabled = state.crashReportingEnabled,
                onAction = onAction,
            )
        }
}

/** Brand card plus creator attribution, mirroring the reference About hero. */
@Composable
internal fun AboutHero(
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = rememberIridiumHaptics()
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Icon(
                        imageVector = IridiumIcons.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_about_app),
                            style = IridiumEmphasized.headlineMedium.copy(
                                fontFamily = LocalAppFonts.current.displaySoft,
                                fontWeight = FontWeight.Black,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(
                                    imageVector = IridiumIcons.Sparkle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = stringResource(
                                        R.string.settings_about_version,
                                        appVersion,
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = LocalAppFonts.current.topBarTitle,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_about_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = stringResource(R.string.settings_about_handle),
                        role = Role.Button,
                        onClick = {
                            haptics(IridiumHaptic.Select)
                            context.openUrl(AboutLinks.DEVELOPER)
                        },
                    ),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_about_by),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Image(
                        painter = painterResource(R.drawable.dev_avatar),
                        contentDescription = stringResource(R.string.settings_about_developer),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_about_developer),
                            style = IridiumEmphasized.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text = stringResource(R.string.settings_about_handle),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = LocalAppFonts.current.topBarTitle,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_github_mark),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
/**
 * About link rows plus the crash-reporting switch. The licenses row keeps the
 * existing [onLicensesClick] wiring intact.
 */
@Composable
internal fun AboutLinkRows(
    onLicensesClick: () -> Unit,
    crashReportingEnabled: Boolean,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = rememberIridiumHaptics()
    var privacyOpen by remember { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        IridiumSettingRow(
            title = stringResource(R.string.settings_about_github),
            subtitle = stringResource(R.string.settings_about_github_subtitle),
            icon = IridiumIcons.Code,
            onClick = {
                haptics(IridiumHaptic.Select)
                context.openUrl(AboutLinks.REPOSITORY)
            },
            trailing = {
                Icon(
                    imageVector = IridiumIcons.Forward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        IridiumSettingRow(
            title = stringResource(R.string.settings_about_issue),
            subtitle = stringResource(R.string.settings_about_issue_subtitle),
            icon = IridiumIcons.BugReport,
            onClick = {
                haptics(IridiumHaptic.Select)
                context.openUrl(AboutLinks.ISSUES)
            },
            trailing = {
                Icon(
                    imageVector = IridiumIcons.Forward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        IridiumSettingRow(
            title = stringResource(R.string.settings_about_privacy),
            subtitle = stringResource(R.string.settings_about_privacy_subtitle),
            icon = IridiumIcons.PrivacyLock,
            onClick = {
                haptics(IridiumHaptic.Select)
                privacyOpen = true
            },
            trailing = {
                Icon(
                    imageVector = IridiumIcons.Forward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        IridiumSettingRow(
            title = stringResource(R.string.settings_about_licenses),
            subtitle = stringResource(R.string.settings_about_licenses_subtitle),
            icon = IridiumIcons.Info,
            onClick = {
                haptics(IridiumHaptic.Select)
                onLicensesClick()
            },
            trailing = {
                Icon(
                    imageVector = IridiumIcons.Forward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        IridiumSettingSwitch(
            title = stringResource(R.string.settings_crash_title),
            subtitle = stringResource(R.string.settings_crash_subtitle),
            checked = crashReportingEnabled,
            onCheckedChange = { onAction(SettingsAction.SetCrashReporting(it)) },
        )
    }
    if (privacyOpen) {
        IridiumAlertDialog(
            onDismissRequest = { privacyOpen = false },
            icon = IridiumIcons.PrivacyLock,
            title = {
                Text(
                    text = stringResource(R.string.settings_about_privacy),
                    style = IridiumEmphasized.headlineSmall.copy(
                        fontFamily = LocalAppFonts.current.displaySoft,
                        fontWeight = FontWeight.Black,
                    ),
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_about_privacy_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                IridiumTonalButton(onClick = { privacyOpen = false }) {
                    Text(stringResource(R.string.settings_dialog_understood))
                }
            },
        )
    }
}
/** External destinations for the About rows. */
internal object AboutLinks {
    const val REPOSITORY = "https://github.com/ego1s1/iridium"
    const val ISSUES = "https://github.com/ego1s1/iridium/issues"
    const val DEVELOPER = "https://github.com/ego1s1"
}
internal fun Context.openUrl(url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
