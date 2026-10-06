package com.iridium.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class IridiumSliderTest {

    @Test
    fun computeScrubBucketQuantizesSmoothly() {
        val range = 0f..100f
        assertEquals(0, computeScrubBucket(0f, range))
        assertEquals(1, computeScrubBucket(5f, range))
        assertEquals(2, computeScrubBucket(10f, range))
        assertEquals(10, computeScrubBucket(50f, range))
        assertEquals(19, computeScrubBucket(99f, range))
        assertEquals(20, computeScrubBucket(100f, range))
    }

    @Test
    fun computeScrubBucketHandlesZeroSpan() {
        val range = 10f..10f
        assertEquals(0, computeScrubBucket(10f, range))
    }

    @Test
    fun computeScrubBucketHandlesCustomSegments() {
        val range = 0f..1f
        assertEquals(0, computeScrubBucket(0f, range, segments = 10))
        assertEquals(5, computeScrubBucket(0.5f, range, segments = 10))
        assertEquals(10, computeScrubBucket(1.0f, range, segments = 10))
    }
}
