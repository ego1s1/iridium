package com.iridium.core.data

import com.iridium.core.fakes.TestBooksRepository
import com.iridium.core.testing.TestData
import com.iridium.core.testing.TestDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DriveSyncTest {

    @get:Rule
    val dispatcherRule = TestDispatcherRule()

    private lateinit var repository: TestBooksRepository
    private lateinit var manager: DriveSyncManager
    private lateinit var gateway: FakeGateway

    @Before
    fun setup() {
        repository = TestBooksRepository()
        gateway = FakeGateway()
        manager = DriveSyncManager(
            backup = LibraryBackupManager(repository),
            gatewayFactory = DriveGatewayFactory { gateway },
        )
    }

    @Test
    fun `decision matrix prefers the newer side`() {
        assertEquals(SyncDecision.UPLOAD, decideSync(10, null))
        assertEquals(SyncDecision.UPLOAD, decideSync(10, 5))
        assertEquals(SyncDecision.DOWNLOAD, decideSync(5, 10))
        assertEquals(SyncDecision.UP_TO_DATE, decideSync(10, 10))
    }

    @Test
    fun `blank account is signed out`() = runTest {
        assertTrue(manager.syncNow("") is DriveSyncResult.SignedOut)
    }

    @Test
    fun `missing remote uploads the local snapshot`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "1")))
        val result = manager.syncNow("user@example.com")
        assertTrue(result is DriveSyncResult.Uploaded)
        assertEquals(1, (result as DriveSyncResult.Uploaded).books)
        assertEquals(1, gateway.uploads)
    }

    @Test
    fun `newer local uploads over the remote`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "1")))
        gateway.remote = RemoteBackup("f1", createdAt = 1L)
        val result = manager.syncNow("user@example.com")
        assertTrue(result is DriveSyncResult.Uploaded)
    }

    @Test
    fun `newer remote downloads and restores`() = runTest {
        repository.setBooks(listOf(TestData.book(id = "1", title = "Local", progress = 0f)))
        val remoteJson = LibraryBackupCodec.encode(
            LibraryBackup(
                createdAt = System.currentTimeMillis() + 60_000L,
                books = listOf(
                    BackupBook(
                        sourcePath = "content://docs/1.epub",
                        title = "Local",
                        author = null,
                        progress = 0.8f,
                        lastLocator = null,
                        bookmarked = false,
                    ),
                ),
            ),
        )
        gateway.remote = RemoteBackup("f1", createdAt = System.currentTimeMillis() + 60_000L)
        gateway.payload = remoteJson
        val result = manager.syncNow("user@example.com")
        assertTrue(result is DriveSyncResult.Downloaded)
        assertEquals(0.8f, repository.observeLibrary(com.iridium.core.model.LibraryQuery()).first().first().progress, 0.001f)
    }

    private class FakeGateway : DriveGateway {
        var remote: RemoteBackup? = null
        var payload: String? = null
        var uploads = 0

        override suspend fun findBackup(): RemoteBackup? = remote
        override suspend fun download(fileId: String): String? = payload
        override suspend fun upload(filename: String, json: String, existingFileId: String?): RemoteBackup? {
            uploads++
            val saved = RemoteBackup(existingFileId ?: "new", System.currentTimeMillis())
            remote = saved
            payload = json
            return saved
        }
    }
}
