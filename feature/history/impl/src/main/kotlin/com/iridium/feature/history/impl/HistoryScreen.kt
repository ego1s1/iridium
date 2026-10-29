package com.iridium.feature.history.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iridium.core.designsystem.BookCoverArt
import com.iridium.core.designsystem.IridiumCollapsingTopBar
import com.iridium.core.model.Book
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Bucket a history entry falls into; the label is resolved in the UI layer
 * so grouping stays locale-free and unit-testable. */
sealed interface HistoryGroupKey {
    data object Today : HistoryGroupKey
    data object Yesterday : HistoryGroupKey
    data class Dated(val startOfDayMillis: Long) : HistoryGroupKey
}

/** A day's worth of recently read books, newest first. */
data class HistoryGroup(val key: HistoryGroupKey, val books: List<Book>)

data class HistoryUiState(val groups: List<HistoryGroup> = emptyList())

/** Localised header for a history bucket. */
@Composable
private fun historyGroupLabel(key: HistoryGroupKey): String = when (key) {
    HistoryGroupKey.Today -> stringResource(R.string.history_today)
    HistoryGroupKey.Yesterday -> stringResource(R.string.history_yesterday)
    is HistoryGroupKey.Dated -> remember(key.startOfDayMillis) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            .format(Date(key.startOfDayMillis))
    }
}


/** Public tab content for the main viewport. */
@Composable
fun HistoryTabContent(
    onReadClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    HistoryRoute(
        onReadClick = onReadClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryRoute(
    onReadClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        topBar = {
            IridiumCollapsingTopBar(
                title = stringResource(R.string.history_title),
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, top = 8.dp, end = 16.dp, bottom = 112.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "stats", contentType = "stats") {
                    HistoryStatsBentoRoute(modifier = Modifier.fillMaxWidth())
                }
                if (state.groups.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.history_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(32.dp),
                            )
                        }
                    }
                } else {
                    state.groups.forEach { group ->
                        item(key = "header-${group.key}", contentType = "header") {
                            Text(
                                text = historyGroupLabel(group.key),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                            )
                        }
                        items(group.books, key = { it.id }, contentType = { "row" }) { book ->
                            HistoryRow(
                                book = book,
                                onClick = { onReadClick(book.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp),
        ) {
            Box(
                Modifier
                    .size(width = 48.dp, height = 72.dp)
                    .clip(RoundedCornerShape(6.dp)),
            ) {
                BookCoverArt(coverPath = book.coverPath, contentDescription = book.title)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                book.author?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                val progress = book.progress.coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.history_progress, (progress * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
