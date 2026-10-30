package com.iridium.core.data

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import com.iridium.core.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Drive transport for builds that may ship Play services. Uses the user's
 * persisted account credential, so sign-in intent scopes must include
 * [DriveSyncManager.APPDATA_SCOPE].
 */
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
