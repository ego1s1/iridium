package com.iridium.core.data

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * SAF tree walk: finds EPUBs, skips hidden directories, never prunes on
 * failure. The tree is faked in memory (no provider); only Uri needs the
 * Android runtime, hence Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SafLinkedTreeListerTest {

    private fun doc(path: String, dir: Boolean = false): TreeDoc =
        TreeDoc(Uri.parse("content://tree/$path"), path.substringAfterLast('/'), dir, 0L)

    private fun tree(
        files: Map<String, List<TreeDoc>>,
        failOn: Set<String> = emptySet(),
    ): (TreeDoc) -> List<TreeDoc>? = { dir ->
        val key = dir.uri.toString()
        when {
            key in failOn -> null
            else -> files[key]
        }
    }

    @Test
    fun `finds epubs recursively`() {
        val root = doc("root", dir = true)
        val kids = mapOf(
            root.uri.toString() to listOf(
                doc("root/a.epub"),
                doc("root/notes.txt"),
                doc("root/sub", dir = true),
            ),
            "content://tree/root/sub" to listOf(
                doc("root/sub/b.epub"),
                doc("root/sub/deep", dir = true),
            ),
            "content://tree/root/sub/deep" to listOf(doc("root/sub/deep/c.epub")),
        )

        val result = collectLinkedBooks(root, tree(kids))

        assertFalse(result.walkFailed)
        assertEquals(
            setOf("a.epub", "b.epub", "c.epub"),
            result.documents.map { it.name }.toSet(),
        )
    }

    @Test
    fun `matches epub extension case-insensitively`() {
        val root = doc("root", dir = true)
        val kids = mapOf(
            root.uri.toString() to listOf(doc("root/upper.EPUB"), doc("root/mixed.Epub")),
        )

        assertEquals(2, collectLinkedBooks(root, tree(kids)).documents.size)
    }

    @Test
    fun `skips hidden directories and files`() {
        val root = doc("root", dir = true)
        val kids = mapOf(
            root.uri.toString() to listOf(
                doc("root/.hidden", dir = true),
                doc("root/.thumb.db"),
                doc("root/ok.epub"),
            ),
            "content://tree/root/.hidden" to listOf(doc("root/.hidden/y.epub")),
        )

        val result = collectLinkedBooks(root, tree(kids))

        assertFalse(result.walkFailed)
        assertEquals(listOf("ok.epub"), result.documents.map { it.name })
    }

    @Test
    fun `failed subtree marks partial instead of pruning`() {
        val root = doc("root", dir = true)
        val kids = mapOf(
            root.uri.toString() to listOf(
                doc("root/ok.epub"),
                doc("root/bad", dir = true),
            ),
        )

        val result = collectLinkedBooks(
            root,
            tree(kids, failOn = setOf("content://tree/root/bad")),
        )

        assertTrue(result.walkFailed)
        assertEquals(listOf("ok.epub"), result.documents.map { it.name })
    }
}
