package com.iridium.feature.reader.impl

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import androidx.compose.ui.unit.dp
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** Page-turn slide duration: the reference flow settles in ~180 ms. */
internal const val PageTurnDurationMs = 180

/** Delay before capturing a settled page (lets the swap finish painting). */
internal const val PageSnapshotSettleMs = 120L

/** Top bar + its padding, in dp (blanked from turn snapshots). */
internal val ChromeTopStrip = 76.dp

/** Scrubber island + its padding, in dp (blanked from turn snapshots). */
internal val ChromeBottomStrip = 90.dp

/**
 * A tap-time page-turn pulse: [id] increments per accepted turn so the UI
 * restarts the slide even for same-direction repeats; [direction] is +1
 * forward, -1 backward. [Idle] is the pre-first-turn value.
 */
internal data class PageTurnPulse(val id: Long, val direction: Int) {
    companion object {
        val Idle = PageTurnPulse(0L, 1)
    }
}

/**
 * Captures the settled reader page for the next turn's outgoing layer.
 * Uses PixelCopy (hardware-accurate); returns null where unavailable so the
 * slide degrades to a plain slide-in instead of failing. The chrome strips
 * are repainted with the page colour: a window capture bakes the bars in and
 * they would otherwise ghost across the slide.
 */
internal suspend fun capturePage(
    activity: Activity?,
    view: View?,
    blankTopPx: Int,
    blankBottomPx: Int,
    fillArgb: Int,
): Bitmap? {
    if (activity == null || view == null) return null
    if (view.width <= 0 || view.height <= 0) return null
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
    val bitmap = suspendCancellableCoroutine<Bitmap?> { cont ->
        val loc = IntArray(2)
        view.getLocationInWindow(loc)
        val src = Rect(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
        val target = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.RGB_565)
        PixelCopy.request(
            activity.window,
            src,
            target,
            { result ->
                if (cont.isActive) {
                    cont.resume(if (result == PixelCopy.SUCCESS) target else null)
                }
            },
            Handler(Looper.getMainLooper()),
        )
    } ?: return null
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply { color = fillArgb }
    if (blankTopPx > 0) {
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), blankTopPx.toFloat(), paint)
    }
    if (blankBottomPx > 0) {
        canvas.drawRect(
            0f,
            (bitmap.height - blankBottomPx).toFloat(),
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            paint,
        )
    }
    return bitmap
}
