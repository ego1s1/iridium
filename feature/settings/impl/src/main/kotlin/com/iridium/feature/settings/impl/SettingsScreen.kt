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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iridium.core.designsystem.IridiumAlertDialog
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
import com.iridium.core.model.ThemeMode

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
    val context = LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.onAction(SettingsAction.AddLinkedFolder(uri.toString()))
        }
    }
    SettingsScreen(
        state = state,
        appVersion = appVersion,
        onAction = viewModel::onAction,
        onLicensesClick = onLicensesClick,
        onAddFolder = { folderPicker.launch(null) },
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
) {
    Scaffold(
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
            IridiumSectionCard(title = stringResource(R.string.settings_card_appearance)) {
                OptionLabel(stringResource(R.string.settings_theme))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ThemeChoice(
                        selected = state.theme.mode == ThemeMode.SYSTEM,
                        onClick = { onAction(SettingsAction.SetThemeMode(ThemeMode.SYSTEM)) },
                        icon = IridiumIcons.Contrast,
                        label = stringResource(R.string.settings_theme_system),
                    )
                    ThemeChoice(
                        selected = state.theme.mode == ThemeMode.LIGHT,
                        onClick = { onAction(SettingsAction.SetThemeMode(ThemeMode.LIGHT)) },
                        icon = IridiumIcons.LightMode,
                        label = stringResource(R.string.settings_theme_light),
                    )
                    ThemeChoice(
                        selected = state.theme.mode == ThemeMode.DARK,
                        onClick = { onAction(SettingsAction.SetThemeMode(ThemeMode.DARK)) },
                        icon = IridiumIcons.DarkMode,
                        label = stringResource(R.string.settings_theme_dark),
                    )
                }
                Spacer(Modifier.height(8.dp))
                IridiumSettingSwitch(
                    title = stringResource(R.string.settings_wallpaper_title),
                    subtitle = stringResource(R.string.settings_wallpaper_subtitle),
                    checked = state.theme.dynamicColor,
                    onCheckedChange = { onAction(SettingsAction.SetDynamicColor(it)) },
                )
                IridiumSettingSwitch(
                    title = stringResource(R.string.settings_haptics_title),
                    subtitle = stringResource(R.string.settings_haptics_subtitle),
                    checked = state.theme.hapticsEnabled,
                    onCheckedChange = { onAction(SettingsAction.SetHapticsEnabled(it)) },
                    icon = IridiumIcons.Vibration,
                )
                IridiumSettingSwitch(
                    title = stringResource(R.string.settings_amoled_title),
                    subtitle = stringResource(R.string.settings_amoled_subtitle),
                    checked = state.theme.amoled,
                    onCheckedChange = { onAction(SettingsAction.SetAmoled(it)) },
                    // Pure black only reads on a dark canvas: explicit LIGHT
                    // never allows it; SYSTEM follows the system theme.
                    enabled = state.theme.mode == ThemeMode.DARK ||
                        (state.theme.mode == ThemeMode.SYSTEM && isSystemInDarkTheme()),
                )
                Spacer(Modifier.height(8.dp))
                OptionLabel(stringResource(R.string.settings_colors))
                SchemePickerRow(
                    theme = state.theme,
                    onDynamic = { onAction(SettingsAction.SetDynamicColor(true)) },
                    onScheme = { onAction(SettingsAction.SetColorScheme(it)) },
                )
            }

            IridiumSectionCard(title = stringResource(R.string.settings_card_motion)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ThemeChoice(
                        selected = state.motionStyle == MotionStyle.EXPRESSIVE,
                        onClick = {
                            onAction(SettingsAction.SetMotionStyle(MotionStyle.EXPRESSIVE))
                        },
                        icon = IridiumIcons.Animation,
                        label = stringResource(R.string.settings_motion_expressive),
                    )
                    ThemeChoice(
                        selected = state.motionStyle == MotionStyle.CALM,
                        onClick = {
                            onAction(SettingsAction.SetMotionStyle(MotionStyle.CALM))
                        },
                        icon = IridiumIcons.Spa,
                        label = stringResource(R.string.settings_motion_calm),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.settings_motion_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

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

            IridiumSectionCard(title = stringResource(R.string.settings_card_reading)) {
                IridiumSliderRow(
                    label = stringResource(R.string.settings_reading_font_size),
                    value = state.reader.fontScale,
                    valueRange = 0.5f..2.5f,
                    valueText = "${(state.reader.fontScale * 100).toInt()}%",
                    onValueChange = { onAction(SettingsAction.SetFontSize(it)) },
                )
                Spacer(Modifier.height(8.dp))
                IridiumSliderRow(
                    label = stringResource(R.string.settings_reading_margins),
                    value = state.reader.pageMargins,
                    valueRange = 0.5f..3.0f,
                    valueText = String.format(
                        java.util.Locale.US,
                        "%.1fx",
                        state.reader.pageMargins,
                    ),
                    onValueChange = { onAction(SettingsAction.SetMargins(it)) },
                )
                Spacer(Modifier.height(8.dp))
                IridiumSliderRow(
                    label = stringResource(R.string.settings_reading_line_height),
                    value = state.reader.lineHeight,
                    valueRange = 1.0f..2.5f,
                    valueText = String.format(
                        java.util.Locale.US,
                        "%.2f",
                        state.reader.lineHeight,
                    ),
                    onValueChange = { onAction(SettingsAction.SetLineHeight(it)) },
                )
                Spacer(Modifier.height(8.dp))
                IridiumSettingSwitch(
                    title = stringResource(R.string.settings_reading_keep_screen_on),
                    checked = state.reader.keepScreenOn,
                    onCheckedChange = { onAction(SettingsAction.SetKeepScreenOn(it)) },
                )
                IridiumSettingSwitch(
                    title = stringResource(R.string.settings_reading_page_counter),
                    checked = state.reader.showPageCounter,
                    onCheckedChange = { onAction(SettingsAction.SetShowPageCounter(it)) },
                )
                IridiumSettingSwitch(
                    title = stringResource(R.string.settings_reading_volume_keys),
                    checked = state.reader.volumeKeys,
                    onCheckedChange = { onAction(SettingsAction.SetVolumeKeys(it)) },
                )
            }

            IridiumSectionCard(title = stringResource(R.string.settings_card_storage)) {
                val folderCount = state.linkedFolders.size
                IridiumSettingRow(
                    title = stringResource(R.string.settings_storage_location_title),
                    subtitle = if (folderCount == 0) {
                        stringResource(R.string.settings_storage_location_subtitle)
                    } else {
                        stringResource(R.string.settings_storage_linked, folderCount)
                    },
                    onClick = onAddFolder,
                )
            }

            IridiumSectionCard(title = stringResource(R.string.settings_card_about)) {
                AboutHero(appVersion = appVersion)
                AboutLinkRows(
                    onLicensesClick = onLicensesClick,
                    crashReportingEnabled = state.crashReportingEnabled,
                    onAction = onAction,
                )
            }

            Spacer(Modifier.height(96.dp))
        }
    }
}

/** Brand card plus creator attribution, mirroring the reference About hero. */
@Composable
private fun AboutHero(
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
private fun AboutLinkRows(
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

/** Small section label inside a card, heading-marked for accessibility. */
@Composable
private fun OptionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = IridiumEmphasized.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() },
    )
}

/**
 * One pill in a single-choice row, with an optional leading icon. Plain
 * [ToggleButton] with default shapes: custom asymmetric container shapes
 * break tap hit-testing under Robolectric, so the morphing group stays out
 * until the design-system component is test-safe.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.ThemeChoice(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector? = null,
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { checked -> if (checked != selected) onClick() },
        modifier = Modifier.weight(1f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

/** External destinations for the About rows. */
private object AboutLinks {
    const val REPOSITORY = "https://github.com/ego1s1/iridium"
    const val ISSUES = "https://github.com/ego1s1/iridium/issues"
    const val DEVELOPER = "https://github.com/ego1s1"
}

private fun Context.openUrl(url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
