package com.iridium.core.data

import com.iridium.core.common.IoDispatcher
import com.iridium.core.common.safeCatching
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Outcome of one [DriveSyncManager.syncNow]. */
sealed interface DriveSyncResult {
    data object SignedOut : DriveSyncResult
    data object UpToDate : DriveSyncResult
    data class Uploaded(val books: Int) : DriveSyncResult
    data class Downloaded(val restored: Int, val skipped: Int) : DriveSyncResult
    data class Failed(val reason: String) : DriveSyncResult
}

/** What a sync pass should do, given local vs remote timestamps. */
enum class SyncDecision { UPLOAD, DOWNLOAD, UP_TO_DATE }

/**
 * Pure sync arbitration: the newer side wins; equal/missing timestamps are
 * ties. A missing remote always uploads; a missing local snapshot never
 * downloads over an empty library on a failed read (callers pass 0 only
 * when the snapshot genuinely holds no books).
 */
internal fun decideSync(localCreatedAt: Long, remoteCreatedAt: Long?): SyncDecision =
    when {
        remoteCreatedAt == null -> SyncDecision.UPLOAD
        localCreatedAt > remoteCreatedAt -> SyncDecision.UPLOAD
        remoteCreatedAt > localCreatedAt -> SyncDecision.DOWNLOAD
        else -> SyncDecision.UP_TO_DATE
    }

/** Remote metadata for the single backup file. */
data class RemoteBackup(val fileId: String, val createdAt: Long)

/** Thin transport seam over the backup folder (faked in tests). */
interface DriveGateway {
    suspend fun findBackup(): RemoteBackup?
    suspend fun download(fileId: String): String?
    suspend fun upload(filename: String, json: String, existingFileId: String?): RemoteBackup?
}

/**
 * Library-metadata sync over the hidden appDataFolder (`drive.appdata`
 * scope): no broad access, no user-visible files. The payload is the
 * [LibraryBackup] JSON; EPUB files never leave the device.
 */
class DriveSyncManager @Inject constructor(
    private val backup: LibraryBackupManager,
    private val gatewayFactory: DriveGatewayFactory,
    // Injected so a sync pass can be driven from a test dispatcher. Hilt
    // always supplies the qualified IO binding; the default only keeps
    // direct construction (tests, ad-hoc callers) source-compatible.
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val syncMutex = kotlinx.coroutines.sync.Mutex()
    /**
     * One manual sync for [accountEmail]: uploads when the local snapshot is
     * newer, downloads + restores when the remote wins, no-op on ties.
     */
    suspend fun syncNow(accountEmail: String): DriveSyncResult = syncMutex.withLock {
        withContext(ioDispatcher) {
        if (accountEmail.isBlank()) return@withContext DriveSyncResult.SignedOut
        // Builds without a transport (no Play services) fail here rather
        // than constructing a gateway that can never authenticate.
        if (!gatewayFactory.isAvailable) return@withContext DriveSyncResult.Failed("Drive unavailable")
        val gateway = safeCatching { gatewayFactory.create(accountEmail) }.getOrNull()
            ?: return@withContext DriveSyncResult.Failed("Drive unavailable")
        val localJson = backup.snapshot()
        val localAt = safeCatching { LibraryBackupCodec.decode(localJson).createdAt }.getOrDefault(0L)
        val remote = safeCatching { gateway.findBackup() }.getOrNull()
        return@withContext when (decideSync(localAt, remote?.createdAt)) {
            SyncDecision.UP_TO_DATE -> DriveSyncResult.UpToDate
            SyncDecision.UPLOAD -> {
                val saved = safeCatching {
                    gateway.upload(BACKUP_FILENAME, localJson, remote?.fileId)
                }.getOrNull() ?: return@withContext DriveSyncResult.Failed("Upload failed")
                val count = safeCatching { LibraryBackupCodec.decode(localJson).books.size }.getOrDefault(0)
                if (saved.createdAt <= 0) DriveSyncResult.Failed("Upload failed")
                else DriveSyncResult.Uploaded(count)
            }
            SyncDecision.DOWNLOAD -> {
                val fileId = remote?.fileId
                    ?: return@withContext DriveSyncResult.Failed("Download failed")
                val json = safeCatching { gateway.download(fileId) }.getOrNull()
                    ?: return@withContext DriveSyncResult.Failed("Download failed")
                val report = safeCatching { backup.restore(json) }.getOrNull()
                    ?: return@withContext DriveSyncResult.Failed("Invalid backup file")
                DriveSyncResult.Downloaded(report.restored, report.skipped)
            }
        }
        }
    }

    companion object {
        const val BACKUP_FILENAME = "iridium-backup.json"

        /** OAuth scope for the hidden appDataFolder (no broad Drive access). */
        const val APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}

/**
 * Builds an authenticated [DriveGateway] for a signed-in account.
 *
 * [isAvailable] is the build-level capability flag: flavors that ship no
 * transport report false so callers can skip Drive work entirely.
 */
fun interface DriveGatewayFactory {
    fun create(accountEmail: String): DriveGateway

    /** False when this build has no remote transport compiled in. */
    val isAvailable: Boolean get() = true
}
