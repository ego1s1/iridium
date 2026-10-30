package com.iridium.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold-start harness. Compares the two compilation modes that matter for a
 * release: the baseline profile that ships inside the APK versus no AOT/JIT
 * profile at all, so a startup change and a profile change can be told apart.
 *
 * The measured end is the app's own [android.app.Activity.reportFullyDrawn],
 * fired when the onboarding gate resolves and the shell is composed — not the
 * first frame, which is only the loading surface.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private val device
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun coldStartupWithBaselineProfile() =
        coldStartup(CompilationMode.Partial(BaselineProfileMode.Require))

    @Test
    fun coldStartupNoCompilation() = coldStartup(CompilationMode.None())

    private fun coldStartup(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = BuildConfig.TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        iterations = 10,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        device.wait(Until.hasObject(By.pkg(BuildConfig.TARGET_PACKAGE).depth(0)), 5_000)
    }
}
