package com.iridium.app

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Opt-in crash capture, no third party and no background upload.
 *
 * Nothing is recorded until the user explicitly enables it. When enabled, an
 * uncaught exception is written as a plain-text tombstone in the cache and the
 * user is offered a share sheet on the next launch — the report leaves the
 * device only if they choose to send it, and the cache is theirs to clear.
 */
@Singleton
class CrashReporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val directory: File get() = File(context.cacheDir, DIR).apply { mkdirs() }

    private val previousHandler = AtomicReference<Thread.UncaughtExceptionHandler?>()

    /**
     * Explicit installed flag rather than a null sentinel: the platform's
     * default handler is itself null on a fresh process, so "previous != null"
     * would wrongly report "not installed" and wrap the handler twice.
     */
    private val installed = java.util.concurrent.atomic.AtomicBoolean(false)

    /** Chains a handler that records the crash, then defers to the platform. */
    fun install() {
        if (!installed.compareAndSet(false, true)) return
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        previousHandler.set(previous)
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeTombstone(thread, throwable) }
            // Never swallow the crash: the system must still see it.
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun uninstall() {
        if (!installed.compareAndSet(true, false)) return
        Thread.setDefaultUncaughtExceptionHandler(previousHandler.getAndSet(null))
    }

    /** Tombstones waiting to be offered to the user, newest first. */
    suspend fun pending(): List<File> = withContext(Dispatchers.IO) {
        directory.listFiles()?.filter { it.isFile && it.name.endsWith(EXT) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
    }

    suspend fun read(file: File): String? = withContext(Dispatchers.IO) {
        runCatching { file.readText() }.getOrNull()
    }

    suspend fun clear(file: File) = withContext(Dispatchers.IO) {
        runCatching { file.delete() }
        Unit
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        directory.listFiles()?.forEach { runCatching { it.delete() } }
        Unit
    }

    private fun writeTombstone(thread: Thread, throwable: Throwable) {
        val stackTrace = StringWriter().also { writer ->
            PrintWriter(writer).use { throwable.printStackTrace(it) }
        }.toString()

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val fileName = "crash-${System.currentTimeMillis()}$EXT"
        val body = buildString {
            appendLine("time: $timestamp")
            appendLine("thread: ${thread.name}")
            appendLine("app: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.SDK_INT}")
            appendLine("---")
            appendLine(stackTrace)
        }
        runCatching { File(directory, fileName).writeText(body) }
    }

    private companion object {
        const val DIR = "crashes"
        const val EXT = ".txt"
    }
}
