package com.iridium.feature.reader.impl

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumSheet
import com.iridium.core.model.Highlight
import com.iridium.core.model.HighlightColor
import com.iridium.core.model.TocEntry

/** Table-of-contents sheet: chapters navigate via Readium locators. */
@Composable
internal fun ReaderTocSheet(
    toc: List<TocEntry>,
    onEntryClick: (TocEntry) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IridiumSheet(onDismiss = onDismiss, modifier = modifier) {
        Text(
            "Contents",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        ) {
            items(toc, key = { it.href }) { entry ->
                Column(
                    Modifier.fillMaxWidth()
                        .clickable { onEntryClick(entry) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    HorizontalDivider(Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}

/** Highlights + notes sheet: color dots, notes, delete. */
@Composable
internal fun ReaderHighlightsSheet(
    highlights: List<Highlight>,
    focusedId: String?,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IridiumSheet(onDismiss = onDismiss, modifier = modifier) {
        Text(
            "Highlights (${highlights.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        if (highlights.isEmpty()) {
            Text(
                "Long-press text, then use “Highlight selection” in the menu to save one here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(highlights, key = { it.id }) { highlight ->
                    HighlightRow(
                        highlight = highlight,
                        focused = highlight.id == focusedId,
                        onDelete = { onDelete(highlight.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightRow(
    highlight: Highlight,
    focused: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth()
            .background(
                if (focused) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                } else {
                    Color.Transparent
                },
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Box(
            Modifier.size(16.dp)
                .clip(CircleShape)
                .background(Color(highlightTint(highlight.color)))
                .align(Alignment.CenterVertically),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            highlight.note?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = highlight.selectedText.ifBlank { highlight.href.substringAfterLast('/') },
                style = if (highlight.note == null) {
                    MaterialTheme.typography.bodyLarge
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = if (highlight.note == null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(IridiumIcons.Delete, contentDescription = "Delete highlight")
        }
    }
}

/**
 * Add-highlight dialog (Lithium's color row + "Tap to add a note"):
 * pick a color, optionally attach a note, save the current selection.
 */
@Composable
internal fun AddHighlightDialog(
    onSave: (HighlightColor, String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var color by remember { mutableStateOf(HighlightColor.YELLOW) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Highlight") },
        text = {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    HighlightColor.entries.forEach { candidate ->
                        ColorDot(
                            color = candidate,
                            selected = candidate == color,
                            onClick = { color = candidate },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Tap to add a note") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(color, note.ifBlank { null }) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        modifier = modifier,
    )
}

@Composable
private fun ColorDot(
    color: HighlightColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(if (selected) 40.dp else 32.dp)
            .clip(CircleShape)
            .background(Color(highlightTint(color)))
            .clickable(onClick = onClick),
    ) {
        if (selected) {
            Text("✓", color = Color.Black)
        }
    }
}
