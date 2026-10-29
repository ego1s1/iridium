package com.iridium.feature.history.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.rememberIridiumHaptics

/** Week / month / year window selector above the activity chart. */
@Composable
internal fun HistoryRangeSelector(
    range: HistoryStatsRange,
    onSelect: (HistoryStatsRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = HistoryStatsRange.entries
    val haptics = rememberIridiumHaptics()
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = range == option,
                onClick = {
                    haptics(IridiumHaptic.Select)
                    onSelect(option)
                },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = when (option) {
                            HistoryStatsRange.WEEK -> stringResource(R.string.history_range_week)
                            HistoryStatsRange.MONTH -> stringResource(R.string.history_range_month)
                            HistoryStatsRange.YEAR -> stringResource(R.string.history_range_year)
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                modifier = Modifier.testTag(HistoryStatsTestTags.rangeFor(option)),
            )
        }
    }
}

/** Bars past this count aggregate into weekly maxima (YEAR view). */
private const val MAX_CHART_BARS = 60

/** Rounded-bar chart of per-day read books, normalised to the peak. */
@Composable
internal fun HistoryActivityChart(
    buckets: List<HistoryDayBucket>,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val peakColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    if (buckets.isEmpty()) return
    val chartDescription = stringResource(R.string.history_stats_chart_desc)
    // A full year is 365 daily buckets in ~360dp: aggregate into weekly
    // maxima past ~60 bars so bars stay legible instead of overdrawing.
    val drawn = remember(buckets) {
        if (buckets.size <= MAX_CHART_BARS) {
            buckets
        } else {
            val size = (buckets.size + MAX_CHART_BARS - 1) / MAX_CHART_BARS
            buckets.chunked(size) { chunk ->
                chunk.first().copy(
                    booksActive = chunk.maxOf { it.booksActive },
                    booksFinished = chunk.sumOf { it.booksFinished },
                )
            }
        }
    }
    val maxActive = drawn.maxOf { it.booksActive }.coerceAtLeast(1)
    val peak = drawn.maxOf { it.booksActive }
    Canvas(
        modifier = modifier.semantics {
            this.contentDescription = chartDescription
            this.role = Role.Image
        },
    ) {
        val count = drawn.size
        val slot = size.width / count
        val barWidth = (slot * 0.6f).coerceAtLeast(2f)
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        drawn.forEachIndexed { index, bucket ->
            val ratio = bucket.booksActive.toFloat() / maxActive.toFloat()
            val barHeight = (size.height * ratio)
                .coerceAtLeast(if (bucket.booksActive > 0) 4f else 2f)
            val left = index * slot + (slot - barWidth) / 2f
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, size.height - 2f),
                size = Size(barWidth, 2f),
                cornerRadius = radius,
            )
            val isPeak = bucket.booksActive == peak && peak > 0
            drawRoundRect(
                color = when {
                    bucket.booksActive == 0 -> trackColor
                    isPeak -> peakColor
                    else -> barColor
                },
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}
