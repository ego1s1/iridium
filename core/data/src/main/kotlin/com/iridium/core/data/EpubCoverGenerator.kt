package com.iridium.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes cover thumbnails under `filesDir/covers/<coverId>.jpg`. Covers come
 * from the EPUB's own cover image, already extracted by the parser, so no
 * second parse is needed during indexing.
 */
@Singleton
internal class EpubCoverGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun generate(coverBytes: ByteArray?, coverId: String): String? =
        withContext(Dispatchers.IO) {
            if (coverBytes == null || coverBytes.isEmpty()) return@withContext null
            val bitmap = runCatching {
                decodeDownsampled(coverBytes, COVER_MAX_DIMENSION)
            }.getOrNull() ?: return@withContext null
            try {
                val coversDir = File(context.filesDir, COVERS_DIR).apply { mkdirs() }
                val dest = File(coversDir, "$coverId.jpg")
                FileOutputStream(dest).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, COVER_QUALITY, out)
                }
                bitmap.recycle()
                dest.absolutePath
            } catch (_: Exception) {
                runCatching { bitmap.recycle() }
                null
            }
        }

    private fun decodeDownsampled(bytes: ByteArray, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    companion object {
        const val COVERS_DIR = "covers"
        const val COVER_MAX_DIMENSION = 512
        const val COVER_QUALITY = 85
    }
}
