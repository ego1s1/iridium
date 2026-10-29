package com.iridium.core.common

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeCatchingTest {

    @Test
    fun `success wraps the value`() {
        val result = safeCatching { 42 }
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `failure captures the exception`() {
        val result = safeCatching { error("boom") }
        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test
    fun `cancellation is rethrown instead of captured`() {
        var thrown: Throwable? = null
        try {
            safeCatching { throw CancellationException("cancelled") }
        } catch (e: CancellationException) {
            thrown = e
        }
        assertTrue("CancellationException must propagate", thrown is CancellationException)
    }
}
