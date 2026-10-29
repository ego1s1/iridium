package com.iridium.feature.reader.impl

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.iridium.core.designsystem.IridiumEnter
import com.iridium.core.designsystem.IridiumEnterKind

/**
 * The chrome overlay: top bar + bottom scrubber animated as one unit over a
 * static page. Both bars float above the WebView and never participate in
 * layout, so tapping can neither resize content nor repaginate the book.
 */
@Composable
internal fun BoxScope.ReaderChromeOverlay(
    visible: Boolean,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = IridiumEnter.enter(IridiumEnterKind.CHROME_TOP),
        exit = IridiumEnter.exit(IridiumEnterKind.CHROME_TOP),
        modifier = Modifier.align(Alignment.TopCenter),
    ) {
        topBar()
    }
    AnimatedVisibility(
        visible = visible,
        enter = IridiumEnter.enter(IridiumEnterKind.CHROME_BOTTOM),
        exit = IridiumEnter.exit(IridiumEnterKind.CHROME_BOTTOM),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        bottomBar()
    }
}
