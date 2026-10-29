package com.iridium.feature.reader.impl

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The page-turn ceiling is a fixed rate, so these tests pin the exact numbers
 * that keep the slide from tearing: consecutive turns must clear the interval,
 * and a sustained rate cannot exceed the per-second cap.
 */
class TurnRateLimiterTest {

    @Test
    fun `a turn is accepted immediately`() {
        assertTrue(TurnRateLimiter().tryAcquire(0L))
    }

    @Test
    fun `taps closer than the interval are dropped`() {
        val limiter = TurnRateLimiter()
        assertTrue(limiter.tryAcquire(0L))
        // 180 ms is a full slide; the next tap must wait for the interval.
        assertFalse(limiter.tryAcquire(180L))
        assertTrue(limiter.tryAcquire(TurnRateLimiter.MIN_INTERVAL_MS))
    }

    @Test
    fun `sustained tapping cannot exceed four per second`() {
        val limiter = TurnRateLimiter()
        var accepted = 0
        // 100 ms apart is well inside the interval, so the window cap decides.
        for (i in 0 until 20) {
            val now = i * 100L
            if (limiter.tryAcquire(now)) accepted++
        }
        assertTrue("accepted $accepted turns in two seconds", accepted <= 8)
    }

    @Test
    fun `exactly four turns fit in one window`() {
        val limiter = TurnRateLimiter()
        val spaced = (0 until 4).map { it * TurnRateLimiter.MIN_INTERVAL_MS }
        for (at in spaced) {
            assertTrue("turn at $at ms", limiter.tryAcquire(at))
        }
        // The fifth inside the same window is refused even though it is spaced.
        assertFalse(limiter.tryAcquire(4 * TurnRateLimiter.MIN_INTERVAL_MS))
    }

    @Test
    fun `a pause resets the budget`() {
        val limiter = TurnRateLimiter()
        repeat(4) { assertTrue(limiter.tryAcquire(it * TurnRateLimiter.MIN_INTERVAL_MS)) }
        assertFalse(limiter.tryAcquire(960L))
        // Reading for a second then tapping works immediately again.
        assertTrue(limiter.tryAcquire(2_500L))
    }

    @Test
    fun `the configured rate stays conservative`() {
        // Guards against a future tweak that would outrun the 180 ms slide.
        assertTrue(
            "interval must exceed the slide duration",
            TurnRateLimiter.MIN_INTERVAL_MS >= 200L,
        )
        assertTrue(TurnRateLimiter.MAX_PER_WINDOW <= 4)
    }
}
