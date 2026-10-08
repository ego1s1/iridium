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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            // WindowManager only accepts -1 (system) or 0..1: a switch owns
            // the -1 detent so the slider can never emit an invalid value,
            // and persistence happens on release, not per drag tick.
            var systemBrightness by remember(prefs.brightness < 0f) {
                mutableStateOf(prefs.brightness < 0f)
            }
            IridiumSettingSwitch(
                title = stringResource(R.string.reader_settings_brightness_system),
                checked = systemBrightness,
                onCheckedChange = {
                    systemBrightness = it
                    onAction(ReaderAction.SetBrightness(if (it) -1f else 0.5f))
                },
            )
            if (!systemBrightness) {
                var draft by remember(prefs.brightness) {
                    mutableFloatStateOf(prefs.brightness.coerceIn(0f, 1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = draft,
                        onValueChange = { draft = it },
                        onValueChangeFinished = {
                            onAction(ReaderAction.SetBrightness(draft.coerceIn(0f, 1f)))
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    Text("A", style = MaterialTheme.typography.titleMedium)
                }
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
            var lineHeightDraft by remember(prefs.lineHeight) {
                mutableFloatStateOf(prefs.lineHeight.coerceIn(1f, 2.5f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reader_settings_line_height), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${(lineHeightDraft * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Slider(
                    value = lineHeightDraft,
                    onValueChange = { lineHeightDraft = it },
                    onValueChangeFinished = {
                        onAction(ReaderAction.SetLineHeight(lineHeightDraft.coerceIn(1f, 2.5f)))
                    },
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
            IridiumSettingSwitch(
                title = stringResource(R.string.reader_settings_invert_taps),
                subtitle = stringResource(R.string.reader_settings_invert_taps_body),
                checked = prefs.invertTaps,
                onCheckedChange = { onAction(ReaderAction.SetInvertTaps(it)) },
                modifier = Modifier.testTag(ReaderChromeTestTags.TapZoneInvertSwitch),
            )

            Spacer(Modifier.height(8.dp))
            IridiumSettingSwitch(
                title = stringResource(R.string.reader_settings_night_light),
                subtitle = stringResource(R.string.reader_settings_night_light_body),
                checked = prefs.nightLight,
                onCheckedChange = { onAction(ReaderAction.SetNightLight(it)) },
            )
            if (prefs.nightLight) {
                var warmthDraft by remember(prefs.nightLightIntensity) {
                    mutableFloatStateOf(prefs.nightLightIntensity.coerceIn(0f, 1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.reader_settings_night_light_intensity),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "${(warmthDraft * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Slider(
                        value = warmthDraft,
                        onValueChange = { warmthDraft = it },
                        onValueChangeFinished = {
                            onAction(ReaderAction.SetNightLightIntensity(warmthDraft.coerceIn(0f, 1f)))
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1.4f),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))
            IridiumSettingSwitch(
                title = stringResource(R.string.reader_settings_volume_keys),
                checked = prefs.volumeKeys,
                onCheckedChange = { onAction(ReaderAction.SetVolumeKeys(it)) },
            )
            if (prefs.volumeKeys) {
                IridiumSettingSwitch(
                    title = stringResource(R.string.reader_settings_volume_invert),
                    subtitle = stringResource(R.string.reader_settings_volume_invert_subtitle),
                    checked = prefs.volumeKeysInverted,
                    onCheckedChange = { onAction(ReaderAction.SetVolumeKeysInverted(it)) },
                )
            }
            IridiumSettingSwitch(
                title = stringResource(R.string.reader_settings_keep_screen_on),
                checked = prefs.keepScreenOn,
                onCheckedChange = { onAction(ReaderAction.SetKeepScreenOn(it)) },
            )
            IridiumSettingSwitch(
                title = stringResource(R.string.reader_settings_page_counter),
                checked = prefs.showPageCounter,
                onCheckedChange = { onAction(ReaderAction.SetShowPageCounter(it)) },
            )
        }
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
