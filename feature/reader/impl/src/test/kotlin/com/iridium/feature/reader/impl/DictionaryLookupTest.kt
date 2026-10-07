package com.iridium.feature.reader.impl

import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Lookup parsing is verified against inline dictionaryapi.dev fixtures —
 * no network. The HTTP layer is a [DictionaryHttpClient] test double.
 */
@RunWith(RobolectricTestRunner::class)
class DictionaryLookupTest {

    private class FakeHttpClient(
        var result: HttpResult = HttpResult(200, "[]"),
        var failure: IOException? = null,
    ) : DictionaryHttpClient {
        val requestedUrls = mutableListOf<String>()

        override fun get(url: String): HttpResult {
            requestedUrls.add(url)
            failure?.let { throw it }
            return result
        }
    }

    @Test
    fun `parses word and meanings`() {
        val definition = parseWordDefinition(SINGLE_ENTRY_JSON, "hello")

        checkNotNull(definition)
        assertEquals("hello", definition.word)
        assertNull(definition.phonetic)
        assertEquals(2, definition.meanings.size)
        assertEquals("interjection", definition.meanings[0].partOfSpeech)
        assertEquals(
            listOf("Used as a greeting.", "An expression of surprise."),
            definition.meanings[0].definitions,
        )
        assertEquals("verb", definition.meanings[1].partOfSpeech)
        assertEquals("https://en.wiktionary.org/wiki/hello", definition.sourceUrl)
    }

    @Test
    fun `strips html from definitions`() {
        val definition = parseWordDefinition(HTML_DEFINITION_JSON, "any")

        checkNotNull(definition)
        assertEquals("noun", definition.meanings.single().partOfSpeech)
        assertEquals("At any time that.", definition.meanings.single().definitions.single())
    }

    @Test
    fun `caps meanings at three with two definitions each`() {
        val definition = parseWordDefinition(MANY_MEANINGS_JSON, "run")

        checkNotNull(definition)
        assertEquals(3, definition.meanings.size)
        definition.meanings.forEach { assertEquals(2, it.definitions.size) }
    }

    @Test
    fun `empty entry array parses to null`() {
        assertNull(parseWordDefinition("{}", "hello"))
        assertNull(parseWordDefinition("{\"en\":[]}", "hello"))
    }

    @Test
    fun `define caches by normalized word`() = runTest {
        val client = FakeHttpClient(HttpResult(200, SINGLE_ENTRY_JSON))
        val lookup = HttpDictionaryLookup(client, mutableMapOf())

        val first = lookup.define(" Hello, ").getOrThrow()
        val second = lookup.define("hello").getOrThrow()

        assertEquals(first, second)
        assertEquals(1, client.requestedUrls.size)
        assertTrue(client.requestedUrls.single().endsWith("/hello"))
    }

    @Test
    fun `define maps 404 to WordNotFound`() = runTest {
        val lookup = HttpDictionaryLookup(FakeHttpClient(HttpResult(404, NOT_FOUND_JSON)))

        val result = lookup.define("zxqwvx")

        val error = result.exceptionOrNull()
        assertTrue(error is WordNotFoundException)
        assertEquals("zxqwvx", (error as WordNotFoundException).word)
    }

    @Test
    fun `define maps IOException to network error`() = runTest {
        val lookup = HttpDictionaryLookup(FakeHttpClient(failure = IOException("unreachable")))

        val result = lookup.define("hello")

        assertTrue(result.exceptionOrNull() is DictionaryNetworkException)
    }

    @Test
    fun `define maps server errors to network error`() = runTest {
        val lookup = HttpDictionaryLookup(FakeHttpClient(HttpResult(500, "boom")))

        val result = lookup.define("hello")

        assertTrue(result.exceptionOrNull() is DictionaryNetworkException)
    }

    @Test
    fun `define maps malformed payload to parse error`() = runTest {
        val lookup = HttpDictionaryLookup(FakeHttpClient(HttpResult(200, "not json")))

        val result = lookup.define("hello")

        assertTrue(result.exceptionOrNull() is DictionaryParseException)
    }

    @Test
    fun `blank selections fail without a request`() = runTest {
        val client = FakeHttpClient(HttpResult(200, SINGLE_ENTRY_JSON))
        val lookup = HttpDictionaryLookup(client)

        val result = lookup.define("   ")

        assertTrue(result.exceptionOrNull() is WordNotFoundException)
        assertTrue(client.requestedUrls.isEmpty())
    }

    private companion object {
        const val SINGLE_ENTRY_JSON = """{
          "en": [
            {
              "partOfSpeech": "interjection",
              "definitions": [
                {"definition": "Used as a greeting."},
                {"definition": "An expression of surprise."},
                {"definition": "A third sense that must be trimmed."}
              ]
            },
            {
              "partOfSpeech": "verb",
              "definitions": [{"definition": "To greet."}]
            }
          ]
        }"""

        const val HTML_DEFINITION_JSON = """{
          "en": [
            {"partOfSpeech": "noun", "definitions": [
              {"definition": "At <a rel=\"mw:WikiLink\" href=\"/wiki/any\" title=\"any\">any</a> time that."}
            ]}
          ]
        }"""

        const val MANY_MEANINGS_JSON = """{
          "en": [
            {"partOfSpeech": "verb", "definitions": [
              {"definition": "one"}, {"definition": "two"}, {"definition": "three"}]},
            {"partOfSpeech": "noun", "definitions": [
              {"definition": "one"}, {"definition": "two"}, {"definition": "three"}]},
            {"partOfSpeech": "adjective", "definitions": [
              {"definition": "one"}, {"definition": "two"}]},
            {"partOfSpeech": "extra", "definitions": [{"definition": "trimmed"}]}
          ]
        }"""

        const val NOT_FOUND_JSON =
            """{"type": "not-found", "title": "Not found."}"""
    }
}
