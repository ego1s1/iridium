package com.iridium.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry

/** Measures from library ViewModel creation until the first database-backed frame. */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class LibraryReadyBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private val device
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun firstLibraryFrame() = rule.measureRepeated(
        packageName = BuildConfig.TARGET_PACKAGE,
        metrics = listOf(TraceSectionMetric("iri.library.ready")),
        compilationMode = CompilationMode.Partial(),
        iterations = 10,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        device.waitForIdle()
    }
}
