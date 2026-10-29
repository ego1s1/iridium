package com.iridium.feature.history.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iridium.core.data.BooksRepository
import com.iridium.core.model.Book
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.LibrarySortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: BooksRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = repository
        .observeLibrary(LibraryQuery(sortOrder = LibrarySortOrder.RECENTLY_READ))
        .map { books ->
            HistoryUiState(books.filter { it.progress > 0f }.groupByDay())
        }
        .catch { error ->
            // A failed read would otherwise cancel the state flow and leave the
            // tab on its initial (loading) value forever. Empty history is the
            // closest existing state: the screen already renders it as "no
            // history yet", and there is no dedicated error state to emit.
            if (error is CancellationException) throw error
            emit(HistoryUiState())
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
    // Calendar arithmetic, not minus-24h: DST transitions make yesterday
    // 23 or 25 hours long.
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val startOfYesterday = calendar.timeInMillis

    return groupBy { book ->
        when {
            book.updatedAt >= startOfToday -> HistoryGroupKey.Today
            book.updatedAt >= startOfYesterday -> HistoryGroupKey.Yesterday
            else -> HistoryGroupKey.Dated(startOfDay(book.updatedAt))
        }
    }.map { (key, books) ->
        HistoryGroup(key, books.sortedByDescending { it.updatedAt })
    }
}

/** Midnight in the device's zone for [millis], used to bucket dated groups. */
private fun startOfDay(millis: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
