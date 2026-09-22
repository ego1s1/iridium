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
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumIcons
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
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Flow", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            ButtonGroup(modifier = Modifier.fillMaxWidth()) {
                ToggleButton(
                    checked = prefs.flow == ReadingFlow.AUTO,
                    onCheckedChange = { onAction(ReaderAction.SetFlow(ReadingFlow.AUTO)) },
                    modifier = Modifier.weight(1f),
                ) { Text("Auto") }
                ToggleButton(
                    checked = prefs.flow == ReadingFlow.PAGED,
                    onCheckedChange = { onAction(ReaderAction.SetFlow(ReadingFlow.PAGED)) },
                    modifier = Modifier.weight(1f),
                ) { Text("Paged") }
                ToggleButton(
                    checked = prefs.flow == ReadingFlow.SCROLLED,
                    onCheckedChange = { onAction(ReaderAction.SetFlow(ReadingFlow.SCROLLED)) },
                    modifier = Modifier.weight(1f),
                ) { Text("Scrolled") }
            }

            Spacer(Modifier.height(12.dp))
            Text("Brightness", style = MaterialTheme.typography.titleSmall)
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
            Text("Theme", style = MaterialTheme.typography.titleSmall)
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
                    Text("Text size", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${(prefs.fontScale * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onAction(ReaderAction.SetFontScale(prefs.fontScale - 0.1f)) }) {
                    Icon(IridiumIcons.Remove, contentDescription = "Smaller text")
                }
                IconButton(onClick = { onAction(ReaderAction.SetFontScale(prefs.fontScale + 0.1f)) }) {
                    Icon(IridiumIcons.Add, contentDescription = "Larger text")
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Text align", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        when (prefs.textAlign) {
                            TextAlign.ORIGINAL -> "Original"
                            TextAlign.LEFT -> "Left"
                            TextAlign.JUSTIFY -> "Justified"
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
                    ) { Text("Left", style = MaterialTheme.typography.labelSmall) }
                    ToggleButton(
                        checked = prefs.textAlign == TextAlign.JUSTIFY,
                        onCheckedChange = { onAction(ReaderAction.SetTextAlign(TextAlign.JUSTIFY)) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Full", style = MaterialTheme.typography.labelSmall) }
                    ToggleButton(
                        checked = prefs.textAlign == TextAlign.ORIGINAL,
                        onCheckedChange = { onAction(ReaderAction.SetTextAlign(TextAlign.ORIGINAL)) },
                        modifier = Modifier.weight(1f),
                    ) { Text("Auto", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
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
                contentDescription = "Selected",
                tint = onSwatch,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
