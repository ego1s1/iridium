package com.iridium.feature.reader.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumAlertDialog
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumSheet
import com.iridium.core.model.ColorSchemeChoice
import com.iridium.core.model.CustomReaderTheme
import com.iridium.core.model.ReaderPreferences
import com.iridium.core.model.isCustomThemeActive
import com.iridium.core.model.pageForegroundArgb
import com.iridium.core.model.resolvedColors
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.sizeIn

/**
 * Reader theme chooser: a bottom sheet of scheme cards, each a miniature
 * page in its real background/foreground colors with sample text, so the
 * choice reads as a preview instead of a name. Tapping applies immediately
 * and leaves the sheet open for comparison; dismiss via swipe or tap-outside.
 */
@Composable
internal fun ReaderThemeSheet(
    prefs: ReaderPreferences,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    IridiumSheet(onDismiss = { onAction(ReaderAction.CloseThemeSheet) }, modifier = modifier) {
        ReaderThemeSheetContent(prefs = prefs, onAction = onAction)
    }
}

/** Sheet body content, exposed for testing (modals do not settle under Robolectric). */
@Composable
internal fun ReaderThemeSheetContent(
    prefs: ReaderPreferences,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf<CustomReaderTheme?>(null) }
    val customActive = prefs.isCustomThemeActive()
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.reader_theme_sheet_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ColorSchemeChoice.entries.forEach { scheme ->
            ThemePreviewCard(
                scheme = scheme,
                selected = scheme == prefs.theme && !customActive,
                onSelect = { onAction(ReaderAction.SetTheme(scheme)) },
            )
        }
        if (prefs.customThemes.isNotEmpty()) {
            Text(
                text = stringResource(R.string.reader_theme_custom_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        prefs.customThemes.forEach { custom ->
            CustomThemePreviewCard(
                custom = custom,
                selected = custom.id == prefs.customThemeId,
                onSelect = { onAction(ReaderAction.SetCustomTheme(custom.id)) },
                onDelete = { onAction(ReaderAction.DeleteCustomTheme(custom.id)) },
            )
        }
        TextButton(
            onClick = {
                editing = CustomReaderTheme(
                    id = java.util.UUID.randomUUID().toString(),
                    name = "",
                    backgroundArgb = prefs.resolvedColors().first,
                    foregroundArgb = prefs.resolvedColors().second,
                )
            },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.reader_theme_custom_new))
        }
    }
    editing?.let { draft ->
        CustomThemeEditorDialog(
            draft = draft,
            onSave = {
                onAction(ReaderAction.SaveCustomTheme(it))
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ThemePreviewCard(
    scheme: ColorSchemeChoice,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageBg = scheme.pageBackground()
    val pageFg = scheme.pageForeground()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 76.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Miniature page: scheme background with sample title + text lines.
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .size(width = 64.dp, height = 52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(pageBg)
                .padding(horizontal = 8.dp, vertical = 7.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(pageFg),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(pageFg.copy(alpha = 0.55f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(pageFg.copy(alpha = 0.55f)),
            )
        }
        Text(
            text = scheme.displayName(),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = IridiumIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ColorSchemeChoice.displayName(): String = when (this) {
    ColorSchemeChoice.LIGHT -> stringResource(R.string.reader_theme_light)
    ColorSchemeChoice.SEPIA -> stringResource(R.string.reader_theme_sepia)
    ColorSchemeChoice.GREY -> stringResource(R.string.reader_theme_grey)
    ColorSchemeChoice.DARK -> stringResource(R.string.reader_theme_dark)
    ColorSchemeChoice.BLACK -> stringResource(R.string.reader_theme_black)
}

/** Preview card for one user palette, with a delete affordance. */
@Composable
private fun CustomThemePreviewCard(
    custom: CustomReaderTheme,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageBg = Color(custom.backgroundArgb)
    val pageFg = Color(custom.foregroundArgb)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 76.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .size(width = 64.dp, height = 52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(pageBg)
                .padding(horizontal = 8.dp, vertical = 7.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(pageFg),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(pageFg.copy(alpha = 0.55f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(pageFg.copy(alpha = 0.55f)),
            )
        }
        Text(
            text = custom.name.ifBlank { stringResource(R.string.reader_theme_custom_untitled) },
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = IridiumIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = IridiumIcons.Delete,
                contentDescription = stringResource(R.string.reader_theme_custom_delete),
            )
        }
    }
}

/** Small editor for one user palette: name plus background/foreground swatches. */
@Composable
private fun CustomThemeEditorDialog(
    draft: CustomReaderTheme,
    onSave: (CustomReaderTheme) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember(draft.id) { mutableStateOf(draft.name.ifBlank { "" }) }
    var bg by remember(draft.id) { mutableIntStateOf(draft.backgroundArgb) }
    var fg by remember(draft.id) { mutableIntStateOf(draft.foregroundArgb) }
    IridiumAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reader_theme_custom_editor_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.reader_theme_custom_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.reader_theme_custom_background))
                SwatchRow(selected = bg, onSelect = { bg = it })
                Text(stringResource(R.string.reader_theme_custom_foreground))
                SwatchRow(selected = fg, onSelect = { fg = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(draft.copy(name = name.trim().ifBlank { "Custom" }, backgroundArgb = bg, foregroundArgb = fg))
                },
            ) {
                Text(stringResource(R.string.reader_theme_custom_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.reader_dialog_cancel))
            }
        },
        modifier = modifier,
    )
}

private val CustomSwatches = listOf(
    0xFFFFFFFF.toInt(), 0xFFF5EFE0.toInt(), 0xFFE8E0D0.toInt(), 0xFF9AA0A6.toInt(),
    0xFF3C4043.toInt(), 0xFF202124.toInt(), 0xFF000000.toInt(), 0xFF1B2A1B.toInt(),
    0xFF2A1B1B.toInt(), 0xFF1B2A3A.toInt(), 0xFFE8F0E8.toInt(), 0xFFF0E8E0.toInt(),
)

@Composable
private fun SwatchRow(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        CustomSwatches.forEach { color ->
            val active = color == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = if (active) 3.dp else 1.dp,
                        color = if (active) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    )
                    .clickable(role = Role.RadioButton, onClick = { onSelect(color) }),
            )
        }
    }
}

/** Page foreground (body text) paired with [pageBackgroundArgb]. */
internal fun ColorSchemeChoice.pageForeground(): Color = Color(pageForegroundArgb())
