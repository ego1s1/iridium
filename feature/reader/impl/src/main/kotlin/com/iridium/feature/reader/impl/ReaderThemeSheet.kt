package com.iridium.feature.reader.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumSheet
import com.iridium.core.model.ColorSchemeChoice
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
    selected: ColorSchemeChoice,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    IridiumSheet(onDismiss = { onAction(ReaderAction.CloseThemeSheet) }, modifier = modifier) {
        ReaderThemeSheetContent(selected = selected, onAction = onAction)
    }
}

/** Sheet body content, exposed for testing (modals do not settle under Robolectric). */
@Composable
internal fun ReaderThemeSheetContent(
    selected: ColorSchemeChoice,
    onAction: (ReaderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
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
                selected = scheme == selected,
                onSelect = { onAction(ReaderAction.SetTheme(scheme)) },
            )
        }
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

/** Page foreground (body text) paired with [pageBackground]. */
internal fun ColorSchemeChoice.pageForeground(): Color = when (this) {
    ColorSchemeChoice.LIGHT -> Color(0xFF1A1C1E)
    ColorSchemeChoice.SEPIA -> Color(0xFF3E2F1C)
    ColorSchemeChoice.GREY -> Color(0xFFF2F2F2)
    ColorSchemeChoice.DARK -> Color(0xFFE3E1E5)
    ColorSchemeChoice.BLACK -> Color(0xFFFFFFFF)
}
