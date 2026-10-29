package com.iridium.feature.history.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumSectionCard

/** Test tags for the history stats bento. */
object HistoryStatsTestTags {
    const val Content = "historyStatsContent"
    const val EmptyState = "historyStatsEmpty"
    const val Hero = "historyStatsHero"
    const val Segments = "historyStatsSegments"
    const val Finished = "historyStatsFinished"
    const val Chart = "historyStatsChart"
    const val Streak = "historyStatsStreak"
    const val TopBooks = "historyStatsTopBooks"

    fun rangeFor(range: HistoryStatsRange): String = "historyStatsRange:${range.name}"
}

/** Route entry: bento grid bound to [HistoryStatsViewModel]. */
@Composable
fun HistoryStatsBentoRoute(
    modifier: Modifier = Modifier,
    viewModel: HistoryStatsViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryStatsBentoContent(
        snapshot = snapshot,
        onRangeSelect = viewModel::onRangeSelect,
        modifier = modifier,
    )
}

/**
 * the reference §3.4 bento grid: hero card, companion cards, activity chart with
 * range selector, streak card, top-books list.
 */
@Composable
fun HistoryStatsBentoContent(
    snapshot: HistoryStatsSnapshot,
    onRangeSelect: (HistoryStatsRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (snapshot.totals.booksStarted == 0) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .testTag(HistoryStatsTestTags.EmptyState),
        ) {
            Text(
                text = stringResource(R.string.history_stats_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(HistoryStatsTestTags.Content),
    ) {
        ReadingTimeHeroCard(
            durationMs = snapshot.totals.estimatedReadingMs,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HistoryStatsTestTags.Hero),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompanionStatCard(
                label = stringResource(R.string.history_stats_segments),
                value = snapshot.totals.segmentsRead.toString(),
                icon = IridiumIcons.MenuBook,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag(HistoryStatsTestTags.Segments),
            )
            CompanionStatCard(
                label = stringResource(R.string.history_stats_finished),
                value = snapshot.totals.booksFinished.toString(),
                icon = IridiumIcons.Check,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag(HistoryStatsTestTags.Finished),
            )
        }
        IridiumSectionCard(title = stringResource(R.string.history_stats_activity)) {
            HistoryRangeSelector(
                range = snapshot.range,
                onSelect = onRangeSelect,
            )
            HistoryActivityChart(
                buckets = snapshot.buckets,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .padding(top = 12.dp)
                    .testTag(HistoryStatsTestTags.Chart),
            )
        }
        HistoryStreakCard(
            streak = snapshot.streak,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(HistoryStatsTestTags.Streak),
        )
        if (snapshot.topBooks.isNotEmpty()) {
            Text(
                text = stringResource(R.string.history_stats_most_read),
                style = IridiumEmphasized.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag(HistoryStatsTestTags.TopBooks),
            )
            snapshot.topBooks.forEach { top ->
                HistoryTopBookRow(top = top)
            }
        }
        // Clearance for the floating pill navigator, which overlays the
        // scrollable content (same 96dp reservation as Settings).
        Spacer(Modifier.height(96.dp))
    }
}
