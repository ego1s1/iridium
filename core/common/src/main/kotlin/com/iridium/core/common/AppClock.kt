package com.iridium.core.common

/**
 * Testable clock. Production time comes from [SystemAppClock] (bound in
 * `core:data`); tests supply fakes with fixed or advancing time instead of
 * mocking the system clock.
 */
interface AppClock {
    /** Wall-clock millis, e.g. for `createdAt`/`updatedAt` columns. */
    fun wallNow(): Long

    /** Monotonic uptime millis, e.g. for tap-timing grace windows. */
    fun uptimeNow(): Long
}
