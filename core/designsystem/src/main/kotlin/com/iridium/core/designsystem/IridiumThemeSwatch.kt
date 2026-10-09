package com.iridium.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * Round theme-swatch button: a filled circle with a selected ring and check.
 * Colors are caller-owned (reader schemes, app palettes) so this stays free
 * of domain enums; selection uses radio semantics for TalkBack.
 */
@Composable
fun IridiumThemeSwatch(
    background: Color,
    contentColor: Color,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val haptics = rememberIridiumHaptics()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = CircleShape,
            )
            .clickable(
                role = Role.RadioButton,
                onClick = {
                    haptics(IridiumHaptic.Select)
                    onSelect()
                },
            ),
    ) {
        if (selected) {
            Icon(
                imageVector = IridiumIcons.Check,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
