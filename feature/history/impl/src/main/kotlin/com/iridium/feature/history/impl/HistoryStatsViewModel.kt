package com.iridium.feature.history.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iridium.core.data.BooksRepository
import com.iridium.core.model.LibraryQuery
import com.iridium.core.model.LibrarySortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@HiltViewModel
class HistoryStatsViewModel @Inject constructor(
    repository: BooksRepository,
) : ViewModel() {

    private val range = MutableStateFlow(HistoryStatsRange.WEEK)

    /** Zero-value snapshot; the bento renders it as its existing empty state. */
    private val emptySnapshot = HistoryStatsSnapshot(
        totals = HistoryTotals(0, 0, 0, 0L),
        buckets = emptyList(),
        range = HistoryStatsRange.WEEK,
        streak = HistoryStreak(0, 0),
        topBooks = emptyList(),
    )

    val uiState: StateFlow<HistoryStatsSnapshot> = combine(
        repository.observeLibrary(LibraryQuery(sortOrder = LibrarySortOrder.RECENTLY_READ)),
        range,
    ) { books, selected ->
        // One clock read inside toHistoryStats: buckets and streak agree.
        books.toHistoryStats(selected)
    }.catch { error ->
        // A failed read would otherwise cancel the state flow and leave the
        // bento on its initial value forever. The zero snapshot is the closest
        // existing state: the bento already renders it as "no reading yet",
        // and there is no dedicated error state to emit.
        if (error is CancellationException) throw error
        emit(emptySnapshot)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptySnapshot,
    )

    fun onRangeSelect(range: HistoryStatsRange) {
        this.range.update { range }
    }
}
