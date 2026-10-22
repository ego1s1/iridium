package com.iridium.core.data

import org.json.JSONObject
import org.readium.r2.shared.publication.Locator

/**
 * The single String↔Locator boundary. Locators persist as JSON strings
 * (progress columns, highlight anchors); every other layer passes [Locator]
 * or opaque strings and never touches `org.json` directly.
 */
object LocatorStrings {

    /** Serializes a locator for storage. Never throws. */
    fun encode(locator: Locator): String = locator.toJSON().toString()

    /** Parses a stored locator; null when missing or corrupt. Never throws. */
    fun decode(raw: String?): Locator? =
        raw?.let { runCatching { Locator.fromJSON(JSONObject(it)) }.getOrNull() }
}
