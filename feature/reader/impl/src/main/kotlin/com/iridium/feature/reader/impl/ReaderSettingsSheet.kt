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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.ReadingFlow
import com.iridium.core.model.TextAlign

/**
 * Reader settings bottom sheet (Lithium's "Choose from three themes" card):
 * Flow Auto/Paged/Scrolled, brightness slider, theme swatches, text size
 * stepper, text alignment. Expressive ButtonGroup for the Flow row.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReaderSettingsSheet(
    prefs: ReaderPreferences,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(ReaderAction.CloseSettings) },
        modifier = modifier,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Flow", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FlowOption(
                    label = "Auto",
                    selected = prefs.flow == ReadingFlow.AUTO,
                    onClick = { onAction(ReaderAction.SetFlow(ReadingFlow.AUTO)) },
                )
                FlowOption(
                    label = "Paged",
                    selected = prefs.flow == ReadingFlow.PAGED,
                    onClick = { onAction(ReaderAction.SetFlow(ReadingFlow.PAGED)) },
                )
                FlowOption(
                    label = "Scrolled",
                    selected = prefs.flow == ReadingFlow.SCROLLED,
                    onClick = { onAction(ReaderAction.SetFlow(ReadingFlow.SCROLLED)) },
                )
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
                IconButton(
                    onClick = {
                        onAction(ReaderAction.SetFontScale(prefs.fontScale - 0.1f))
                    },
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Smaller text")
                }
                IconButton(
                    onClick = {
                        onAction(ReaderAction.SetFontScale(prefs.fontScale + 0.1f))
                    },
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Larger text")
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.width(220.dp),
                ) {
                    AlignOption(
                        label = "Left",
                        selected = prefs.textAlign == TextAlign.LEFT,
                        onClick = { onAction(ReaderAction.SetTextAlign(TextAlign.LEFT)) },
                    )
                    AlignOption(
                        label = "Full",
                        selected = prefs.textAlign == TextAlign.JUSTIFY,
                        onClick = { onAction(ReaderAction.SetTextAlign(TextAlign.JUSTIFY)) },
                    )
                    AlignOption(
                        label = "Auto",
                        selected = prefs.textAlign == TextAlign.ORIGINAL,
                        onClick = { onAction(ReaderAction.SetTextAlign(TextAlign.ORIGINAL)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun androidx.compose.foundation.layout.RowScope.FlowOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        modifier = Modifier.weight(1f),
    ) {
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun androidx.compose.foundation.layout.RowScope.AlignOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        modifier = Modifier.weight(1f),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ThemeSwatch(
    theme: ColorSchemeChoice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (bg, border) = when (theme) {
        ColorSchemeChoice.LIGHT -> 0xFFFFFFFF.toInt() to MaterialTheme.colorScheme.outline
        ColorSchemeChoice.SEPIA -> 0xFFF5E6C8.toInt() to MaterialTheme.colorScheme.outline
        ColorSchemeChoice.GREY -> 0xFF444444.toInt() to MaterialTheme.colorScheme.outline
        ColorSchemeChoice.DARK -> 0xFF121212.toInt() to MaterialTheme.colorScheme.outline
        ColorSchemeChoice.BLACK -> 0xFF000000.toInt() to MaterialTheme.colorScheme.outline
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(androidx.compose.ui.graphics.Color(bg))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else border,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    ) {
        if (selected) {
            Text(
                "✓",
                color = when (theme) {
                    ColorSchemeChoice.GREY, ColorSchemeChoice.DARK, ColorSchemeChoice.BLACK ->
                        androidx.compose.ui.graphics.Color.White
                    else -> androidx.compose.ui.graphics.Color.Black
                },
            )
        }
    }
}
