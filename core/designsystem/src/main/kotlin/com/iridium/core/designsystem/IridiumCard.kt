package com.iridium.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Unified Material 3 Expressive container card: supports spring-based tactile
 * bounce on press when interactive, with fluid tonal surfaces.
 */
@Composable
fun IridiumExpressiveCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border: BorderStroke? = null,
    elevation: CardElevation = CardDefaults.cardElevation(),
    haptic: IridiumHaptic = IridiumHaptic.Select,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    val haptics = rememberIridiumHaptics()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && expressiveMotion) 0.97f else 1f,
        animationSpec = IridiumMotion.defaultSpatialSpec(),
        label = "expressiveCardScale",
    )

    Card(
        onClick = {
            haptics(haptic)
            onClick()
        },
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        interactionSource = interactionSource,
        content = content,
    )
}
