package com.iridium.feature.reader.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumMotion
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.model.ReadingFlow
import kotlin.math.roundToInt

/**
 * Mori reader-chrome parity for Iridium's EPUB reader (UI hierarchy.md §3.3).
 *
 * New chrome only: floating scrubber island with tooltip, floating EPUB
 * action dock (flow / theme / contents / highlights / settings). The existing
 * reader engine ([ReaderScreen], [ReaderViewModel]) is untouched; the chrome
 * is stateless and driven by callbacks so the host adopts it without
 * rewiring Readium plumbing.
 *
 * EPUB adaptation notes (comic pages -> reflowable positions):
 * - Mori's page index/count becomes the EPUB position index/count.
 * - Mori's reading direction still mirrors the scrubber + tap zones.
 */
enum class ChromeReadingDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
}

/** Tap-zone outcome for a tap inside the reading surface. */
enum class ChromeTapZone {
    PREV,
    MENU,
    NEXT,
}

/** Test tags for the Mori-parity reader chrome. */
object ReaderChromeTestTags {
    const val ScrubberIsland = "chromeScrubberIsland"
    const val ScrubTooltip = "chromeScrubTooltip"
    const val ScrubSlider = "chromeScrubSlider"
    const val ScrubPrev = "chromeScrubPrev"
    const val ScrubNext = "chromeScrubNext"
    const val EpubDock = "chromeEpubDock"
    const val EpubFlowButton = "chromeEpubFlow"
    const val EpubThemeButton = "chromeEpubTheme"
    const val EpubTocButton = "chromeEpubToc"
    const val EpubHighlightsButton = "chromeEpubHighlights"
    const val EpubSettingsButton = "chromeEpubSettings"
    const val TapZoneInvertSwitch = "tapZoneInvertSwitch"
}

// Pure helpers (unit-testable without composition)

/** 0-based position index for a 0f..1f progression over [count] positions. */
fun scrubIndexForProgression(progression: Float, count: Int): Int {
    if (count <= 1) return 0
    return (progression.coerceIn(0f, 1f) * (count - 1)).roundToInt()
        .coerceIn(0, count - 1)
}

/** 0f..1f progression for a 0-based position [index] over [count] positions. */
fun progressionForIndex(index: Int, count: Int): Float {
    if (count <= 1) return 0f
    return index.coerceIn(0, count - 1).toFloat() / (count - 1).toFloat()
}

fun canChromeGoForward(index: Int, count: Int): Boolean = index < count - 1

fun canChromeGoBackward(index: Int): Boolean = index > 0

/** Warm scrim color for night light at [intensity] 0f..1f (max 40% alpha). */
fun nightLightColor(intensity: Float): Color {
    val alpha = (intensity.coerceIn(0f, 1f) * MaxNightLightAlpha * 255f)
        .roundToInt().coerceIn(0, 255)
    return Color(red = 0xFF, green = 0x9E, blue = 0x42, alpha = alpha)
}

private const val MaxNightLightAlpha = 0.4f

/** "12 / 173" style counter; [oneBased] is 1-based. */
fun formatChromeCounter(oneBased: Int, total: Int): String = "$oneBased / $total"

/**
 * Resolves which tap zone a tap at ([fractionX], [fractionY]) falls into:
 * fixed horizontal thirds (outer partitions navigate, the center toggles
 * chrome), optionally mirrored for right-to-left reading or the user's
 * invert-taps switch. Taps landing on the top chrome strip always toggle.
 */
fun chromeZoneForTap(
    fractionX: Float,
    fractionY: Float,
    direction: ChromeReadingDirection,
    invertTaps: Boolean = false,
): ChromeTapZone {
    val mirror = invertTaps != (direction == ChromeReadingDirection.RIGHT_TO_LEFT)
    val x = if (mirror) {
        1f - fractionX.coerceIn(0f, 1f)
    } else {
        fractionX.coerceIn(0f, 1f)
    }
    if (fractionY < 0.05f) return ChromeTapZone.MENU
    return when {
        x < 1f / 3f -> ChromeTapZone.PREV
        x > 2f / 3f -> ChromeTapZone.NEXT
        else -> ChromeTapZone.MENU
    }
}

// Composables

/**
 * Floating scrubber island: prev/next steppers flanking a pill with the
 * position counter + slider, with a live tooltip bubble while scrubbing.
 * Row order mirrors [direction] so "forward" follows the reading direction.
 */
@Composable
fun ReaderScrubberIsland(
    positionIndex: Int,
    positionCount: Int,
    direction: ChromeReadingDirection,
    onSeek: (Float) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberIridiumHaptics()
    val rowDirection = if (direction == ChromeReadingDirection.RIGHT_TO_LEFT) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    val safeIndex = positionIndex.coerceIn(0, (positionCount - 1).coerceAtLeast(0))
    var scrub by remember(positionCount) { mutableStateOf<Int?>(null) }
    val shownIndex = scrub ?: safeIndex
    val isRtl = direction == ChromeReadingDirection.RIGHT_TO_LEFT
    val prevIcon = if (isRtl) IridiumIcons.SkipNext else IridiumIcons.SkipPrevious
    val nextIcon = if (isRtl) IridiumIcons.SkipPrevious else IridiumIcons.SkipNext
    val prevEnabled = canChromeGoBackward(safeIndex)
    val nextEnabled = canChromeGoForward(safeIndex, positionCount)
    val prevContainer = MaterialTheme.colorScheme.surfaceContainerHigh
        .copy(alpha = if (prevEnabled) 1f else 0.4f)
    val prevContent = MaterialTheme.colorScheme.onSurface
        .copy(alpha = if (prevEnabled) 1f else 0.38f)
    val nextContainer = MaterialTheme.colorScheme.surfaceContainerHigh
        .copy(alpha = if (nextEnabled) 1f else 0.4f)
    val nextContent = MaterialTheme.colorScheme.onSurface
        .copy(alpha = if (nextEnabled) 1f else 0.38f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(max = 840.dp)
            .fillMaxWidth()
            .testTag(ReaderChromeTestTags.ScrubberIsland),
    ) {
        AnimatedVisibility(
            visible = scrub != null,
            enter = fadeIn(IridiumMotion.defaultEffectsSpec()) +
                scaleIn(IridiumMotion.defaultSpatialSpec(), initialScale = 0.85f),
            exit = fadeOut(IridiumMotion.defaultEffectsSpec()) +
                scaleOut(IridiumMotion.defaultSpatialSpec(), targetScale = 0.85f),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 6.dp,
                tonalElevation = 6.dp,
                modifier = Modifier.testTag(ReaderChromeTestTags.ScrubTooltip),
            ) {
                Text(
                    text = formatChromeCounter(shownIndex + 1, positionCount),
                    style = IridiumEmphasized.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }

        if (positionCount > 1) {
            CompositionLocalProvider(LocalLayoutDirection provides rowDirection) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            haptics(IridiumHaptic.Select)
                            onPrevious()
                        },
                        enabled = prevEnabled,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = prevContainer,
                            contentColor = prevContent,
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(ReaderChromeTestTags.ScrubPrev),
                    ) {
                        Icon(
                            imageVector = prevIcon,
                            contentDescription = "Previous position",
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 4.dp,
                        shadowElevation = 6.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = positionCount.coerceAtLeast(1).toString(),
                                    style = IridiumEmphasized.titleMedium,
                                    modifier = Modifier.alpha(0f)
                                        .clearAndSetSemantics { },
                                )
                                Text(
                                    text = (shownIndex + 1).toString(),
                                    style = IridiumEmphasized.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            // Force the LTR slider geometry even in RTL rows:
                            // the value domain (first -> last position) must
                            // not flip with layout direction.
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Slider(
                                    value = shownIndex.toFloat(),
                                    onValueChange = { raw ->
                                        val next = raw.roundToInt()
                                        if (next != scrub) haptics(IridiumHaptic.FrequentTick)
                                        scrub = next
                                    },
                                    onValueChangeFinished = {
                                        haptics(IridiumHaptic.Select)
                                        scrub?.let { onSeek(progressionForIndex(it, positionCount)) }
                                        scrub = null
                                    },
                                    valueRange = 0f..(positionCount - 1).toFloat(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag(ReaderChromeTestTags.ScrubSlider),
                                )
                            }
                            Text(
                                text = positionCount.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = {
                            haptics(IridiumHaptic.Select)
                            onNext()
                        },
                        enabled = nextEnabled,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = nextContainer,
                            contentColor = nextContent,
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag(ReaderChromeTestTags.ScrubNext),
                    ) {
                        Icon(
                            imageVector = nextIcon,
                            contentDescription = "Next position",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
    )
}

/**
 * EPUB bottom dock: flow / theme / TOC / highlights / settings in one
 * elevated segmented pill. Stateless; the host owns wiring into
 * [ReaderScreen] (a parallel worker owns that call site).
 */
@Composable
fun ReaderEpubDock(
    onFlowCycle: () -> Unit,
    onThemeClick: () -> Unit,
    onTocClick: () -> Unit,
    onHighlightsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberIridiumHaptics()
    val segmentShape = RoundedCornerShape(14.dp)

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
        ),
        modifier = modifier
            .widthIn(max = 480.dp)
            .fillMaxWidth()
            .testTag(ReaderChromeTestTags.EpubDock),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            FilledTonalIconButton(
                onClick = {
                    haptics(IridiumHaptic.Select)
                    onFlowCycle()
                },
                shape = segmentShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .size(44.dp)
                    .testTag(ReaderChromeTestTags.EpubFlowButton)
                    .semantics {
                        onClick(label = "Reading flow", action = null)
                        stateDescription = "Reading flow"
                    },
            ) {
                Icon(IridiumIcons.ViewAgenda, contentDescription = "Cycle reading flow")
            }

            DockDivider()

            FilledTonalIconButton(
                onClick = {
                    haptics(IridiumHaptic.Select)
                    onThemeClick()
                },
                shape = segmentShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .size(44.dp)
                    .testTag(ReaderChromeTestTags.EpubThemeButton)
                    .semantics {
                        onClick(label = "Reading theme", action = null)
                        stateDescription = "Reading theme"
                    },
            ) {
                Icon(IridiumIcons.Contrast, contentDescription = "Choose reading theme")
            }

            DockDivider()

            FilledTonalIconButton(
                onClick = {
                    haptics(IridiumHaptic.Select)
                    onTocClick()
                },
                shape = segmentShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .size(44.dp)
                    .testTag(ReaderChromeTestTags.EpubTocButton)
                    .semantics {
                        onClick(label = "Table of contents", action = null)
                        stateDescription = "Table of contents"
                    },
            ) {
                Icon(IridiumIcons.MenuBook, contentDescription = "Table of contents")
            }

            DockDivider()

            FilledTonalIconButton(
                onClick = {
                    haptics(IridiumHaptic.Select)
                    onHighlightsClick()
                },
                shape = segmentShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .size(44.dp)
                    .testTag(ReaderChromeTestTags.EpubHighlightsButton)
                    .semantics {
                        onClick(label = "Highlights", action = null)
                        stateDescription = "Highlights"
                    },
            ) {
                Icon(IridiumIcons.Highlight, contentDescription = "Highlights")
            }

            DockDivider()

            FilledTonalIconButton(
                onClick = {
                    haptics(IridiumHaptic.Select)
                    onSettingsClick()
                },
                shape = segmentShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .size(44.dp)
                    .testTag(ReaderChromeTestTags.EpubSettingsButton)
                    .semantics {
                        onClick(label = "Reader settings", action = null)
                        stateDescription = "Reader settings"
                    },
            ) {
                Icon(IridiumIcons.Settings, contentDescription = "Reader settings")
            }
        }
    }
}
