package com.iridium.feature.reader.impl

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.iridium.core.data.ReadiumOpener
import com.iridium.core.fakes.TestBooksRepository
import com.iridium.core.fakes.TestPreferencesDataSource
import com.iridium.core.testing.TestData
import com.iridium.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the "never attach the navigator host to a session that does not
 * exist" contract: a missing or unreadable book must leave the session neither
 * ready nor in flight, so the UI shows an error instead of composing a host
 * fragment with no navigator factory.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReaderViewModelTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private val repository = TestBooksRepository()
    private lateinit var preferences: TestPreferencesDataSource
    private lateinit var store: ReaderSessionStore
    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferences = TestPreferencesDataSource()
        store = ReaderSessionStore()
        viewModel = ReaderViewModel(
            savedStateHandle = SavedStateHandle(mapOf("bookId" to "book-1")),
            repository = repository,
            preferences = preferences,
            opener = ReadiumOpener(context),
            store = store,
        )
    }

    @Test
    fun `a book that is not in the library never becomes attachable`() = runTest {
        repository.setBooks(emptyList())

        viewModel.uiState.first { it == ReaderUiState.Gone }

        assertFalse("no publication was opened", store.sessionReady.value)
        assertFalse("nothing may be left in flight", store.openInFlight.value)
        assertFalse(viewModel.sessionReady.value)
    }

    // NOTE: the unreadable-file path (open fails -> failOpen -> OpenFailed) is
    // not exercised here on purpose. Driving it means calling Readium's
    // Streamer, which needs a real main looper and hangs under Robolectric.
    // The store half of that contract is covered by ReaderSessionStoreTest's
    // `a failed open leaves nothing in flight and nothing ready`.

    @Test
    fun `session readiness is exposed straight from the store`() = runTest {
        // The UI gate must read the same value the store publishes.
        assertFalse(viewModel.sessionReady.value)
        store.beginOpen()
        assertTrue(store.openInFlight.value)
        assertFalse(viewModel.sessionReady.value)
        store.failOpen()
        assertFalse(viewModel.sessionReady.value)
    }
}
