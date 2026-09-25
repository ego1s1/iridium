package com.iridium.feature.reader.impl

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The store is the single source of truth for "safe to attach the navigator
 * host". These tests pin the flag machine; the `publish` transition itself is
 * exercised by the ViewModel tests, which drive a real open.
 */
class ReaderSessionStoreTest {

    private val store = ReaderSessionStore()

    @Test
    fun `a fresh store is not ready and nothing is in flight`() {
        assertFalse(store.sessionReady.value)
        assertFalse(store.openInFlight.value)
        assertNull(store.navigatorFactory)
    }

    @Test
    fun `starting an open blocks attachment until it publishes`() {
        store.beginOpen()

        assertFalse("no factory exists yet, so the host must not attach", store.sessionReady.value)
        assertTrue("a slow open must not look like a lost session", store.openInFlight.value)
        assertNull(store.navigatorFactory)
    }

    @Test
    fun `a failed open leaves nothing in flight and nothing ready`() {
        store.beginOpen()
        store.failOpen()

        assertFalse(store.sessionReady.value)
        assertFalse(store.openInFlight.value)
        assertNull(store.navigatorFactory)
    }

    @Test
    fun `clearing resets both flags`() {
        store.beginOpen()
        store.clear()

        assertFalse(store.sessionReady.value)
        assertFalse(store.openInFlight.value)
    }

    @Test
    fun `beginning a new open cannot leave the previous session attachable`() {
        store.beginOpen()
        // Whatever happened before, a new open must not be attachable until
        // it publishes: otherwise the host could bind to a stale factory.
        store.beginOpen()

        assertFalse(store.sessionReady.value)
        assertTrue(store.openInFlight.value)
    }

    @Test
    fun `events are delivered as a stream`() = runTest {
        store.emit(ReaderSessionEvent.NavigatorAttached)
        assertEquals(ReaderSessionEvent.NavigatorAttached, store.events.first())
    }
}
