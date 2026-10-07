package com.iridium.feature.reader.impl

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.LocalExpressiveMotionEnabled
import com.iridium.core.designsystem.rememberIridiumHaptics

/**
 * Newer M3 Expressive reader chrome: the five Mori reader actions from
 * [ReaderActionDock] hosted in a [HorizontalFloatingToolbar] with a vibrant
 * expand/collapse FAB.
 *
 * States:
 * - Expanded (default): FAB shows Close; all five actions are reachable.
 *   This is the only state used when reduce-motion is on.
 * - Collapsed: FAB shows Add; the toolbar morphs down to the FAB alone.
 *   Only entered with expressive motion enabled.
 * - Reduced-motion fallback: [LocalExpressiveMotionEnabled] == false forces
 *   the toolbar expanded and static, so state changes apply instantly with
 *   no spring/morph animation. The FAB still acknowledges taps with haptics
 *   and reports the (unapplied) collapse request to the host.
 *
 * Hero moments (exactly one animated): the expand/collapse toolbar morph,
 * driven by the toolbar itself. The vibrant FAB color is static chrome, not
 * a moment. Deliberately not wired into [ReaderScreen] yet: the host keeps
 * owning [ReaderChromeConfig] and can swap [ReaderActionDock] for this
 * toolbar in one call site once screenshots are approved.
 */
object ReaderFloatingToolbarTestTags {
    const val Toolbar = "readerFloatingToolbar"
    const val ExpandFab = "readerFloatingToolbarFab"
}

/**
 * Effective toolbar expansion honoring reduce-motion. Pure so it stays
 * unit-testable without composition: expressive motion follows the host's
 * [requested] state, reduced motion pins the toolbar expanded (instant).
 */
fun resolveFloatingToolbarExpanded(requested: Boolean, expressiveMotion: Boolean): Boolean =
    requested || !expressiveMotion

/**
 * Expressive floating reader toolbar. Every tap (FAB + five actions) plays
 * its semantic haptic via [rememberIridiumHaptics] before delegating out;
 * the host owns [config] persistence and the [expanded] state.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReaderChromeFloatingToolbar(
    config: ReaderChromeConfig,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDirectionToggle: () -> Unit,
    onFitCycle: () -> Unit,
    onCropToggle: () -> Unit,
    onOverviewClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expressiveMotion = LocalExpressiveMotionEnabled.current
    val haptics = rememberIridiumHaptics()
    val effectiveExpanded = resolveFloatingToolbarExpanded(expanded, expressiveMotion)

    HorizontalFloatingToolbar(
        expanded = effectiveExpanded,
        floatingActionButton = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = {
                    haptics(IridiumHaptic.Select)
                    onExpandedChange(!effectiveExpanded)
                },
                modifier = Modifier
                    .testTag(ReaderFloatingToolbarTestTags.ExpandFab)
                    .semantics {
                        onClick(
                            label = if (effectiveExpanded) {
                                "Collapse reader toolbar"
                            } else {
                                "Expand reader toolbar"
                            },
                            action = null,
                        )
                        stateDescription = if (effectiveExpanded) "Expanded" else "Collapsed"
                    },
            ) {
                Icon(
                    imageVector = if (effectiveExpanded) IridiumIcons.Close else IridiumIcons.Add,
                    contentDescription = if (effectiveExpanded) {
                        "Collapse reader toolbar"
                    } else {
                        "Expand reader toolbar"
                    },
                )
            }
        },
        modifier = modifier.testTag(ReaderFloatingToolbarTestTags.Toolbar),
    ) {
        val directionState = when (config.direction) {
            ChromeReadingDirection.LEFT_TO_RIGHT -> "Left to right"
            ChromeReadingDirection.RIGHT_TO_LEFT -> "Right to left"
        }
        ToolbarAction(
            onClick = {
                haptics(IridiumHaptic.Select)
                onDirectionToggle()
            },
            icon = { Icon(IridiumIcons.Motion, contentDescription = "Reading direction") },
            testTag = ReaderChromeTestTags.DirectionButton,
            onClickLabel = "Reading direction",
            stateText = directionState,
        )
        val fitState = when (config.pageFit) {
            ChromePageFit.WIDTH -> "Fit width"
            ChromePageFit.HEIGHT -> "Fit height"
            ChromePageFit.ORIGINAL -> "Original size"
        }
        ToolbarAction(
            onClick = {
                haptics(IridiumHaptic.Select)
                onFitCycle()
            },
            icon = { Icon(IridiumIcons.Tune, contentDescription = "Page fit") },
            testTag = ReaderChromeTestTags.FitButton,
            onClickLabel = "Page fit",
            stateText = fitState,
        )
        ToolbarAction(
            onClick = {
                haptics(IridiumHaptic.Select)
                onCropToggle()
            },
            icon = {
                Icon(
                    IridiumIcons.FormatPaint,
                    contentDescription = "Crop margins",
                    tint = if (config.cropMargins) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
            testTag = ReaderChromeTestTags.CropButton,
            onClickLabel = "Crop margins",
            stateText = if (config.cropMargins) "On" else "Off",
        )
        ToolbarAction(
            onClick = {
                haptics(IridiumHaptic.Select)
                onOverviewClick()
            },
            icon = { Icon(IridiumIcons.List, contentDescription = "Contents overview") },
            testTag = ReaderChromeTestTags.OverviewButton,
            onClickLabel = "Contents overview",
            stateText = null,
        )
        ToolbarAction(
            onClick = {
                haptics(IridiumHaptic.Select)
                onSettingsClick()
            },
            icon = { Icon(IridiumIcons.Settings, contentDescription = "Reader settings") },
            testTag = ReaderChromeTestTags.SettingsButton,
            onClickLabel = "Reader settings",
            stateText = null,
        )
    }
}

@Composable
private fun ToolbarAction(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    testTag: String,
    onClickLabel: String,
    stateText: String?,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .testTag(testTag)
            .semantics {
                onClick(label = onClickLabel, action = null)
                if (stateText != null) {
                    stateDescription = stateText
                }
            },
        content = { icon() },
    )
}
