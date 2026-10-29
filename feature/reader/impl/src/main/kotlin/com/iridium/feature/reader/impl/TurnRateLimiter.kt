package com.iridium.feature.reader.impl

/**
 * Fixed ceiling on how fast page turns may be accepted.
 *
 * A turn runs a 180 ms slide plus the navigator's own swap, so accepting taps
 * faster than that stacks animations and the transitions visibly tear. Two
 * rules apply, both flat rather than decaying so behaviour is predictable:
 *
 * - consecutive turns must be at least [minIntervalMs] apart;
 * - no more than [maxPerWindow] turns inside a rolling [windowMs].
 *
 * Extra taps are dropped, never queued: a queued turn would fire long after
 * the tap and feel like an unrelated page jump. Both limits are sized to the
 * same rate, so the window rule only matters for a finger held on the edge.
 *
 * Pure and single-threaded by contract (called from the main thread only), so
 * it is unit-testable without a clock or a device.
 */
internal class TurnRateLimiter(
    private val minIntervalMs: Long = MIN_INTERVAL_MS,
    private val windowMs: Long = WINDOW_MS,
    private val maxPerWindow: Int = MAX_PER_WINDOW,
) {

    // Nullable rather than a MIN_VALUE sentinel: subtracting from MIN_VALUE
    // overflows and would reject the very first turn.
    private var lastAcceptedMs: Long? = null
    private var windowStartMs: Long? = null
    private var acceptedInWindow = 0

    /** Records a turn at [nowMs] and reports whether it should happen. */
    fun tryAcquire(nowMs: Long): Boolean {
        // A long pause starts a fresh window, so resuming reading is instant.
        val windowStart = windowStartMs
        if (windowStart == null || nowMs - windowStart >= windowMs) {
            windowStartMs = nowMs
            acceptedInWindow = 0
        }
        if (acceptedInWindow >= maxPerWindow) return false
        val lastAccepted = lastAcceptedMs
        if (lastAccepted != null && nowMs - lastAccepted < minIntervalMs) return false
        lastAcceptedMs = nowMs
        acceptedInWindow += 1
        return true
    }

    internal companion object {
        /** One turn per 240 ms: about four per second, faster than anyone reads. */
        const val MIN_INTERVAL_MS = 240L

        /** Rolling window for the hard cap. */
        const val WINDOW_MS = 1_000L

        /** Four per second, matching [MIN_INTERVAL_MS]. */
        const val MAX_PER_WINDOW = 4
    }
}
