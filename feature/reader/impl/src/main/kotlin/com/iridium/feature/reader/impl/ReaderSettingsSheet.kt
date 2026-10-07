package com.iridium.feature.reader.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumSettingSwitch
import com.iridium.core.designsystem.IridiumSheet
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ReadingFlow
import com.iridium.core.model.TapInvertMode
import com.iridium.core.model.TapZoneMode
import com.iridium.core.model.TextAlign

/**
 * Reader settings sheet (Lithium's "Choose from three themes" card):
 * Flow Auto/Paged/Scrolled, brightness slider, theme swatches, text size
 * stepper, text alignment — using M3 Expressive ButtonGroups.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReaderSettingsSheet(
    prefs: ReaderPreferences,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    IridiumSheet(onDismiss = { onAction(ReaderAction.CloseSettings) }, modifier = modifier) {
        ReaderSettingsContent(prefs = prefs, onAction = onAction)
    }
}

/** Sheet body content, exposed for testing (modals do not settle under Robolectric). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReaderSettingsContent(
    prefs: ReaderPreferences,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
            Text(stringResource(R.string.reader_settings_flow), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            ButtonGroup(modifier = Modifier.fillMaxWidth()) {
                ToggleButton(
                    checked = prefs.flow == ReadingFlow.AUTO,
                    onCheckedChange = { onAction(ReaderAction.SetFlow(ReadingFlow.AUTO)) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.reader_settings_flow_auto)) }
                ToggleButton(
                    checked = prefs.flow == ReadingFlow.PAGED,
                    onCheckedChange = { onAction(ReaderAction.SetFlow(ReadingFlow.PAGED)) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.reader_settings_flow_paged)) }
                ToggleButton(
                    checked = prefs.flow == ReadingFlow.SCROLLED,
                    onCheckedChange = { onAction(ReaderAction.SetFlow(ReadingFlow.SCROLLED)) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.reader_settings_flow_scrolled)) }
            }

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.reader_settings_brightness), style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("A", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = prefs.brightness,
                    onValueChange = { onAction(ReaderAction.SetBrightness(it)) },
                    valueRange = -1f..1f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text("A", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.reader_settings_theme), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorSchemeChoice.entries.forEach { theme ->
                    ThemeSwatch(
                        theme = theme,
                        selected = prefs.theme == theme,
                        onClick = { onAction(ReaderAction.SetTheme(theme)) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reader_settings_text_size), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${(prefs.fontScale * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onAction(ReaderAction.SetFontScale(prefs.fontScale - 0.1f)) }) {
                    Icon(IridiumIcons.Remove, contentDescription = stringResource(R.string.reader_settings_text_smaller))
                }
                IconButton(onClick = { onAction(ReaderAction.SetFontScale(prefs.fontScale + 0.1f)) }) {
                    Icon(IridiumIcons.Add, contentDescription = stringResource(R.string.reader_settings_text_larger))
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reader_settings_line_height), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${(prefs.lineHeight * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Slider(
                    value = prefs.lineHeight,
                    onValueChange = { onAction(ReaderAction.SetLineHeight(it)) },
                    valueRange = 1f..2.5f,
                    modifier = Modifier.weight(1.4f),
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reader_settings_text_align), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        when (prefs.textAlign) {
                            TextAlign.ORIGINAL -> stringResource(R.string.reader_settings_align_auto)
                            TextAlign.LEFT -> stringResource(R.string.reader_settings_align_left)
                            TextAlign.JUSTIFY -> stringResource(R.string.reader_settings_align_justified)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ButtonGroup(modifier = Modifier.weight(1.4f)) {
                    ToggleButton(
                        checked = prefs.textAlign == TextAlign.LEFT,
                        onCheckedChange = { onAction(ReaderAction.SetTextAlign(TextAlign.LEFT)) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.reader_settings_align_left), style = MaterialTheme.typography.labelSmall) }
                    ToggleButton(
                        checked = prefs.textAlign == TextAlign.JUSTIFY,
                        onCheckedChange = { onAction(ReaderAction.SetTextAlign(TextAlign.JUSTIFY)) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.reader_settings_align_full), style = MaterialTheme.typography.labelSmall) }
                    ToggleButton(
                        checked = prefs.textAlign == TextAlign.ORIGINAL,
                        onCheckedChange = { onAction(ReaderAction.SetTextAlign(TextAlign.ORIGINAL)) },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.reader_settings_align_auto), style = MaterialTheme.typography.labelSmall) }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.reader_settings_tap_zones), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.reader_settings_tap_zones_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            ChromeNavModeChoiceCards(
                selectedMode = prefs.tapZoneMode,
                direction = ChromeReadingDirection.LEFT_TO_RIGHT,
                onSelectMode = { onAction(ReaderAction.SetTapZoneMode(it)) },
            )
            Spacer(Modifier.height(8.dp))
            ButtonGroup(modifier = Modifier.fillMaxWidth()) {
                InvertOption(
                    selected = prefs.tapZoneInvert == TapInvertMode.NONE,
                    onSelect = { onAction(ReaderAction.SetTapZoneInvert(TapInvertMode.NONE)) },
                    label = stringResource(R.string.reader_settings_invert_none),
                    modifier = Modifier.weight(1f).testTag(ReaderChromeTestTags.TapZoneInvertNone),
                )
                InvertOption(
                    selected = prefs.tapZoneInvert == TapInvertMode.HORIZONTAL,
                    onSelect = { onAction(ReaderAction.SetTapZoneInvert(TapInvertMode.HORIZONTAL)) },
                    label = stringResource(R.string.reader_settings_invert_horizontal),
                    modifier = Modifier.weight(1f),
                )
                InvertOption(
                    selected = prefs.tapZoneInvert == TapInvertMode.VERTICAL,
                    onSelect = { onAction(ReaderAction.SetTapZoneInvert(TapInvertMode.VERTICAL)) },
                    label = stringResource(R.string.reader_settings_invert_vertical),
                    modifier = Modifier.weight(1f),
                )
                InvertOption(
                    selected = prefs.tapZoneInvert == TapInvertMode.BOTH,
                    onSelect = { onAction(ReaderAction.SetTapZoneInvert(TapInvertMode.BOTH)) },
                    label = stringResource(R.string.reader_settings_invert_both),
                    modifier = Modifier.weight(1f).testTag(ReaderChromeTestTags.TapZoneInvertBoth),
                )
            }
            Spacer(Modifier.height(8.dp))
            TapZoneOverlay(
                direction = ChromeReadingDirection.LEFT_TO_RIGHT,
                navMode = prefs.tapZoneMode,
                invertMode = prefs.tapZoneInvert,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(MaterialTheme.shapes.medium),
            )

            Spacer(Modifier.height(8.dp))
            IridiumSettingSwitch(
                title = stringResource(R.string.reader_settings_night_light),
                subtitle = stringResource(R.string.reader_settings_night_light_body),
                checked = prefs.nightLight,
                onCheckedChange = { onAction(ReaderAction.SetNightLight(it)) },
            )
            if (prefs.nightLight) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.reader_settings_night_light_intensity),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "${(prefs.nightLightIntensity * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Slider(
                        value = prefs.nightLightIntensity,
                        onValueChange = { onAction(ReaderAction.SetNightLightIntensity(it)) },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1.4f),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InvertOption(
    selected: Boolean,
    onSelect: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { if (it) onSelect() },
        modifier = modifier,
    ) { Text(label, style = MaterialTheme.typography.labelSmall) }
}

@Composable
private fun ThemeSwatch(
    theme: ColorSchemeChoice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (bg, onSwatch) = when (theme) {
        ColorSchemeChoice.LIGHT -> 0xFFFFFFFF.toInt() to Color.Black
        ColorSchemeChoice.SEPIA -> 0xFFF5E6C8.toInt() to Color.Black
        ColorSchemeChoice.GREY -> 0xFF444444.toInt() to Color.White
        ColorSchemeChoice.DARK -> 0xFF121212.toInt() to Color.White
        ColorSchemeChoice.BLACK -> 0xFF000000.toInt() to Color.White
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(bg))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    ) {
        if (selected) {
            Icon(
                imageVector = IridiumIcons.Check,
                contentDescription = stringResource(R.string.reader_settings_swatch_selected),
                tint = onSwatch,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
