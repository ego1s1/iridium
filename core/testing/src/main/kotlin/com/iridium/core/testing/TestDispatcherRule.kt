package com.iridium.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps `Dispatchers.Main` for a test dispatcher around each test.
 *
 * Defaults to [UnconfinedTestDispatcher] (eager execution) because the bulk
 * of the suite was written against it; debounced/delayed paths then run on
 * real time. Prefer [StandardTestDispatcher] for new timing-sensitive tests
 * so virtual time ([advanceTimeBy], [runCurrent]) controls the clock instead
 * of wall time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }

    /** Advances virtual time (no-op on [UnconfinedTestDispatcher]). */
    fun advanceTimeBy(delayMillis: Long) {
        (testDispatcher.scheduler).advanceTimeBy(delayMillis)
    }

    /** Runs tasks scheduled up to the current virtual time. */
    fun runCurrent() {
        testDispatcher.scheduler.runCurrent()
    }
}
