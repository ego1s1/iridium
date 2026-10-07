package com.iridium.feature.reader.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.iridium.core.model.TapInvertMode
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Full-bleed 3x3 tap-zone preview (Mori `TapZoneOverlay` parity, adapted to
 * EPUB tap semantics).
 *
 * Pure and stateless: samples [chromeZoneForTap] at each cell center for the
 * given [direction]/[navMode]/[invertMode] and paints the outcome —
 * PREV in tertiaryContainer, NEXT in primaryContainer, MENU in
 * surfaceVariant (all at 0.28 alpha) with pill labels. The call site owns
 * visibility gating; this composable is never wired into a screen itself.
 */
@Composable
fun TapZoneOverlay(
    direction: ChromeReadingDirection,
    navMode: ChromeNavMode,
    invertMode: TapInvertMode = TapInvertMode.NONE,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(ReaderChromeTestTags.TapZoneOverlay),
    ) {
        for (row in 0 until 3) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) {
                for (column in 0 until 3) {
                    val fractionX = (column * 2f + 1f) / 6f
                    val fractionY = (row * 2f + 1f) / 6f
                    val zone = chromeZoneForTap(
                        fractionX = fractionX,
                        fractionY = fractionY,
                        direction = direction,
                        navMode = navMode,
                        invertMode = invertMode,
                    )
                    TapZoneCell(
                        zone = zone,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TapZoneCell(
    zone: ChromeTapZone,
    modifier: Modifier = Modifier,
) {
    val cellColor = when (zone) {
        ChromeTapZone.PREV -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f)
        ChromeTapZone.NEXT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        ChromeTapZone.MENU -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
    }
    val pillContainer = when (zone) {
        ChromeTapZone.PREV -> MaterialTheme.colorScheme.tertiaryContainer
        ChromeTapZone.NEXT -> MaterialTheme.colorScheme.primaryContainer
        ChromeTapZone.MENU -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val pillContent = when (zone) {
        ChromeTapZone.PREV -> MaterialTheme.colorScheme.onTertiaryContainer
        ChromeTapZone.NEXT -> MaterialTheme.colorScheme.onPrimaryContainer
        ChromeTapZone.MENU -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = when (zone) {
        ChromeTapZone.PREV -> "PREV"
        ChromeTapZone.NEXT -> "NEXT"
        ChromeTapZone.MENU -> "MENU"
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(cellColor),
    ) {
        Surface(
            shape = CircleShape,
            color = pillContainer,
            contentColor = pillContent,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * Totally static 3x3 preview driven by an explicit zones grid instead of
 * [chromeZoneForTap] math. Kept alongside the math-driven [TapZoneOverlay]
 * for call sites (settings sheets, debug previews) that already resolved
 * zones and want zero recomputation surprises.
 */
@Composable
fun TapZoneGridPreview(
    zones: List<List<ChromeTapZone>>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag(ReaderChromeTestTags.TapZoneOverlay),
    ) {
        zones.take(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
            ) {
                row.take(3).forEach { zone ->
                    TapZoneCell(
                        zone = zone,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                    )
                }
            }
        }
    }
}
