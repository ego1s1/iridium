package com.iridium.core.data

import com.iridium.core.fakes.TestBooksRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The `foss` channel compiles Drive out entirely; these assertions pin the
 * capability flags the settings UI relies on to hide its Drive rows.
 */
class FossDriveTest {

    @Test
    fun `no-op factory reports itself unavailable`() {
        assertFalse(UnavailableDriveGatewayFactory().isAvailable)
    }

    @Test
    fun `no-op sign-in reports itself unsupported`() {
        assertFalse(UnavailableDriveSignIn().isSupported)
    }

    @Test
    fun `sync fails instead of building a gateway`() = runTest {
        val manager = DriveSyncManager(
            backup = LibraryBackupManager(TestBooksRepository()),
            gatewayFactory = UnavailableDriveGatewayFactory(),
            ioDispatcher = Dispatchers.Unconfined,
        )
        assertTrue(manager.syncNow("user@example.com") is DriveSyncResult.Failed)
    }
}
