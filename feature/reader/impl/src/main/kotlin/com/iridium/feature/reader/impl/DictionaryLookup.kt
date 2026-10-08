package com.iridium.feature.reader.impl

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * A single dictionary meaning: a part of speech with short definitions.
 */
data class WordMeaning(
    val partOfSpeech: String,
    val definitions: List<String>,
)

/**
 * A looked-up word with phonetic spelling and meanings.
 */
data class WordDefinition(
    val word: String,
    val phonetic: String? = null,
    val meanings: List<WordMeaning> = emptyList(),
    val sourceUrl: String? = null,
)

/** Thrown when no dictionary entry exists for the requested word. */
class WordNotFoundException(val word: String) : Exception("No definition found for \"$word\"")

/** Thrown when the lookup fails for connectivity or server reasons. */
class DictionaryNetworkException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** Thrown when a 2xx response cannot be understood. */
class DictionaryParseException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Lookup contract for the reader's word popup. Results are [Result]-wrapped:
 * [WordNotFoundException] for unknown words, [DictionaryNetworkException]
 * for offline/server failures, [DictionaryParseException] for bad payloads.
 */
interface DictionaryLookup {
    suspend fun define(word: String): Result<WordDefinition>
}

/** Minimal HTTP surface the lookup needs; faked in tests, no mocking libs. */
fun interface DictionaryHttpClient {
    @Throws(IOException::class)
    fun get(url: String): HttpResult
}

/** Status code plus body of a GET response. */
data class HttpResult(
    val statusCode: Int,
    val body: String,
)

/** [DictionaryHttpClient] over [HttpURLConnection]; no new dependencies. */
class UrlConnectionDictionaryHttpClient(
    private val timeoutMs: Int = HTTP_TIMEOUT_MS,
) : DictionaryHttpClient {
    override fun get(url: String): HttpResult {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = timeoutMs
            connection.readTimeout = timeoutMs
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            val stream = if (code in HTTP_OK_MIN..HTTP_OK_MAX) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.bufferedReader()?.readText().orEmpty()
            return HttpResult(code, body)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val HTTP_TIMEOUT_MS = 8_000
        const val HTTP_OK_MIN = 200
        const val HTTP_OK_MAX = 299
    }
}

/**
 * Free Wiktionary lookup (MediaWiki REST API, no key required) with a
 * bounded LRU cache. The cache map is caller-owned (e.g. a session-scoped
 * map) so eviction policy stays with the caller; the default cap keeps
 * repeat lookups instant without unbounded growth.
 */
class HttpDictionaryLookup(
    private val httpClient: DictionaryHttpClient = UrlConnectionDictionaryHttpClient(),
    private val cache: MutableMap<String, WordDefinition> = synchronizedLruCache(),
) : DictionaryLookup {

    /** Serializes check-then-act cache access across concurrent lookups. */
    private val cacheMutex = Mutex()

    override suspend fun define(word: String): Result<WordDefinition> {
        val query = normalizeLookupWord(word)
            ?: return Result.failure(WordNotFoundException(word))
        cacheMutex.withLock { cache[query] }?.let { return Result.success(it) }
        return try {
            val response = withContext(Dispatchers.IO) {
                val encoded = URLEncoder.encode(query, Charsets.UTF_8.name())
                httpClient.get("$DICTIONARY_ENDPOINT/$encoded")
            }
            val definition = when {
                response.statusCode == HTTP_NOT_FOUND -> throw WordNotFoundException(query)
                response.statusCode !in HTTP_OK_MIN..HTTP_OK_MAX ->
                    throw DictionaryNetworkException("Dictionary lookup failed (HTTP ${response.statusCode})")
                else -> parseWordDefinition(response.body, query) ?: throw WordNotFoundException(query)
            }
            cacheMutex.withLock { cache[query] = definition }
            Result.success(definition)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: WordNotFoundException) {
            Result.failure(e)
        } catch (e: DictionaryNetworkException) {
            Result.failure(e)
        } catch (e: DictionaryParseException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(DictionaryNetworkException("You're offline — couldn't look up \"$query\"", e))
        } catch (e: Exception) {
            // org.json throws JSONException (a RuntimeException) on bad payloads.
            Result.failure(DictionaryParseException("Couldn't understand the dictionary response", e))
        }
    }

    private companion object {
        const val DICTIONARY_ENDPOINT = "https://en.wiktionary.org/api/rest_v1/page/definition"
        const val HTTP_NOT_FOUND = 404
        const val HTTP_OK_MIN = 200
        const val HTTP_OK_MAX = 299
    }
}

/**
 * Reduces raw selection text to a single query word: first whitespace token,
 * trimmed of surrounding punctuation, lowercased. Null when nothing usable
 * remains (blank or punctuation-only selections).
 */
internal fun normalizeLookupWord(raw: String): String? {
    val token = raw.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
    val cleaned = token.trim { !it.isLetterOrDigit() && it != '\'' && it != '-' }
        .trim('\'', '-')
        .lowercase()
    return cleaned.ifBlank { null }
}

/**
 * Parses one Wiktionary definition payload (`{"en": [{partOfSpeech,
 * definitions: [{definition: html}]}]}`) into the first usable
 * [WordDefinition]. Definition HTML is stripped to plain text. Returns null
 * when the payload holds no usable definition; throws on malformed JSON.
 */
internal fun parseWordDefinition(json: String, query: String): WordDefinition? {
    val root = JSONObject(json)
    val entries = root.optJSONArray("en") ?: return null
    val meanings = parseMeanings(entries)
    if (meanings.isEmpty()) return null
    return WordDefinition(
        word = query,
        phonetic = null,
        meanings = meanings,
        sourceUrl = "https://en.wiktionary.org/wiki/$query",
    )
}

private fun parseMeanings(entries: JSONArray): List<WordMeaning> {
    val out = mutableListOf<WordMeaning>()
    for (i in 0 until entries.length()) {
        if (out.size >= MAX_MEANINGS) break
        val meaning = entries.optJSONObject(i) ?: continue
        val definitions = meaning.optJSONArray("definitions") ?: continue
        val texts = (0 until definitions.length()).asSequence()
            .map { stripDefinitionHtml(definitions.optJSONObject(it)?.optString("definition").orEmpty()) }
            .filter { it.isNotBlank() }
            .take(MAX_DEFINITIONS)
            .toList()
        if (texts.isEmpty()) continue
        out.add(
            WordMeaning(
                partOfSpeech = meaning.optString("partOfSpeech").ifBlank { PART_OF_SPEECH_FALLBACK },
                definitions = texts,
            ),
        )
    }
    return out
}

/** Strips definition HTML (`<a>`, `<b>` links/emphasis) to plain text. */
internal fun stripDefinitionHtml(html: String): String {
    var text = html.replace(Regex("<[^>]*>"), "")
    text = text
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
    return text.replace(Regex("\\s+"), " ").trim()
}

private const val MAX_MEANINGS = 3
private const val MAX_DEFINITIONS = 2
private const val PART_OF_SPEECH_FALLBACK = "unknown"

/** Default cap for [HttpDictionaryLookup]'s session cache (words). */
internal const val DICTIONARY_CACHE_MAX = 100

/**
 * Access-ordered, thread-safe LRU map: repeat lookups inside a session hit
 * memory instead of the network, and the eldest entry drops past [maxSize].
 */
internal fun synchronizedLruCache(
    maxSize: Int = DICTIONARY_CACHE_MAX,
): MutableMap<String, WordDefinition> =
    java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, WordDefinition>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, WordDefinition>?,
            ): Boolean = size > maxSize
        },
    )
