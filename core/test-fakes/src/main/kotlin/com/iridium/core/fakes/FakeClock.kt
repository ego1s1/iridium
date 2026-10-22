package com.iridium.core.fakes

import com.iridium.core.common.AppClock

/** Controllable clock: tests set or advance time instead of sleeping. */
class FakeClock(
    var wall: Long = 1_700_000_000_000L,
    var uptime: Long = 0L,
) : AppClock {

    override fun wallNow(): Long = wall

    override fun uptimeNow(): Long = uptime

    fun advanceUptimeBy(millis: Long) {
        uptime += millis
    }
}
