package com.iridium.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Library scroll harness: frame timing for a fling over the book grid. Needs a
 * library with enough books to scroll; on an empty install the test is skipped
 * instead of measuring nothing.
 */
@RunWith(AndroidJUnit4::class)
class LibraryScrollBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private val device
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun libraryFling() = rule.measureRepeated(
        packageName = BuildConfig.TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            startActivityAndWait()
            device.wait(Until.findObject(By.scrollable(true)), 10_000)
            device.waitForIdle()
        },
    ) {
        val grid = device.findObject(By.scrollable(true))
        assumeTrue("no scrollable grid: the library needs books", grid != null)
        grid!!
        grid.setGestureMargin(device.displayWidth / 5)
        repeat(3) {
            grid.fling(Direction.DOWN)
            device.waitForIdle()
        }
        grid.fling(Direction.UP)
        device.waitForIdle()
        device.wait(Until.findObject(By.scrollable(true)), 2_000)
    }
}
