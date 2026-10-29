package com.iridium.feature.reader.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumMotion
import com.iridium.core.designsystem.rememberIridiumHaptics
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/** Tap-zone outcome for a tap inside the reading surface. */
enum class ChromeTapZone {
    PREV,
    MENU,
    NEXT,
}

/** Test tags for the reader chrome. */
internal object ReaderChromeTestTags {
    const val ScrubberIsland = "chromeScrubberIsland"
    const val ScrubTooltip = "chromeScrubTooltip"
    const val ScrubSlider = "chromeScrubSlider"
    const val TapZoneInvertSwitch = "tapZoneInvertSwitch"
    const val ChapterPill = "chromeChapterPill"
    const val PageLoading = "readerPageLoading"
}

// Pure helpers (unit-testable without composition)

/** 0-based position index for a 0f..1f progression over [count] positions. */
internal fun scrubIndexForProgression(progression: Float, count: Int): Int {
    if (count <= 1) return 0
    return (progression.coerceIn(0f, 1f) * (count - 1)).roundToInt()
        .coerceIn(0, count - 1)
}

/** 0f..1f progression for a 0-based position [index] over [count] positions. */
internal fun progressionForIndex(index: Int, count: Int): Float {
    if (count <= 1) return 0f
    return index.coerceIn(0, count - 1).toFloat() / (count - 1).toFloat()
}

/** Warm scrim color for night light at [intensity] 0f..1f (max 40% alpha). */
internal fun nightLightColor(intensity: Float): Color {
    val alpha = (intensity.coerceIn(0f, 1f) * MaxNightLightAlpha * 255f)
        .roundToInt().coerceIn(0, 255)
    return Color(red = 0xFF, green = 0x9E, blue = 0x42, alpha = alpha)
}

private const val MaxNightLightAlpha = 0.4f

/**
 * Static Material 3 Expressive progress track: a rounded inactive rail, a
 * filled active segment and a stop indicator at the end. No squiggle or
 * motion — the scrubber reads as a progress bar that happens to be draggable.
 */
@Composable
private fun ExpressiveProgressTrack(fraction: Float) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.surfaceVariant
    val stop = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(TrackHeight),
    ) {
        val h = size.height
        val radius = CornerRadius(h / 2f, h / 2f)
        drawRoundRect(color = inactive, cornerRadius = radius)
        val filled = (size.width * fraction.coerceIn(0f, 1f)).coerceAtLeast(h)
        drawRoundRect(
            color = active,
            size = Size(filled, h),
            cornerRadius = radius,
        )
        // Stop indicator: the M3 Expressive end-cap dot.
        drawCircle(
            color = stop,
            radius = h / 3f,
            center = Offset(size.width - h / 2f, h / 2f),
        )
    }
}

/** Height of the scrubber progress rail. */
private val TrackHeight = 8.dp

/** "12 / 173" style counter; [oneBased] is 1-based. */
internal fun formatChromeCounter(oneBased: Int, total: Int): String = "$oneBased / $total"

/**
 * Resolves which tap zone a tap at ([fractionX], [fractionY]) falls into:
 * fixed horizontal thirds (outer partitions navigate, the center toggles
 * chrome). The invert-taps switch mirrors the thirds for reachability (left
 * taps turn forward, right taps back); the reading direction does not mirror
 * the zones. Taps landing on the top chrome strip always toggle.
 */
internal fun chromeZoneForTap(
    fractionX: Float,
    fractionY: Float,
    invertTaps: Boolean = false,
): ChromeTapZone {
    val x = if (invertTaps) {
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
 * Floating scrubber island: position counter + full-width slider, with a
 * live tooltip bubble while scrubbing. Stateless and driven by callbacks;
 * the value domain (first -> last position) never mirrors with layout
 * direction, so the whole island is pinned to LTR.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScrubberIsland(
    positionIndex: Int,
    positionCount: Int,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    chapterTitle: String? = null,
    onScrubbingChanged: (Boolean) -> Unit = {},
) {
    val haptics = rememberIridiumHaptics()
    val safeIndex = positionIndex.coerceIn(0, (positionCount - 1).coerceAtLeast(0))
    // Holds the drag value until the navigator echo converges: clearing on
    // release snaps the thumb back to the pre-seek page for ~500ms while
    // nav.go() lands. Converge-or-timeout instead.
    var scrub by remember { mutableStateOf<Int?>(null) }
    var dragStart by remember { mutableStateOf<Int?>(null) }
    var lastTickBucket by remember { mutableStateOf(Int.MIN_VALUE) }
    val shownIndex = (scrub ?: safeIndex).coerceIn(0, (positionCount - 1).coerceAtLeast(0))

    // Echo converged (or table shrank under us): release the hold.
    LaunchedEffect(safeIndex, positionCount) {
        val held = scrub
        if (held != null) {
            if (held == safeIndex || held >= positionCount || safeIndex >= positionCount - 1 && held >= positionCount - 1) {
                scrub = null
                dragStart = null
                lastTickBucket = Int.MIN_VALUE
                onScrubbingChanged(false)
            }
        }
        if (positionCount <= 1 && scrub != null) {
            scrub = null
            dragStart = null
            onScrubbingChanged(false)
        }
    }
    // Stuck-echo timeout: never trap the thumb if nav.go() never lands.
    LaunchedEffect(scrub) {
        if (scrub != null) {
            delay(2500L)
            if (scrub != null) {
                scrub = null
                dragStart = null
                lastTickBucket = Int.MIN_VALUE
                onScrubbingChanged(false)
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(max = 840.dp)
            .fillMaxWidth()
            .testTag(ReaderChromeTestTags.ScrubberIsland),
    ) {
        AnimatedVisibility(
            visible = scrub != null,
            enter = fadeIn(IridiumMotion.chromeEnterSpec()) +
                scaleIn(IridiumMotion.chromeEnterSpec(), initialScale = 0.9f),
            exit = fadeOut(IridiumMotion.chromeExitSpec()) +
                scaleOut(IridiumMotion.chromeExitSpec(), targetScale = 0.9f),
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
            // Chapter label pinned to the island's leading edge: a small pill
            // above the track, so it reads as attached to the scrubber
            // without competing with the page counter inside it.
            chapterTitle?.takeIf { it.isNotBlank() }?.let { title ->
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 12.dp, bottom = 6.dp)
                        .testTag(ReaderChromeTestTags.ChapterPill),
                ) {
                    Text(
                        text = title,
                        style = IridiumEmphasized.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
            // Reading direction does not flip the scrubber: always LTR order.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                    ) {
                        // Fixed-width slot keeps the slider from shifting as
                        // the counter ticks; width is hidden from semantics.
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = positionCount.coerceAtLeast(1).toString(),
                                style = IridiumEmphasized.titleMedium,
                                modifier = Modifier.alpha(0f)
                                    .clearAndSetSemantics { },
                            )
                            AnimatedContent(
                                targetState = shownIndex,
                                transitionSpec = {
                                    // Quick crossfade while dragging: springs
                                    // wobble when retargeted every step.
                                    fadeIn(IridiumMotion.chromeEnterSpec()) togetherWith
                                        fadeOut(IridiumMotion.chromeExitSpec())
                                },
                                label = "scrubCounter",
                            ) { index ->
                                Text(
                                    text = (index + 1).toString(),
                                    style = IridiumEmphasized.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        val scrubInteraction = remember { MutableInteractionSource() }
                        Slider(
                            value = shownIndex.toFloat(),
                            onValueChange = { raw ->
                                val next = raw.roundToInt().coerceIn(0, (positionCount - 1).coerceAtLeast(0))
                                if (dragStart == null) {
                                    dragStart = safeIndex
                                    onScrubbingChanged(true)
                                }
                                // Quantized ticks (~20 buckets): per-page ticks
                                // buzz on 1000-position books and scale the
                                // annoyance with book length.
                                if (next != scrub) {
                                    val segments = 20
                                    val bucket = if (positionCount > 1) {
                                        (next * segments / (positionCount - 1)).coerceIn(0, segments)
                                    } else {
                                        0
                                    }
                                    if (bucket != lastTickBucket) {
                                        lastTickBucket = bucket
                                        haptics(IridiumHaptic.FrequentTick)
                                    }
                                    scrub = next
                                }
                            },
                            onValueChangeFinished = {
                                val target = scrub
                                val start = dragStart
                                // Only confirm when a seek is actually emitted:
                                // a tap-without-move is a no-op, not a select.
                                if (target != null && target != start) {
                                    haptics(IridiumHaptic.Select)
                                    onSeek(progressionForIndex(target, positionCount))
                                    // Hold `scrub` until the echo converges
                                    // (cleared by LaunchedEffect above).
                                } else {
                                    scrub = null
                                    dragStart = null
                                    lastTickBucket = Int.MIN_VALUE
                                    onScrubbingChanged(false)
                                }
                            },
                            valueRange = 0f..(positionCount - 1).toFloat(),
                            interactionSource = scrubInteraction,
                            thumb = { _ ->
                                // Minimal handle: the track carries the state.
                                Box(
                                    Modifier
                                        .size(width = 4.dp, height = 22.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onPrimary,
                                            RoundedCornerShape(2.dp),
                                        ),
                                )
                            },
                            track = { state ->
                                ExpressiveProgressTrack(
                                    fraction = progressionForIndex(
                                        shownIndex,
                                        positionCount,
                                    ),
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag(ReaderChromeTestTags.ScrubSlider),
                        )
                        Text(
                            text = positionCount.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
