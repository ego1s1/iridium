package com.iridium.core.fakes

import com.iridium.core.datastore.SafGrantStore

/** Fake SAF grants: tests control exactly which linked folders stay readable. */
class FakeSafGrantStore(
    var persisted: MutableSet<String> = mutableSetOf(),
) : SafGrantStore {

    val released = mutableListOf<String>()

    override suspend fun persistedReadUris(): Set<String> = persisted.toSet()

    override suspend fun releaseReadUri(uri: String): Boolean {
        released += uri
        return persisted.remove(uri)
    }
}
