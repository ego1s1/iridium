package com.iridium.feature.reader.impl

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.iridium.core.designsystem.IridiumEnter
import com.iridium.core.designsystem.IridiumEnterKind

/**
 * Full-screen showcase for a tapped book image (image showcase). Loads the
 * tapped resource's bytes and draws them edge-to-edge; a tap anywhere
 * dismisses back to the book.
 */
@Composable
internal fun ReaderImageShowcase(
    src: String,
    loadImageBytes: suspend (String) -> ByteArray?,
    onDismiss: () -> Unit,
) {
    var bitmap by remember(src) { mutableStateOf<ImageBitmap?>(null) }
    val context = LocalContext.current
    LaunchedEffect(src) {
        bitmap = runCatching {
            val bytes = loadImageBytes(src) ?: return@runCatching null
            decodeShowcaseBitmap(bytes, context)?.asImageBitmap()
        }.getOrNull()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onDismiss)
            .testTag("readerImageShowcase"),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp == null) {
            CircularProgressIndicator(color = Color.White)
        } else {
            key(bmp) {
                var entered by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { entered = true }
                AnimatedVisibility(
                    visible = entered,
                    enter = IridiumEnter.enter(IridiumEnterKind.DIALOG_POP),
                    exit = IridiumEnter.exit(IridiumEnterKind.DIALOG_POP),
                    label = "showcaseImageEnter",
                ) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/**
 * Bounded showcase decode: subsamples huge EPUB illustrations down to the
 * display size before allocating pixels. A 4000x6000 plate is ~96MB decoded
 * at full res — enough to OOM the reader — while the screen can only show
 * ~2MP of it anyway.
 */
internal fun decodeShowcaseBitmap(bytes: ByteArray, context: Context): android.graphics.Bitmap? {
    if (bytes.isEmpty()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val metrics = context.resources.displayMetrics
    val screenMax = maxOf(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(1)
    val imageMax = maxOf(bounds.outWidth, bounds.outHeight)
    var sample = 1
    while (imageMax / (sample * 2) >= screenMax) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
}
