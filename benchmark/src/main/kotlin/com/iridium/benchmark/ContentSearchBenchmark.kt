package com.iridium.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
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

/** Measures indexed full-text search; skips when the library has no searchable chapters. */
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class ContentSearchBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private val device
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private val searchField = By.res(Pattern.compile(".*libraryChromeSearchField"))
    private val hitsHeader = By.res(Pattern.compile(".*libraryContentHitsHeader"))

    @Test
    fun searchIndexedContent() = rule.measureRepeated(
        packageName = BuildConfig.TARGET_PACKAGE,
        metrics = listOf(TraceSectionMetric("iri.search.content")),
        compilationMode = CompilationMode.Partial(),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            startActivityAndWait()
            val field = device.wait(Until.findObject(searchField), 10_000)
            assumeTrue("library search field is unavailable", field != null)
            val search = field ?: throw AssumptionViolatedException("library search field is unavailable")
            search.click()
            search.setText(SEARCH_TERM)
            device.pressEnter()
            assumeTrue("requires indexed content matching the benchmark query", device.wait(Until.hasObject(hitsHeader), 10_000))
            search.setText("")
            device.wait(Until.gone(hitsHeader), 10_000)
            device.waitForIdle()
        },
    ) {
        val field = device.findObject(searchField)
        assumeTrue("library search field is unavailable", field != null)
        val search = field ?: throw AssumptionViolatedException("library search field is unavailable")
        search.click()
        search.setText(SEARCH_TERM)
        device.pressEnter()
        assumeTrue("content search did not produce hits", device.wait(Until.hasObject(hitsHeader), 10_000))
        device.waitForIdle()
    }

    private companion object {
        const val SEARCH_TERM = "hispaniola"
    }
}
