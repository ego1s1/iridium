package com.iridium.feature.library.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.LibraryFilter
import com.iridium.core.model.LibrarySortOrder

/** Sort/filter sheet: FilterChip FlowRows + hide-errors switch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibrarySortFilterSheet(
    display: LibraryDisplay,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = { onAction(LibraryAction.CloseFilter) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text("Sort by", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LibrarySortOrder.entries.forEach { sort ->
                    FilterChip(
                        selected = display.sortOrder == sort,
                        onClick = { onAction(LibraryAction.SortSelected(sort)) },
                        label = { Text(sortLabel(sort)) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Show", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LibraryFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = display.filter == filter,
                        onClick = { onAction(LibraryAction.FilterSelected(filter)) },
                        label = { Text(filterLabel(filter)) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Hide unreadable books",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = display.hideErrors,
                    onCheckedChange = { onAction(LibraryAction.ToggleHideErrors(it)) },
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun sortLabel(sort: LibrarySortOrder): String = when (sort) {
    LibrarySortOrder.RECENTLY_ADDED -> "Recently added"
    LibrarySortOrder.RECENTLY_READ -> "Recently read"
    LibrarySortOrder.TITLE -> "Title"
    LibrarySortOrder.UNFINISHED_FIRST -> "Unfinished first"
}

private fun filterLabel(filter: LibraryFilter): String = when (filter) {
    LibraryFilter.ALL -> "All"
    LibraryFilter.IN_PROGRESS -> "In progress"
    LibraryFilter.UNREAD -> "Unread"
    LibraryFilter.FINISHED -> "Finished"
    LibraryFilter.FAVORITES -> "Favorites"
}
