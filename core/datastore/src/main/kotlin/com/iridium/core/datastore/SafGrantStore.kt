package com.iridium.core.datastore

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import com.iridium.core.common.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Persisted Storage Access Framework grants. The picker takes them;
 * this store answers which linked folders are still readable and releases
 * dead ones. Interface seam so ViewModel tests fake the resolver.
 */
interface SafGrantStore {
    /** URIs with a live persisted read grant. */
    suspend fun persistedReadUris(): Set<String>

    /** Releases the persisted read grant; false when there was none. */
    suspend fun releaseReadUri(uri: String): Boolean
}

@Singleton
internal class ContentResolverGrantStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SafGrantStore {

    override suspend fun persistedReadUris(): Set<String> = withContext(ioDispatcher) {
        context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .mapTo(mutableSetOf()) { it.uri.toString() }
    }

    override suspend fun releaseReadUri(uri: String): Boolean = withContext(ioDispatcher) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            true
        }.getOrDefault(false)
    }
}
