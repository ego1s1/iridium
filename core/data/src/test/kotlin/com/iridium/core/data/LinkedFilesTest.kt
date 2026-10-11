package com.iridium.core.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkedFilesTest {

    @Test
    fun `sanitize keeps a normal name`() {
        assertEquals("my-book.epub", sanitizeImportDisplayName("my-book.epub"))
    }

    @Test
    fun `sanitize appends epub and strips separators`() {
        assertEquals("a-b-c.epub", sanitizeImportDisplayName("a/b\\c"))
        assertEquals("book.epub", sanitizeImportDisplayName("   "))
        assertEquals("book.epub", sanitizeImportDisplayName(null))
    }

    @Test
    fun `copyCapped copies small streams`() {
        val out = ByteArrayOutputStream()
        copyCapped(ByteArrayInputStream(ByteArray(100) { 7 }), out, 1024)
        assertEquals(100, out.size())
    }

    @Test
    fun `copyCapped rejects past the cap`() {
        val out = ByteArrayOutputStream()
        val failed = runCatching {
            copyCapped(ByteArrayInputStream(ByteArray(100) { 7 }), out, 10)
        }.isFailure
        assertTrue(failed)
    }
}
