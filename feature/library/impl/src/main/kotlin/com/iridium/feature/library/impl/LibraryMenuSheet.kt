package com.iridium.feature.library.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iridium.core.designsystem.IridiumAlertDialog
import com.iridium.core.designsystem.IridiumEmphasized
import com.iridium.core.designsystem.IridiumHaptic
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumMotion
import com.iridium.core.designsystem.IridiumPrimaryButton
import com.iridium.core.designsystem.IridiumSettingRow
import com.iridium.core.designsystem.IridiumSheet
import com.iridium.core.designsystem.IridiumTonalButton
import com.iridium.core.designsystem.rememberIridiumHaptics
import com.iridium.core.model.Book
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Long-press quick actions for one book: read/resume, details, bookmark,
 * and remove (behind a confirm dialog). Title in the Flex emphasized face;
 * rows share the settings row language.
 */
@Composable
fun LibraryMenuSheet(
    book: Book,
    deleteConfirm: Boolean,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (bookId: String) -> Unit,
    onDetailsClick: (bookId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    IridiumSheet(
        onDismiss = { onAction(LibraryAction.CloseMenu) },
        // Short menu: open fully so every row is tappable instead of
        // peeking half-expanded with rows below the fold.
        skipPartiallyExpanded = true,
        modifier = modifier.testTag(LibraryTestTags.MenuSheet),
    ) {
        LibraryMenuContent(
            book = book,
            deleteConfirm = deleteConfirm,
            onAction = onAction,
            onReadClick = onReadClick,
            onDetailsClick = onDetailsClick,
        )
    }
}

/** Sheet body, exposed for testing without the modal wrapper. */
@Composable
internal fun LibraryMenuContent(
    book: Book,
    deleteConfirm: Boolean,
    onAction: (LibraryAction) -> Unit,
    onReadClick: (bookId: String) -> Unit,
    onDetailsClick: (bookId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberIridiumHaptics()
    val scope = rememberCoroutineScope()
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Text(
            text = book.title,
            style = IridiumEmphasized.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(LibraryTestTags.MenuTitle),
        )
        IridiumSettingRow(
            title = stringResource(
                if (book.isInProgress) {
                    R.string.library_menu_resume
                } else {
                    R.string.library_menu_read
                },
            ),
            subtitle = stringResource(R.string.library_menu_progress, (book.progress * 100).toInt()),
            icon = IridiumIcons.Play,
            onClick = {
                haptics(IridiumHaptic.PrimaryAction)
                onAction(LibraryAction.CloseMenu)
                onReadClick(book.id)
            },
            modifier = Modifier.testTag(LibraryTestTags.MenuRead),
        )
        IridiumSettingRow(
            title = stringResource(R.string.library_menu_details),
            subtitle = stringResource(R.string.library_card_details),
            icon = IridiumIcons.MenuBook,
            onClick = {
                haptics(IridiumHaptic.Select)
                onAction(LibraryAction.CloseMenu)
                // Let the sheet exit before pushing: the shared-element
                // cover morph needs a clean stage, not an overlap.
                scope.launch {
                    delay(IridiumMotion.ExitScreenMs.toLong())
                    onDetailsClick(book.id)
                }
            },
            modifier = Modifier.testTag(LibraryTestTags.MenuDetails),
        )
        IridiumSettingRow(
            title = stringResource(
                if (book.bookmarked) {
                    R.string.library_menu_bookmark_remove
                } else {
                    R.string.library_menu_bookmark_add
                },
            ),
            subtitle = stringResource(R.string.library_card_bookmarked),
            icon = if (book.bookmarked) {
                IridiumIcons.Bookmark
            } else {
                IridiumIcons.BookmarkBorder
            },
            onClick = {
                haptics(
                    if (book.bookmarked) {
                        IridiumHaptic.ToggleOff
                    } else {
                        IridiumHaptic.ToggleOn
                    },
                )
                onAction(LibraryAction.ToggleMenuBookmark)
            },
            modifier = Modifier.testTag(LibraryTestTags.MenuBookmark),
        )
        IridiumSettingRow(
            title = stringResource(R.string.library_menu_delete),
            subtitle = stringResource(R.string.library_menu_delete_title),
            icon = IridiumIcons.Delete,
            titleColor = MaterialTheme.colorScheme.error,
            iconTint = MaterialTheme.colorScheme.error,
            onClick = { onAction(LibraryAction.OpenMenuDelete) },
            modifier = Modifier.testTag(LibraryTestTags.MenuDelete),
        )
    }
    if (deleteConfirm) {
        IridiumAlertDialog(
            onDismissRequest = { onAction(LibraryAction.CloseMenu) },
            icon = IridiumIcons.Delete,
            title = {
                Text(
                    text = stringResource(R.string.library_menu_delete_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.library_menu_delete_body, book.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                IridiumPrimaryButton(
                    onClick = {
                        onAction(LibraryAction.ConfirmMenuDelete)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    haptic = IridiumHaptic.Confirm,
                    modifier = Modifier.testTag(LibraryTestTags.MenuDeleteConfirm),
                ) {
                    Text(text = stringResource(R.string.library_menu_delete_confirm))
                }
            },
            dismissButton = {
                IridiumTonalButton(onClick = { onAction(LibraryAction.CloseMenu) }) {
                    Text(stringResource(R.string.library_dialog_cancel))
                }
            },
            modifier = Modifier.testTag(LibraryTestTags.MenuDeleteDialog),
        )
    }
}
