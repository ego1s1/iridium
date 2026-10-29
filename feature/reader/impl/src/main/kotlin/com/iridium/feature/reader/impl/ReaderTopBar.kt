package com.iridium.feature.reader.impl

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.rememberIridiumHaptics

/**
 * One overflow-menu row: leading icon is decorative (the screen reader uses
 * the label), an optional trailing value shows the cycle target, and every
 * tap plays the standard select haptic.
 */
@Composable
private fun ReaderMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    val haptics = rememberIridiumHaptics()
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = trailing?.let { { Text(text = it, style = MaterialTheme.typography.labelMedium) } },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = {
            haptics(IridiumHaptic.Select)
            onSelect()
        },
        modifier = modifier.testTag(testTag),
    )
}

/** Opaque top bar: title + quick actions + overflow menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderTopBar(
    title: String,
    author: String?,
    onBackClick: () -> Unit,
    onAction: (ReaderAction) -> Unit,
    onAddHighlight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberIridiumHaptics()
    // Opaque surface: no blur means text must not show through.
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                author?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = {
                haptics(IridiumHaptic.Select)
                onBackClick()
            }) {
                Icon(IridiumIcons.Back, contentDescription = stringResource(R.string.reader_action_back))
            }
        },
        actions = {
            IconButton(onClick = {
                haptics(IridiumHaptic.Select)
                onAction(ReaderAction.OpenToc)
            }) {
                Icon(IridiumIcons.List, contentDescription = stringResource(R.string.reader_toc_title))
            }
            IconButton(onClick = {
                haptics(IridiumHaptic.Select)
                onAction(ReaderAction.OpenHighlights)
            }) {
                Icon(
                    IridiumIcons.Highlight,
                    contentDescription = stringResource(R.string.reader_action_highlights),
                )
            }
            IconButton(onClick = {
                haptics(IridiumHaptic.Select)
                onAction(ReaderAction.OpenSettings)
            }) {
                Icon(
                    IridiumIcons.Settings,
                    contentDescription = stringResource(R.string.reader_action_settings),
                )
            }
            var menuOpen by remember { mutableStateOf(false) }
            IconButton(onClick = {
                haptics(IridiumHaptic.Select)
                menuOpen = true
            }) {
                Icon(IridiumIcons.More, contentDescription = stringResource(R.string.reader_action_more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                ReaderMenuItem(
                    icon = IridiumIcons.List,
                    label = stringResource(R.string.reader_toc_title),
                    testTag = "readerMenuToc",
                    onSelect = {
                        menuOpen = false
                        onAction(ReaderAction.OpenToc)
                    },
                )
                ReaderMenuItem(
                    icon = IridiumIcons.Edit,
                    label = stringResource(R.string.reader_action_highlight_selection),
                    testTag = "readerMenuHighlightSelection",
                    onSelect = {
                        menuOpen = false
                        onAddHighlight()
                    },
                )
                ReaderMenuItem(
                    icon = IridiumIcons.Highlight,
                    label = stringResource(R.string.reader_action_highlights),
                    testTag = "readerMenuHighlights",
                    onSelect = {
                        menuOpen = false
                        onAction(ReaderAction.OpenHighlights)
                    },
                )
                HorizontalDivider()
                ReaderMenuItem(
                    icon = IridiumIcons.Contrast,
                    label = stringResource(R.string.reader_settings_theme),
                    testTag = "readerMenuTheme",
                    onSelect = {
                        menuOpen = false
                        onAction(ReaderAction.OpenThemeSheet)
                    },
                )
                HorizontalDivider()
                ReaderMenuItem(
                    icon = IridiumIcons.Settings,
                    label = stringResource(R.string.reader_action_settings),
                    testTag = "readerMenuSettings",
                    onSelect = {
                        menuOpen = false
                        onAction(ReaderAction.OpenSettings)
                    },
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
