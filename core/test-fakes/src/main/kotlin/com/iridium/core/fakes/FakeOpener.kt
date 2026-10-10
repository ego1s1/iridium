package com.iridium.core.fakes

import com.iridium.core.data.BookOpener
import com.iridium.core.data.OpenResult

/**
 * Scripted book opener for ViewModel tests: always fails with [failure].
 * Success needs a real Readium `Publication`, which unit tests cannot build;
 * failure routing (the uncovered path) is what this fakes. Replaces the
 * real streamer, which hangs under Robolectric.
 */
class FakeOpener(
    var failure: OpenResult = OpenResult.FileMissing,
) : BookOpener {

    val openedIds = mutableListOf<String>()

    override suspend fun open(sourcePath: String): OpenResult {
        openedIds += sourcePath
        return failure
    }
}
