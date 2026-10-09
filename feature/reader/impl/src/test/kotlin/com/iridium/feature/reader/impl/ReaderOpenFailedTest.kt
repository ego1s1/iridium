package com.iridium.feature.reader.impl

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.iridium.core.data.ReadiumOpener
import com.iridium.core.designsystem.IridiumEmptyState
import com.iridium.core.designsystem.IridiumIcons
import com.iridium.core.designsystem.IridiumTheme
import com.iridium.core.fakes.TestBooksRepository
import com.iridium.core.fakes.TestPreferencesDataSource
import com.iridium.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the reader open-failure dead-end ([ReaderUiState.OpenFailed]):
 * title/body/Back render and Back leaves explicitly (pop), never auto-pops.
 *
 * [ReaderOpenFailed] itself is private to ReaderScreen, so this mirrors its
 * exact wiring (same [IridiumEmptyState] copy) plus the [ReaderAction.Back]
 * -> [ReaderMessage.Pop] dispatch that [ReaderRoute] maps to onBackClick.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderOpenFailedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private fun setFailed(
        fileMissing: Boolean,
        onBackClick: () -> Unit = {},
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val title = context.getString(R.string.reader_open_failed_title)
        val body = context.getString(
            if (fileMissing) {
                R.string.reader_open_failed_body_missing
            } else {
                R.string.reader_open_failed_body_corrupt
            },
        )
        val back = context.getString(R.string.reader_open_failed_back)
        composeTestRule.setContent {
            IridiumTheme(expressiveMotion = false) {
                IridiumEmptyState(
                    icon = IridiumIcons.MenuBook,
                    title = title,
                    body = body,
                    actionLabel = back,
                    onAction = onBackClick,
                )
            }
        }
    }

    @Test
    fun `missing file renders the missing copy with a Back button`() {
        setFailed(fileMissing = true)

        composeTestRule.onNodeWithText("Couldn't open this book").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("The file is gone or unreadable. If you moved it, link its folder again from the library.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Back to library").assertIsDisplayed()
    }

    @Test
    fun `corrupt file renders the corrupt copy with a Back button`() {
        setFailed(fileMissing = false)

        composeTestRule.onNodeWithText("Couldn't open this book").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("This file looks corrupt or incomplete.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Back to library").assertIsDisplayed()
    }

    @Test
    fun `Back leaves explicitly through the callback`() {
        var backs = 0
        setFailed(fileMissing = true, onBackClick = { backs++ })

        composeTestRule.onNodeWithText("Back to library").performClick()

        assertEquals(1, backs)
    }

    @Test
    fun `OpenFailed distinguishes missing from corrupt`() {
        assertTrue(ReaderUiState.OpenFailed(fileMissing = true).fileMissing)
        assertEquals(false, ReaderUiState.OpenFailed(fileMissing = false).fileMissing)
        assertEquals(false, ReaderUiState.OpenFailed().fileMissing)
    }

    @Test
    fun `Back action dispatches pop`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = ReaderViewModel(
            savedStateHandle = SavedStateHandle(mapOf("bookId" to "book-1")),
            repository = TestBooksRepository(),
            preferences = TestPreferencesDataSource(),
            opener = ReadiumOpener(context),
            store = ReaderSessionStore(),
        )

        viewModel.onAction(ReaderAction.Back)

        assertEquals(ReaderMessage.Pop, viewModel.messages.first())
    }
}
