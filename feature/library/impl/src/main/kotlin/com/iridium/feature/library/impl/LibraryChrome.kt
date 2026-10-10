package com.iridium.feature.library.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.TopBarTitleStyle
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.model.LibraryFilter

/**
 * Mori-parity Library chrome for Iridium, adapted to EPUB semantics.
 *
 * Ports section 3.1 of Mori's UI hierarchy (collapsing top bar with a 28sp
 * title, search pill island, quick-filters) without touching the existing
 * [LibraryScreen]: these are standalone building blocks a future screen
 * revision can compose together.
 *
 * EPUB adaptations vs Mori (comic reader):
 * - Progress is a 0f..1f locator fraction, never a page index.
 * - Iridium has no user collections and no shelves carousel: filtering is
 *   the quick-filter row plus the sort/filter sheet.
 */
object LibraryChromeTestTags {
    const val TopBar = "libraryChromeTopBar"
    const val FilterButton = "libraryChromeFilterButton"
    const val FilterDot = "libraryChromeFilterDot"
    const val SearchIsland = "libraryChromeSearchIsland"
    const val SearchField = "libraryChromeSearchField"
    const val SearchClear = "libraryChromeSearchClear"
    const val QuickFilters = "libraryChromeQuickFilters"
    const val ActionLink = "libraryChromeActionLink"

    fun quickFilterChip(filter: LibraryFilter): String =
        "libraryChromeQuickFilter:${filter.name}"
}

/**
 * Collapsing screen header: heavy 28sp display title that shrinks away on
 * scroll, with sort/filter as a direct icon action. The container is fully
 * transparent so no band ever appears over the grid.
 *
 * Callers must wire `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`
 * on the Scaffold and share this [scrollBehavior] instance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryCollapsingTopBar(
    title: String,
    filterActive: Boolean,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(),
) {
    val haptics = rememberIridiumHaptics()
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = TopBarTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        },
        actions = {
            Box {
                FilledTonalIconButton(
                    onClick = {
                        haptics(IridiumHaptic.Select)
                        onFilterClick()
                    },
                    modifier = Modifier.testTag(LibraryChromeTestTags.FilterButton),
                ) {
                    Icon(
                        imageVector = IridiumIcons.Tune,
                        contentDescription = stringResource(R.string.library_action_sort_filter),
                    )
                }
                if (filterActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp)
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape,
                            )
                            .testTag(LibraryChromeTestTags.FilterDot),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
        scrollBehavior = scrollBehavior,
        modifier = modifier.testTag(LibraryChromeTestTags.TopBar),
    )
}

/**
 * Floating search pill island: a single-line field on an elevated pill that
 * floats over the grid. The clear button appears only while text is present.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySearchIsland(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search title or author",
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shadowElevation: Dp = 0.dp,
) {
    val haptics = rememberIridiumHaptics()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    Surface(
        color = containerColor,
        shadowElevation = shadowElevation,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.testTag(LibraryChromeTestTags.SearchIsland),
    ) {
        TextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = IridiumIcons.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            haptics(IridiumHaptic.Select)
                            onTextChange("")
                        },
                        modifier = Modifier.testTag(LibraryChromeTestTags.SearchClear),
                    ) {
                        Icon(
                            imageVector = IridiumIcons.Close,
                            contentDescription = stringResource(R.string.library_action_clear_search),
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = IridiumEmphasized.bodyLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
            ),
            shape = MaterialTheme.shapes.extraLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 56.dp)
                .testTag(LibraryChromeTestTags.SearchField),
        )
    }
}

/** Quick-filter options in display order: Mori parity plus Finished. */
private val QuickFilterOptions = listOf(
    LibraryFilter.ALL,
    LibraryFilter.IN_PROGRESS,
    LibraryFilter.UNREAD,
    LibraryFilter.FAVORITES,
    LibraryFilter.FINISHED,
)

@Composable
private fun quickFilterLabel(filter: LibraryFilter): String = when (filter) {
    LibraryFilter.ALL -> stringResource(R.string.library_filter_all)
    LibraryFilter.IN_PROGRESS -> stringResource(R.string.library_filter_in_progress)
    LibraryFilter.UNREAD -> stringResource(R.string.library_filter_unread)
    LibraryFilter.FINISHED -> stringResource(R.string.library_filter_finished)
    LibraryFilter.FAVORITES -> stringResource(R.string.library_filter_favorites)
}

/**
 * Segmented quick-filters: one-touch reading-state filtering without opening
 * the sort/filter sheet. The selected chip carries a bookmark glyph only for
 * Favorites, mirroring Mori.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryQuickFilters(
    selected: LibraryFilter,
    onSelect: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberIridiumHaptics()
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .testTag(LibraryChromeTestTags.QuickFilters),
    ) {
        QuickFilterOptions.forEach { filter ->
            val isSelected = filter == selected
            FilterChip(
                selected = isSelected,
                onClick = {
                    haptics(IridiumHaptic.Select)
                    onSelect(filter)
                },
                label = {
                    Text(
                        text = quickFilterLabel(filter),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                },
                leadingIcon = if (isSelected && filter == LibraryFilter.FAVORITES) {
                    {
                        Icon(
                            imageVector = IridiumIcons.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    null
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                border = null,
                modifier = Modifier.testTag(LibraryChromeTestTags.quickFilterChip(filter)),
            )
        }
    }
}
