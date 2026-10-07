package com.iridium.feature.reader.impl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumSettingSwitch
import com.iridium.core.designsystem.IridiumSheet
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ReadingFlow

/** Test tags for the prefs rows in [ReaderChromeSettingsContent]. */
internal object ReaderChromeSettingsSwitchTags {
    const val KeepScreenOn = "chromeKeepScreenOnSwitch"
    const val VolumeKeys = "chromeVolumeKeysSwitch"
    const val InvertVolumeKeys = "chromeInvertVolumeKeysSwitch"
    const val PageCounter = "chromePageCounterSwitch"
}

/**
 * Mori reader-settings parity sheet for Iridium (UI hierarchy.md §3.3).
 *
 * Sections: reading direction, page fit, margin crop, tap-zone choice cards,
 * and the EPUB flow adaptation (paged / scrolled). The sheet body is split
 * into [ReaderChromeSettingsContent] so unit tests can render it directly —
 * the modal presentation does not settle under Robolectric.
 */
@Composable
fun ReaderChromeSettingsSheet(
    config: ReaderChromeConfig,
    onDirectionChange: (ChromeReadingDirection) -> Unit,
    onFitChange: (ChromePageFit) -> Unit,
    onCropChange: (Boolean) -> Unit,
    onNavModeChange: (ChromeNavMode) -> Unit,
    onFlowChange: (ReadingFlow) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    prefs: ReaderPreferences = ReaderPreferences(),
    onAction: (ReaderAction) -> Unit = {},
) {
    IridiumSheet(
        onDismiss = onDismiss,
        modifier = modifier.testTag(ReaderChromeTestTags.SettingsSheet),
    ) {
        ReaderChromeSettingsContent(
            config = config,
            onDirectionChange = onDirectionChange,
            onFitChange = onFitChange,
            onCropChange = onCropChange,
            onNavModeChange = onNavModeChange,
            onFlowChange = onFlowChange,
            prefs = prefs,
            onAction = onAction,
        )
    }
}

/** Sheet body content, exposed for testing. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReaderChromeSettingsContent(
    config: ReaderChromeConfig,
    onDirectionChange: (ChromeReadingDirection) -> Unit,
    onFitChange: (ChromePageFit) -> Unit,
    onCropChange: (Boolean) -> Unit,
    onNavModeChange: (ChromeNavMode) -> Unit,
    onFlowChange: (ReadingFlow) -> Unit,
    modifier: Modifier = Modifier,
    prefs: ReaderPreferences = ReaderPreferences(),
    onAction: (ReaderAction) -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = "Reading settings",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )

        Text(
            text = "Flow",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Paged turns like a book; scrolled flows as one column.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ButtonGroup(modifier = Modifier.fillMaxWidth()) {
            ToggleButton(
                checked = config.flow == ReadingFlow.PAGED,
                onCheckedChange = { if (it) onFlowChange(ReadingFlow.PAGED) },
                modifier = Modifier.weight(1f).testTag(ReaderChromeTestTags.FlowPaged),
            ) { Text(stringResource(R.string.reader_settings_flow_paged)) }
            ToggleButton(
                checked = config.flow == ReadingFlow.SCROLLED,
                onCheckedChange = { if (it) onFlowChange(ReadingFlow.SCROLLED) },
                modifier = Modifier.weight(1f).testTag(ReaderChromeTestTags.FlowScrolled),
            ) { Text(stringResource(R.string.reader_settings_flow_scrolled)) }
        }

        Text(
            text = "Reading direction",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        ButtonGroup(modifier = Modifier.fillMaxWidth()) {
            ToggleButton(
                checked = config.direction == ChromeReadingDirection.LEFT_TO_RIGHT,
                onCheckedChange = {
                    if (it) onDirectionChange(ChromeReadingDirection.LEFT_TO_RIGHT)
                },
                modifier = Modifier.weight(1f).testTag(ReaderChromeTestTags.DirectionLtr),
            ) { Text(stringResource(R.string.reader_settings_direction_ltr)) }
            ToggleButton(
                checked = config.direction == ChromeReadingDirection.RIGHT_TO_LEFT,
                onCheckedChange = {
                    if (it) onDirectionChange(ChromeReadingDirection.RIGHT_TO_LEFT)
                },
                modifier = Modifier.weight(1f).testTag(ReaderChromeTestTags.DirectionRtl),
            ) { Text(stringResource(R.string.reader_settings_direction_rtl)) }
        }

        Text(
            text = "Page fit",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Applies to the paged flow; scrolled always fills width.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ButtonGroup(modifier = Modifier.fillMaxWidth()) {
            ToggleButton(
                checked = config.pageFit == ChromePageFit.WIDTH,
                onCheckedChange = { if (it) onFitChange(ChromePageFit.WIDTH) },
                modifier = Modifier.weight(1f).testTag(ReaderChromeTestTags.FitWidth),
            ) { Text(stringResource(R.string.reader_settings_fit_width)) }
            ToggleButton(
                checked = config.pageFit == ChromePageFit.HEIGHT,
                onCheckedChange = { if (it) onFitChange(ChromePageFit.HEIGHT) },
                modifier = Modifier.weight(1f).testTag(ReaderChromeTestTags.FitHeight),
            ) { Text(stringResource(R.string.reader_settings_fit_height)) }
            ToggleButton(
                checked = config.pageFit == ChromePageFit.ORIGINAL,
                onCheckedChange = { if (it) onFitChange(ChromePageFit.ORIGINAL) },
                modifier = Modifier.weight(1f).testTag(ReaderChromeTestTags.FitOriginal),
            ) { Text(stringResource(R.string.reader_settings_fit_original)) }
        }

        IridiumSettingSwitch(
            title = "Crop page margins",
            subtitle = stringResource(R.string.reader_settings_crop_subtitle),
            checked = config.cropMargins,
            onCheckedChange = onCropChange,
            modifier = Modifier.testTag(ReaderChromeTestTags.CropSwitch),
        )

        Text(
            text = "Tap zones",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Outer partitions turn positions; the center toggles chrome.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ChromeNavModeChoiceCards(
            selectedMode = config.navMode,
            direction = config.direction,
            onSelectMode = onNavModeChange,
        )

        IridiumSettingSwitch(
            title = stringResource(R.string.reader_settings_keep_screen_on),
            checked = prefs.keepScreenOn,
            onCheckedChange = { onAction(ReaderAction.SetKeepScreenOn(it)) },
            modifier = Modifier.testTag(ReaderChromeSettingsSwitchTags.KeepScreenOn),
        )
        IridiumSettingSwitch(
            title = stringResource(R.string.reader_settings_volume_keys),
            checked = prefs.volumeKeys,
            onCheckedChange = { onAction(ReaderAction.SetVolumeKeys(it)) },
            modifier = Modifier.testTag(ReaderChromeSettingsSwitchTags.VolumeKeys),
        )
        if (prefs.volumeKeys) {
            IridiumSettingSwitch(
                title = stringResource(R.string.reader_settings_volume_invert),
                subtitle = stringResource(R.string.reader_settings_volume_invert_subtitle),
                checked = prefs.volumeKeysInverted,
                onCheckedChange = { onAction(ReaderAction.SetVolumeKeysInverted(it)) },
                modifier = Modifier.testTag(ReaderChromeSettingsSwitchTags.InvertVolumeKeys),
            )
        }
        IridiumSettingSwitch(
            title = stringResource(R.string.reader_settings_page_counter),
            checked = prefs.showPageCounter,
            onCheckedChange = { onAction(ReaderAction.SetShowPageCounter(it)) },
            modifier = Modifier.testTag(ReaderChromeSettingsSwitchTags.PageCounter),
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Wrapping tap-zone choice cards with live mini diagrams. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChromeNavModeChoiceCards(
    selectedMode: ChromeNavMode,
    direction: ChromeReadingDirection,
    onSelectMode: (ChromeNavMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberIridiumHaptics()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 3,
        modifier = modifier.fillMaxWidth(),
    ) {
        ChromeNavMode.entries.forEach { mode ->
            ChromeNavModeCard(
                label = chromeNavModeLabel(mode),
                mode = mode,
                selected = mode == selectedMode,
                direction = direction,
                onClick = {
                    haptics(IridiumHaptic.Select)
                    onSelectMode(mode)
                },
            )
        }
    }
}

fun chromeNavModeLabel(mode: ChromeNavMode): String = when (mode) {
    ChromeNavMode.DEFAULT -> "Default"
    ChromeNavMode.L_SHAPE -> "L-shape"
    ChromeNavMode.KINDLISH -> "Kindlish"
    ChromeNavMode.EDGE -> "Edge"
    ChromeNavMode.RIGHT_AND_LEFT -> "Sides"
    ChromeNavMode.DISABLED -> "Off"
}

@Composable
private fun ChromeNavModeCard(
    label: String,
    mode: ChromeNavMode,
    selected: Boolean,
    direction: ChromeReadingDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(width = if (selected) 2.dp else 1.dp, color = borderColor),
        tonalElevation = if (selected) 2.dp else 0.dp,
        modifier = modifier
            .width(84.dp)
            .testTag(ReaderChromeTestTags.navCardFor(mode))
            .semantics {
                this.selected = selected
                this.role = Role.RadioButton
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
        ) {
            ChromeNavZoneDiagram(mode = mode, direction = direction, selected = selected)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Minimal box-partition diagram of the tap zones at phone aspect ratio. */
@Composable
private fun ChromeNavZoneDiagram(
    mode: ChromeNavMode,
    direction: ChromeReadingDirection,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier.size(width = 54.dp, height = 48.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
        ) {
            when (mode) {
                ChromeNavMode.DEFAULT -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        ChromeZoneBox(
                            zone = chromeZoneForTap(0.1f, 0.5f, direction, mode),
                            label = "‹",
                            selected = selected,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        ChromeZoneBox(
                            zone = chromeZoneForTap(0.5f, 0.5f, direction, mode),
                            label = "•",
                            selected = selected,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        ChromeZoneBox(
                            zone = chromeZoneForTap(0.9f, 0.5f, direction, mode),
                            label = "›",
                            selected = selected,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
                ChromeNavMode.RIGHT_AND_LEFT -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        ChromeZoneBox(
                            zone = chromeZoneForTap(0.1f, 0.5f, direction, mode),
                            label = "‹",
                            selected = selected,
                            modifier = Modifier.weight(1.2f).fillMaxHeight(),
                        )
                        ChromeZoneBox(
                            zone = chromeZoneForTap(0.5f, 0.5f, direction, mode),
                            label = "•",
                            selected = selected,
                            modifier = Modifier.weight(0.6f).fillMaxHeight(),
                        )
                        ChromeZoneBox(
                            zone = chromeZoneForTap(0.9f, 0.5f, direction, mode),
                            label = "›",
                            selected = selected,
                            modifier = Modifier.weight(1.2f).fillMaxHeight(),
                        )
                    }
                }
                ChromeNavMode.KINDLISH -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.5.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        ChromeZoneBox(
                            zone = chromeZoneForTap(0.5f, 0.1f, direction, mode),
                            label = "•",
                            selected = selected,
                            modifier = Modifier.fillMaxWidth().height(12.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        ) {
                            ChromeZoneBox(
                                zone = chromeZoneForTap(0.1f, 0.6f, direction, mode),
                                label = "‹",
                                selected = selected,
                                modifier = Modifier.weight(0.35f).fillMaxHeight(),
                            )
                            ChromeZoneBox(
                                zone = chromeZoneForTap(0.6f, 0.6f, direction, mode),
                                label = "›",
                                selected = selected,
                                modifier = Modifier.weight(0.65f).fillMaxHeight(),
                            )
                        }
                    }
                }
                ChromeNavMode.L_SHAPE -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.5.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        ChromeZoneBox(
                            zone = chromeZoneForTap(0.5f, 0.1f, direction, mode),
                            label = "‹",
                            selected = selected,
                            modifier = Modifier.fillMaxWidth().height(11.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        ) {
                            ChromeZoneBox(
                                zone = chromeZoneForTap(0.1f, 0.5f, direction, mode),
                                label = "‹",
                                selected = selected,
                                modifier = Modifier.weight(0.32f).fillMaxHeight(),
                            )
                            ChromeZoneBox(
                                zone = chromeZoneForTap(0.5f, 0.5f, direction, mode),
                                label = "•",
                                selected = selected,
                                modifier = Modifier.weight(0.36f).fillMaxHeight(),
                            )
                            ChromeZoneBox(
                                zone = chromeZoneForTap(0.9f, 0.5f, direction, mode),
                                label = "›",
                                selected = selected,
                                modifier = Modifier.weight(0.32f).fillMaxHeight(),
                            )
                        }
                        ChromeZoneBox(
                            zone = chromeZoneForTap(0.5f, 0.9f, direction, mode),
                            label = "›",
                            selected = selected,
                            modifier = Modifier.fillMaxWidth().height(11.dp),
                        )
                    }
                }
                ChromeNavMode.EDGE -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ChromeZoneBox(
                            zone = chromeZoneForTap(0.05f, 0.05f, direction, mode),
                            label = "",
                            selected = selected,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(1.5.dp),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.52f)
                                .fillMaxHeight(0.72f),
                        ) {
                            ChromeZoneBox(
                                zone = chromeZoneForTap(0.5f, 0.5f, direction, mode),
                                label = "•",
                                selected = selected,
                                modifier = Modifier.fillMaxWidth().weight(1f),
                            )
                            ChromeZoneBox(
                                zone = chromeZoneForTap(0.5f, 0.8f, direction, mode),
                                label = "‹",
                                selected = selected,
                                modifier = Modifier.fillMaxWidth().height(10.dp),
                            )
                        }
                    }
                }
                ChromeNavMode.DISABLED -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                RoundedCornerShape(3.dp),
                            ),
                    ) {
                        Text(
                            text = "OFF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChromeZoneBox(
    zone: ChromeTapZone,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = when (zone) {
        ChromeTapZone.PREV -> MaterialTheme.colorScheme.tertiaryContainer
            .copy(alpha = if (selected) 0.85f else 0.55f)
        ChromeTapZone.NEXT -> MaterialTheme.colorScheme.primaryContainer
            .copy(alpha = if (selected) 0.85f else 0.55f)
        ChromeTapZone.MENU -> MaterialTheme.colorScheme.surfaceContainerHighest
            .copy(alpha = if (selected) 0.95f else 0.7f)
    }
    val contentColor = when (zone) {
        ChromeTapZone.PREV -> MaterialTheme.colorScheme.onTertiaryContainer
        ChromeTapZone.NEXT -> MaterialTheme.colorScheme.onPrimaryContainer
        ChromeTapZone.MENU -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(bg, shape = RoundedCornerShape(3.dp)),
    ) {
        Text(
            text = label.ifEmpty {
                when (zone) {
                    ChromeTapZone.PREV -> "‹"
                    ChromeTapZone.NEXT -> "›"
                    ChromeTapZone.MENU -> "•"
                }
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 9.sp,
            ),
            color = contentColor,
        )
    }
}
