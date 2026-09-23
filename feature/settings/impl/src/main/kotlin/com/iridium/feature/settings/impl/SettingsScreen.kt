package com.iridium.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumSectionCard
import com.iridium.core.designsystem.IridiumSettingRow
import com.iridium.core.designsystem.IridiumSettingSwitch
import com.iridium.core.designsystem.SchemePickerRow
import com.iridium.core.model.LibraryFilter
import com.iridium.core.model.LibrarySortOrder
import com.iridium.core.model.MotionStyle
import com.iridium.core.model.ThemeMode

/** Public tab content for the main viewport. */
@Composable
fun SettingsTabContent(
    appVersion: String,
    modifier: Modifier = Modifier,
) {
    SettingsRoute(appVersion = appVersion, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsRoute(
    appVersion: String,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        appVersion = appVersion,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    appVersion: String,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = IridiumEmphasized.headlineSmall) },
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
            IridiumSectionCard(title = "Appearance") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.theme.mode == mode,
                            onClick = { onAction(SettingsAction.SetThemeMode(mode)) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemeMode.entries.size,
                            ),
                            label = {
                                Text(
                                    mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                IridiumSettingSwitch(
                    title = "Wallpaper color",
                    subtitle = "Match the system palette (Android 12+)",
                    checked = state.theme.dynamicColor,
                    onCheckedChange = { onAction(SettingsAction.SetDynamicColor(it)) },
                )
                IridiumSettingSwitch(
                    title = "Pure black",
                    subtitle = "True-black surfaces for AMOLED screens",
                    checked = state.theme.amoled,
                    onCheckedChange = { onAction(SettingsAction.SetAmoled(it)) },
                    enabled = state.theme.mode != ThemeMode.LIGHT,
                )
                Spacer(Modifier.height(8.dp))
                SchemePickerRow(
                    theme = state.theme,
                    onDynamic = { onAction(SettingsAction.SetDynamicColor(true)) },
                    onScheme = { onAction(SettingsAction.SetColorScheme(it)) },
                )
            }

            IridiumSectionCard(title = "Motion") {
                ButtonGroup(modifier = Modifier.fillMaxWidth()) {
                    ToggleButton(
                        checked = state.motionStyle == MotionStyle.EXPRESSIVE,
                        onCheckedChange = { onAction(SettingsAction.SetMotionStyle(MotionStyle.EXPRESSIVE)) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Expressive") }
                    ToggleButton(
                        checked = state.motionStyle == MotionStyle.CALM,
                        onCheckedChange = { onAction(SettingsAction.SetMotionStyle(MotionStyle.CALM)) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Calm") }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Calm uses plain fades; Expressive uses spring physics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IridiumSectionCard(title = "Library") {
                Text("Sort by", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                ButtonGroup(modifier = Modifier.fillMaxWidth()) {
                    ToggleButton(
                        checked = state.libraryDisplay.sortOrder == LibrarySortOrder.RECENTLY_ADDED,
                        onCheckedChange = { onAction(SettingsAction.SetSortOrder(LibrarySortOrder.RECENTLY_ADDED)) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Added", style = MaterialTheme.typography.labelSmall) }
                    ToggleButton(
                        checked = state.libraryDisplay.sortOrder == LibrarySortOrder.RECENTLY_READ,
                        onCheckedChange = { onAction(SettingsAction.SetSortOrder(LibrarySortOrder.RECENTLY_READ)) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Read", style = MaterialTheme.typography.labelSmall) }
                    ToggleButton(
                        checked = state.libraryDisplay.sortOrder == LibrarySortOrder.TITLE,
                        onCheckedChange = { onAction(SettingsAction.SetSortOrder(LibrarySortOrder.TITLE)) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Title", style = MaterialTheme.typography.labelSmall) }
                }
                Spacer(Modifier.height(8.dp))
                IridiumSettingSwitch(
                    title = "Hide unreadable books",
                    checked = state.libraryDisplay.hideErrors,
                    onCheckedChange = { onAction(SettingsAction.SetHideErrors(it)) },
                )
            }

            IridiumSectionCard(title = "Reading") {
                IridiumSettingSwitch(
                    title = "Keep screen on",
                    checked = state.reader.keepScreenOn,
                    onCheckedChange = { onAction(SettingsAction.SetKeepScreenOn(it)) },
                )
                IridiumSettingSwitch(
                    title = "Show page counter",
                    checked = state.reader.showPageCounter,
                    onCheckedChange = { onAction(SettingsAction.SetShowPageCounter(it)) },
                )
                IridiumSettingSwitch(
                    title = "Volume keys turn pages",
                    checked = state.reader.volumeKeys,
                    onCheckedChange = { onAction(SettingsAction.SetVolumeKeys(it)) },
                )
            }

            IridiumSectionCard(title = "About") {
                IridiumSettingRow(
                    title = "Version",
                    subtitle = appVersion,
                    onClick = {},
                )
                HorizontalDivider()
                IridiumSettingSwitch(
                    title = "Send crash reports",
                    subtitle = "Off by default. Reports stay on device until you share them.",
                    checked = state.crashReportingEnabled,
                    onCheckedChange = { onAction(SettingsAction.SetCrashReporting(it)) },
                )
                HorizontalDivider()
                IridiumSettingRow(
                    title = "Storage",
                    subtitle = "Books are read in place. Only covers and reading state are stored on device.",
                    onClick = {},
                )
            }

            Spacer(Modifier.height(96.dp))
        }
    }
}
