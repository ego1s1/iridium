package com.iridium.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FtsQueryTest {

    @Test
    fun `builds a prefix expression per term`() {
        assertEquals("hispaniola*", FtsQuery.build("Hispaniola"))
        assertEquals("the* sea*", FtsQuery.build("the sea"))
    }

    @Test
    fun `strips characters that have meaning to fts`() {
        // Quotes, operators and punctuation must never reach MATCH verbatim.
        assertEquals("and*", FtsQuery.build("AND"))
        assertEquals("foo*", FtsQuery.build("\"foo\""))
        assertEquals("ab*", FtsQuery.build("a*()b"))
        assertEquals("or*", FtsQuery.build("OR"))
    }

    @Test
    fun `returns null when nothing searchable remains`() {
        assertNull(FtsQuery.build(""))
        assertNull(FtsQuery.build("   "))
        assertNull(FtsQuery.build("***"))
        assertNull(FtsQuery.build("..."))
    }

    @Test
    fun `snippet centres on the match with ellipses`() {
        val body = "The Hispaniola was rolling scuppers under in the ocean swell."
        val snippet = FtsQuery.snippet(body, "scuppers", radius = 10)
        assertTrue(snippet.contains("scuppers"))
        assertTrue(snippet.startsWith("…"))
        assertTrue(snippet.endsWith("…"))
    }

    @Test
    fun `snippet falls back to the head when there is no match`() {
        val body = "Nothing relevant in this chapter body at all."
        val snippet = FtsQuery.snippet(body, "zzz", radius = 20)
        assertTrue(snippet.isNotEmpty())
        assertTrue(body.startsWith(snippet.take(10)))
    }

    @Test
    fun `snippet is case insensitive`() {
        val body = "The HISPANIOLA was rolling."
        assertTrue(FtsQuery.snippet(body, "hispaniola", radius = 5).contains("HISPANIOLA"))
    }
}
