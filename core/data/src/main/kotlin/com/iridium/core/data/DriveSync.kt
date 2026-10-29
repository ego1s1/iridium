package com.iridium.core.data

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import dagger.hilt.android.qualifiers.ApplicationContext
import com.iridium.core.common.IoDispatcher
import com.iridium.core.common.safeCatching
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

/** Thin transport seam over the Drive appDataFolder (faked in tests). */
interface DriveGateway {
    suspend fun findBackup(): RemoteBackup?
    suspend fun download(fileId: String): String?
    suspend fun upload(filename: String, json: String, existingFileId: String?): RemoteBackup?
}

/**
 * Google Drive sync for library metadata (Phase 3.3). Operates only on the
 * hidden appDataFolder (`drive.appdata` scope): no broad Drive access, no
 * user-visible files. The payload is the [LibraryBackup] JSON; EPUB files
 * never leave the device.
 */
class DriveSyncManager @Inject constructor(
    private val backup: LibraryBackupManager,
    private val gatewayFactory: DriveGatewayFactory,
    // Injected so a sync pass can be driven from a test dispatcher. Hilt
    // always supplies the qualified IO binding; the default only keeps
    // direct construction (tests, ad-hoc callers) source-compatible.
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val syncMutex = kotlinx.coroutines.sync.Mutex()
    /**
     * One manual sync for [accountEmail]: uploads when the local snapshot is
     * newer, downloads + restores when the remote wins, no-op on ties.
     */
    suspend fun syncNow(accountEmail: String): DriveSyncResult = syncMutex.withLock {
        withContext(ioDispatcher) {
        if (accountEmail.isBlank()) return@withContext DriveSyncResult.SignedOut
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

/** Builds an authenticated [DriveGateway] for a signed-in account. */
fun interface DriveGatewayFactory {
    fun create(accountEmail: String): DriveGateway
}

class GoogleDriveGatewayFactory @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DriveGatewayFactory {
    override fun create(accountEmail: String): DriveGateway {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA),
        ).apply { selectedAccountName = accountEmail }
        val drive = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
            .setApplicationName("Iridium")
            .build()
        return GoogleDriveGateway(drive, ioDispatcher)
    }
}

internal class GoogleDriveGateway(
    private val drive: Drive,
    private val ioDispatcher: CoroutineDispatcher,
) : DriveGateway {
    override suspend fun findBackup(): RemoteBackup? = withContext(ioDispatcher) {
        val list = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '${DriveSyncManager.BACKUP_FILENAME}' and trashed = false")
            .setFields("files(id, modifiedTime, appProperties)")
            .execute()
        val file = list.files.firstOrNull() ?: return@withContext null
        RemoteBackup(
            fileId = file.id,
            createdAt = file.appProperties?.get("createdAt")?.toLongOrNull()
                ?: file.modifiedTime?.value ?: 0L,
        )
    }

    override suspend fun download(fileId: String): String? = withContext(ioDispatcher) {
        val out = ByteArrayOutputStream()
        drive.files().get(fileId).executeMediaAndDownloadTo(out)
        out.toString(Charsets.UTF_8.name())
    }

    override suspend fun upload(filename: String, json: String, existingFileId: String?): RemoteBackup? =
        withContext(ioDispatcher) {
            val createdAt = System.currentTimeMillis().toString()
            if (existingFileId != null) {
                val meta = File().setAppProperties(mapOf("createdAt" to createdAt))
                val content = ByteArrayContent.fromString("application/json", json)
                val updated = drive.files().update(existingFileId, meta, content).execute()
                RemoteBackup(updated.id, createdAt.toLong())
            } else {
                val meta = File()
                    .setName(filename)
                    .setParents(listOf("appDataFolder"))
                    .setAppProperties(mapOf("createdAt" to createdAt))
                val content = ByteArrayContent.fromString("application/json", json)
                val created = drive.files().create(meta, content).setFields("id").execute()
                RemoteBackup(created.id, createdAt.toLong())
            }
        }
}
