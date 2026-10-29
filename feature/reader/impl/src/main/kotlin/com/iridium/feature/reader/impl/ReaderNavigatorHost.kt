package com.iridium.feature.reader.impl

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import com.iridium.core.designsystem.findActivity

/** Hosts the [ReaderHostFragment] inside Compose via FragmentContainerView. */
@Composable
internal fun NavigatorHost(
    bookId: String,
    sessionReady: Boolean,
    onViewReady: (View) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    if (activity == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.reader_unavailable))
        }
        return
    }
    // Stable id so the FragmentManager can restore the navigator fragment
    // across configuration changes (a generated id would not be restorable).
    val containerId = R.id.reader_navigator_container
    val tag = "iridium_reader_$bookId"

    // The container is always composed so FragmentManager has somewhere to
    // restore into; the host fragment itself is only added once the session
    // is published. Adding it earlier handed Readium a null navigator factory
    // and collided with an in-flight composition on the first open.
    AndroidView(
        factory = { ctx -> FragmentContainerView(ctx).apply { id = containerId } },
        update = { onViewReady(it) },
        modifier = modifier.fillMaxSize(),
    )

    // No spinner here: ReaderScreen owns the single loading surface, and a
    // second animating indicator froze along with the main thread during attach.
    if (!sessionReady) return

    LaunchedEffect(bookId, containerId, sessionReady) {
        val fm = activity.supportFragmentManager
        if (fm.findFragmentByTag(tag) != null || fm.findFragmentById(containerId) != null) {
            return@LaunchedEffect
        }
        val fragment = ReaderHostFragment.newInstance()
        if (fm.isStateSaved) {
            // The activity already saved state (e.g. opening during a
            // restore): commitNow would throw, so allow state loss.
            fm.beginTransaction().add(containerId, fragment, tag).commitAllowingStateLoss()
        } else {
            fm.commitNow { add(containerId, fragment, tag) }
        }
    }

    DisposableEffect(bookId, containerId) {
        onDispose {
            // Config change (rotation) disposes effects but the FragmentManager
            // restores the navigator itself into the stable container id —
            // only detach when truly leaving the reader.
            if (activity.isChangingConfigurations || activity.isFinishing) return@onDispose
            // Leaving the reader: detach the navigator so a stale WebView never
            // lingers behind the library. commitAllowingStateLoss (not
            // commitNow) avoids throwing if the host already saved state.
            val fm = activity.supportFragmentManager
            fm.findFragmentByTag(tag)?.takeIf { it.isAdded }?.let {
                fm.beginTransaction().remove(it).commitAllowingStateLoss()
            }
        }
    }
}
