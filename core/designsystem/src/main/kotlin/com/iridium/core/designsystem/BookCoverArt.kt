package com.iridium.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Scale
import java.io.File

/**
 * Book cover with a tonal bed + MenuBook placeholder until Coil reports
 * Success. [coverPath] is an app-private file path (or null for placeholders).
 * The request is remembered per path and bounded to an inexact fill so Coil
 * may serve a smaller cached bitmap for grid cells.
 */
@Composable
fun BookCoverArt(
    coverPath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (coverPath != null) {
            val appContext = LocalContext.current.applicationContext
            val request = remember(coverPath) {
                ImageRequest.Builder(appContext)
                    .data(File(coverPath))
                    .crossfade(false)
                    .precision(Precision.INEXACT)
                    .scale(Scale.FILL)
                    .build()
            }
            val painter = rememberAsyncImagePainter(
                model = request,
                contentScale = ContentScale.Crop,
            )
            val painterState by painter.state.collectAsStateWithLifecycle()
            Image(
                painter = painter,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (painterState !is AsyncImagePainter.State.Success) {
                CoverPlaceholder()
            }
        } else {
            CoverPlaceholder()
        }
    }
}

@Composable
private fun CoverPlaceholder() {
    Icon(
        imageVector = Icons.Rounded.MenuBook,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.size(40.dp),
    )
}
