package com.iridium.feature.reader.impl

/**
 * Scripted dictionary for ViewModel tests: answers from [definitions],
 * records queries, never touches the network.
 */
class FakeDictionaryLookup(
    private val definitions: Map<String, WordDefinition> = emptyMap(),
) : DictionaryLookup {

    val queries = mutableListOf<String>()

    override suspend fun define(word: String): Result<WordDefinition> {
        val query = normalizeLookupWord(word) ?: return Result.failure(WordNotFoundException(word))
        queries += query
        return definitions[query]?.let { Result.success(it) }
            ?: Result.failure(WordNotFoundException(query))
    }
}
