package com.iridium.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashReporterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val reporter = CrashReporter(context)
    private val originalHandler = Thread.getDefaultUncaughtExceptionHandler()

    @After
    fun tearDown() {
        reporter.uninstall()
        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
    }

    @Test
    fun `nothing is captured before install`() = runTest {
        assertTrue(reporter.pending().isEmpty())
    }

    @Test
    fun `install records a crash and still defers to the platform handler`() = runTest {
        reporter.clearAll()
        val platformSawCrash = AtomicBoolean(false)
        // Stand in for the platform's handler so we can prove delegation.
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> platformSawCrash.set(true) }

        reporter.install()
        val installed = Thread.getDefaultUncaughtExceptionHandler()
        assertNotNull(installed)
        installed!!.uncaughtException(Thread.currentThread(), RuntimeException("boom"))

        assertTrue("the platform handler must still run", platformSawCrash.get())
        val reports = reporter.pending()
        assertTrue("a tombstone should have been written", reports.isNotEmpty())

        val text = reporter.read(reports.first())
        assertNotNull(text)
        assertTrue(text!!.contains("RuntimeException"))
        assertTrue(text.contains("boom"))
    }

    @Test
    fun `uninstall restores the previous handler`() {
        val sentinel: Thread.UncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, _ -> }
        Thread.setDefaultUncaughtExceptionHandler(sentinel)

        reporter.install()
        reporter.uninstall()

        assertTrue(Thread.getDefaultUncaughtExceptionHandler() === sentinel)
    }

    @Test
    fun `reports can be cleared`() = runTest {
        reporter.install()
        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), IllegalStateException("nope"))
        assertTrue(reporter.pending().isNotEmpty())

        reporter.clearAll()
        assertTrue(reporter.pending().isEmpty())
    }

    @Test
    fun `installing twice does not double-wrap`() {
        reporter.install()
        val afterFirst = Thread.getDefaultUncaughtExceptionHandler()
        reporter.install()
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() === afterFirst)
        reporter.uninstall()

        // A single uninstall must fully restore, which proves there was one wrap.
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() === afterFirst)
    }
}
