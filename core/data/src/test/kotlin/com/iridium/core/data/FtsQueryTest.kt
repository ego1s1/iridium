package com.iridium.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

}
