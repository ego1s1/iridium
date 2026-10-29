package com.iridium.app

import android.content.Intent
import android.net.Uri
import com.iridium.core.fakes.TestBooksRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImportDeepLinkHandlerTest {

    private val repository = TestBooksRepository()
    private fun handler() = ImportDeepLinkHandler(
        context = org.robolectric.RuntimeEnvironment.getApplication(),
        repository = repository,
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `VIEW epub intent parses`() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                Uri.parse("content://com.example/book.epub"),
                "application/epub+zip",
            )
        }
        val request = handler().parseIntent(intent)
        assertEquals("content://com.example/book.epub", request?.uri.toString())
    }

    @Test
    fun `VIEW without epub mime falls back to extension`() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://com.example/book.epub")
        }
        assertEquals("content://com.example/book.epub", handler().parseIntent(intent)?.uri.toString())
    }

    @Test
    fun `VIEW of plain text is declined`() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                Uri.parse("content://com.example/notes.txt"),
                "text/plain",
            )
        }
        assertNull(handler().parseIntent(intent))
    }

    @Test
    fun `SEND epub stream parses`() {
        val uri = Uri.parse("content://com.example/shared.epub")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/epub+zip"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        assertEquals(uri, handler().parseIntent(intent)?.uri)
    }

    @Test
    fun `SEND text is declined`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "hello")
        }
        assertNull(handler().parseIntent(intent))
    }

    @Test
    fun `MAIN launcher intent is declined`() {
        assertNull(handler().parseIntent(Intent(Intent.ACTION_MAIN)))
    }

    @Test
    fun `importUri stores the book and returns its id`() = runTest {
        val bytes = "fake-epub-bytes".toByteArray()
        val id = handler().importUri(
            "shared.epub",
        ) { bytes.inputStream() }
        assertEquals("imported-1", id)
        assertEquals(1, repository.imports.size)
        assertEquals("shared.epub", repository.imports.first().second)
    }

    @Test
    fun `importUri null stream returns null`() = runTest {
        assertNull(
            handler().importUri("x.epub") { null },
        )
    }
}
