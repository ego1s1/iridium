package com.iridium.feature.history.impl

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.iridium.core.data.BooksRepository
import com.iridium.core.designsystem.BookCoverArt
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumEnter
import com.iridium.core.designsystem.IridiumEnterKind
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumSectionCard
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.LibrarySortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

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

/**
 * Stats bento over Iridium history data, per Mori UI hierarchy §3.4.
 * Aggregates the history book list (progress / updatedAt / spineCount) into
 * the Mori bento shape: hero reading-time card, segments-read +
 * finished-volumes cards, activity chart, streak card, top-books list.
 */
@HiltViewModel
class HistoryStatsViewModel @Inject constructor(
    repository: BooksRepository,
) : ViewModel() {

    private val range = MutableStateFlow(HistoryStatsRange.WEEK)

    val uiState: StateFlow<HistoryStatsSnapshot> = combine(
        repository.observeLibrary(LibraryQuery(sortOrder = LibrarySortOrder.RECENTLY_READ)),
        range,
    ) { books, selected ->
        // One clock read inside toHistoryStats: buckets and streak agree.
        books.toHistoryStats(selected)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryStatsSnapshot(
            totals = HistoryTotals(0, 0, 0, 0L),
            buckets = emptyList(),
            range = HistoryStatsRange.WEEK,
            streak = HistoryStreak(0, 0),
            topBooks = emptyList(),
        ),
    )

    fun onRangeSelect(range: HistoryStatsRange) {
        this.range.update { range }
    }
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
 * Mori §3.4 bento grid: hero card, companion cards, activity chart with
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
                text = "Books you read will build your stats here.",
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
                label = "Segments read",
                value = snapshot.totals.segmentsRead.toString(),
                icon = IridiumIcons.MenuBook,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag(HistoryStatsTestTags.Segments),
            )
            CompanionStatCard(
                label = "Books finished",
                value = snapshot.totals.booksFinished.toString(),
                icon = IridiumIcons.Check,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .weight(1f)
                    .testTag(HistoryStatsTestTags.Finished),
            )
        }
        IridiumSectionCard(title = "Activity") {
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
                text = "Most read",
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
    }
}

/** Spotlight hero card for estimated reading time (Mori §3.4 primary hero). */
@Composable
private fun ReadingTimeHeroCard(
    durationMs: Long,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
        ) {
            val numberEnter = IridiumEnter.enter(IridiumEnterKind.FADE_THROUGH)
            val numberExit = IridiumEnter.exit(IridiumEnterKind.FADE_THROUGH)
            AnimatedContent(
                targetState = formatHistoryDuration(durationMs),
                transitionSpec = { numberEnter togetherWith numberExit },
                label = "heroReadingTime",
            ) { duration ->
                Text(
                    text = duration,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = "Time reading · estimated",
                style = IridiumEmphasized.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
    }
}

/** Expressive bento companion card with dedicated container colors. */
@Composable
private fun CompanionStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        tonalElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(contentColor.copy(alpha = 0.12f), shape = CircleShape),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            val numberEnter = IridiumEnter.enter(IridiumEnterKind.FADE_THROUGH)
            val numberExit = IridiumEnter.exit(IridiumEnterKind.FADE_THROUGH)
            AnimatedContent(
                targetState = value,
                transitionSpec = { numberEnter togetherWith numberExit },
                label = "companionValue",
            ) { current ->
                Text(
                    text = current,
                    style = MaterialTheme.typography.displayMedium,
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = label,
                style = IridiumEmphasized.labelLarge,
                color = contentColor.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

/** Week / month / year window selector above the activity chart. */
@Composable
private fun HistoryRangeSelector(
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
                            HistoryStatsRange.WEEK -> "7D"
                            HistoryStatsRange.MONTH -> "30D"
                            HistoryStatsRange.YEAR -> "1Y"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                modifier = Modifier.testTag(HistoryStatsTestTags.rangeFor(option)),
            )
        }
    }
}

/** Rounded-bar chart of per-day read books, normalised to the peak. */
@Composable
private fun HistoryActivityChart(
    buckets: List<HistoryDayBucket>,
    modifier: Modifier = Modifier,
) {
    val barColor = MaterialTheme.colorScheme.primary
    val peakColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    if (buckets.isEmpty()) return
    val maxActive = buckets.maxOf { it.booksActive }.coerceAtLeast(1)
    val peak = buckets.maxOf { it.booksActive }
    Canvas(
        modifier = modifier.semantics {
            this.contentDescription = "Bar chart of daily reading activity"
            this.role = Role.Image
        },
    ) {
        val count = buckets.size
        val slot = size.width / count
        val barWidth = (slot * 0.6f).coerceAtLeast(2f)
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        buckets.forEachIndexed { index, bucket ->
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

/** Streak journey card with current and longest records. */
@Composable
private fun HistoryStreakCard(
    streak: HistoryStreak,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Streak",
                    style = IridiumEmphasized.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp),
                        ),
                ) {
                    Icon(
                        imageVector = IridiumIcons.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = "${streak.current} days",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
            Text(
                text = "Longest: ${streak.longest} days",
                style = IridiumEmphasized.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Ranked cover row with percent-read detail. */
@Composable
private fun HistoryTopBookRow(
    top: HistoryTopBook,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
            )
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(64.dp),
        ) {
            BookCoverArt(
                coverPath = top.book.coverPath,
                contentDescription = top.book.title,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = top.book.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            Text(
                text = "${top.percentRead}% read",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Compact hero duration: 2h 15m / 45m / 30s. */
private fun formatHistoryDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}
