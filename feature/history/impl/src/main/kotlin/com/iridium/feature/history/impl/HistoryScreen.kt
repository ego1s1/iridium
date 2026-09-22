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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.iridium.core.data.BooksRepository
import com.iridium.core.designsystem.BookCoverArt
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.model.Book
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.LibrarySortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** A day's worth of recently read books, newest first. */
data class HistoryGroup(val label: String, val books: List<Book>)

data class HistoryUiState(val groups: List<HistoryGroup> = emptyList())

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: BooksRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = repository
        .observeLibrary(LibraryQuery(sortOrder = LibrarySortOrder.RECENTLY_READ))
        .map { books ->
            HistoryUiState(books.filter { it.progress > 0f }.groupByDay())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )
}

/** Groups books into Today / Yesterday / dated buckets by last read. */
private fun List<Book>.groupByDay(now: Long = System.currentTimeMillis()): List<HistoryGroup> {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = now
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfToday = calendar.timeInMillis
    val startOfYesterday = startOfToday - DAY_MS

    val formatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    return groupBy { book ->
        when {
            book.updatedAt >= startOfToday -> "Today"
            book.updatedAt >= startOfYesterday -> "Yesterday"
            else -> formatter.format(Date(book.updatedAt))
        }
    }.map { (label, books) ->
        HistoryGroup(label, books.sortedByDescending { it.updatedAt })
    }
}

private const val DAY_MS = 24L * 60 * 60 * 1000

/** Public tab content for the main viewport. */
@Composable
fun HistoryTabContent(
    onReadClick: (String) -> Unit,
    onBookLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    HistoryRoute(
        onReadClick = onReadClick,
        onBookLongClick = onBookLongClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryRoute(
    onReadClick: (String) -> Unit,
    onBookLongClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("History", style = IridiumEmphasized.headlineSmall) })
        },
        modifier = modifier,
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            if (state.groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Books you read will show up here.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, top = 8.dp, end = 16.dp, bottom = 112.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    state.groups.forEach { group ->
                        item(key = "header-${group.label}", contentType = "header") {
                            Text(
                                text = group.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                            )
                        }
                        items(group.books, key = { it.id }, contentType = { "row" }) { book ->
                            HistoryRow(
                                book = book,
                                onClick = {
                                    if (book.error != null) onBookLongClick(book.id) else onReadClick(book.id)
                                },
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
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${(book.progress * 100).toInt()}% read",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
