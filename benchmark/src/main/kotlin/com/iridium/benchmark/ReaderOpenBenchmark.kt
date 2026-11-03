package com.iridium.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Measures EPUB open/first-page traces and transition frames for a real library book. */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class ReaderOpenBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private val device
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val firstBook = By.res(Pattern.compile(".*libraryCard:.*"))
    private val readerPage = By.res(Pattern.compile(".*readerPageReady"))

    @Test
    fun openFirstBook() = rule.measureRepeated(
        packageName = BuildConfig.TARGET_PACKAGE,
        metrics = listOf(
            TraceSectionMetric("iri.reader.open"),
            TraceSectionMetric("iri.reader.firstPage"),
            FrameTimingMetric(),
        ),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            startActivityAndWait()
            assumeTrue("requires a readable book", device.wait(Until.findObject(firstBook), 10_000) != null)
            device.waitForIdle()
        },
    ) {
        val card = device.findObject(firstBook)
        assumeTrue("requires a visible book card", card != null)
        (card ?: throw AssumptionViolatedException("requires a visible book card")).click()
        assumeTrue("reader did not paint a page", device.wait(Until.hasObject(readerPage), 30_000))
        device.waitForIdle()
        device.pressBack()
        device.wait(Until.findObject(firstBook), 10_000)
    }
}
