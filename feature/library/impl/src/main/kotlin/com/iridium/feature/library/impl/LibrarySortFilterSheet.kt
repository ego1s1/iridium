package com.iridium.feature.library.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumChoiceGroup
import com.iridium.core.designsystem.IridiumChoiceOption
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumSettingSwitch
import com.iridium.core.designsystem.IridiumSheet
import com.iridium.core.model.LibraryDisplay
import com.iridium.core.model.LibraryDisplayMode
import com.iridium.core.model.LibrarySortOrder

/** Sort/display sheet: choice groups + hide-errors switch. Reading-state
 * filters live in the quick chips, so the sheet intentionally has no
 * duplicate filter row. */
@Composable
internal fun LibrarySortFilterSheet(
    display: LibraryDisplay,
    onAction: (LibraryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    IridiumSheet(
        onDismiss = { onAction(LibraryAction.CloseFilter) },
        skipPartiallyExpanded = true,
        modifier = modifier.testTag(LibraryTestTags.SortFilterSheet),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.library_sheet_sort_by),
                style = IridiumEmphasized.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            IridiumChoiceGroup(
                options = LibrarySortOrder.entries.map {
                    IridiumChoiceOption(sortLabel(it))
                },
                selectedIndex = LibrarySortOrder.entries.indexOf(display.sortOrder),
                onSelect = { onAction(LibraryAction.SortSelected(LibrarySortOrder.entries[it])) },
                fillWidth = false,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.library_sheet_display),
                style = IridiumEmphasized.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            IridiumChoiceGroup(
                options = LibraryDisplayMode.entries.map {
                    IridiumChoiceOption(displayModeLabel(it))
                },
                selectedIndex = LibraryDisplayMode.entries.indexOf(display.displayMode),
                onSelect = {
                    onAction(LibraryAction.DisplayModeSelected(LibraryDisplayMode.entries[it]))
                },
                fillWidth = false,
            )
            if (display.displayMode != LibraryDisplayMode.LIST) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.library_sheet_columns),
                    style = IridiumEmphasized.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                IridiumChoiceGroup(
                    options = listOf(
                        IridiumChoiceOption(stringResource(R.string.library_columns_auto)),
                        IridiumChoiceOption("2"),
                        IridiumChoiceOption("3"),
                        IridiumChoiceOption("4"),
                        IridiumChoiceOption("5"),
                        IridiumChoiceOption("6"),
                    ),
                    selectedIndex = listOf(0, 2, 3, 4, 5, 6)
                        .indexOf(display.gridColumns)
                        .coerceAtLeast(0),
                    // Option position is not the column count: index 0 is
                    // auto (0), the rest are literal 2..6.
                    onSelect = {
                        val columns = listOf(0, 2, 3, 4, 5, 6)[it.coerceIn(0, 5)]
                        onAction(LibraryAction.GridColumnsSelected(columns))
                    },
                )
            }
            Spacer(Modifier.height(4.dp))
            IridiumSettingSwitch(
                title = stringResource(R.string.library_sheet_hide_errors),
                checked = display.hideErrors,
                onCheckedChange = { onAction(LibraryAction.ToggleHideErrors(it)) },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun sortLabel(sort: LibrarySortOrder): String = when (sort) {
    LibrarySortOrder.RECENTLY_ADDED -> stringResource(R.string.library_sort_recently_added)
    LibrarySortOrder.RECENTLY_READ -> stringResource(R.string.library_sort_recently_read)
    LibrarySortOrder.TITLE -> stringResource(R.string.library_sort_title)
    LibrarySortOrder.UNFINISHED_FIRST -> stringResource(R.string.library_sort_unfinished_first)
}

@Composable
private fun displayModeLabel(mode: LibraryDisplayMode): String = when (mode) {
    LibraryDisplayMode.COMPACT -> stringResource(R.string.library_display_compact)
    LibraryDisplayMode.COMFORTABLE -> stringResource(R.string.library_display_comfortable)
    LibraryDisplayMode.COVER_ONLY -> stringResource(R.string.library_display_cover_only)
    LibraryDisplayMode.LIST -> stringResource(R.string.library_display_list)
}
