package com.iridium.feature.reader.impl

import androidx.lifecycle.SavedStateHandle
import com.iridium.core.data.OpenResult
import com.iridium.core.fakes.FakeOpener
import com.iridium.core.fakes.TestBooksRepository
import com.iridium.core.fakes.TestPreferencesDataSource
import com.iridium.core.testing.TestData
import com.iridium.core.testing.TestDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
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
    private val opener = FakeOpener()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Before
    fun setup() {
        preferences = TestPreferencesDataSource()
        store = ReaderSessionStore()
        viewModel = ReaderViewModel(
            savedStateHandle = SavedStateHandle(mapOf("bookId" to "book-1")),
            repository = repository,
            preferences = preferences,
            opener = opener,
            dictionaryLookup = FakeDictionaryLookup(),
            store = store,
            appScope = appScope,
        )
    }

    @After
    fun teardown() {
        appScope.cancel()
    }

    @Test
    fun `missing file routes to OpenFailed with fileMissing`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "book-1")))
        opener.failure = OpenResult.FileMissing
        val missing = ReaderViewModel(
            savedStateHandle = SavedStateHandle(mapOf("bookId" to "book-1")),
            repository = repository,
            preferences = preferences,
            opener = opener,
            dictionaryLookup = FakeDictionaryLookup(),
            store = store,
            appScope = appScope,
        )

        val state = missing.uiState.first { it is ReaderUiState.OpenFailed }
        assertTrue((state as ReaderUiState.OpenFailed).fileMissing)
    }

    @Test
    fun `parse failure routes to OpenFailed without fileMissing`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "book-1")))
        opener.failure = OpenResult.ParseFailed
        val broken = ReaderViewModel(
            savedStateHandle = SavedStateHandle(mapOf("bookId" to "book-1")),
            repository = repository,
            preferences = preferences,
            opener = opener,
            dictionaryLookup = FakeDictionaryLookup(),
            store = store,
            appScope = appScope,
        )

        val state = broken.uiState.first { it is ReaderUiState.OpenFailed }
        assertFalse((state as ReaderUiState.OpenFailed).fileMissing)
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

    @Test
    fun `tap invert and night light prefs persist through actions`() = runTest {
        viewModel.onAction(ReaderAction.SetInvertTaps(true))
        viewModel.onAction(ReaderAction.SetNightLight(true))
        viewModel.onAction(ReaderAction.SetNightLightIntensity(0.6f))

        val prefs = preferences.readerPreferences.first()
        assertTrue(prefs.invertTaps)
        assertTrue(prefs.nightLight)
        assertEquals(0.6f, prefs.nightLightIntensity, 0.0001f)
    }

    @Test
    fun `night light intensity clamps to the 0 to 1 range`() = runTest {
        viewModel.onAction(ReaderAction.SetNightLightIntensity(99f))
        assertEquals(1f, preferences.readerPreferences.first().nightLightIntensity, 0.0001f)
        viewModel.onAction(ReaderAction.SetNightLightIntensity(-99f))
        assertEquals(0f, preferences.readerPreferences.first().nightLightIntensity, 0.0001f)
    }
}
