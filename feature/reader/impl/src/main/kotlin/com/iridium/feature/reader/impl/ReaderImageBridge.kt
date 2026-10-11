package com.iridium.feature.reader.impl

import android.webkit.JavascriptInterface

/**
 * Image-showcase plumbing for the reader (tap-to-view).
 *
 * This detects taps on `<img>` not inside a link and calls `openImage`
 * into native. We replicate that with a tiny injected script that binds image
 * taps to the registered `IridiumReader` JS interface. Pure string + testable;
 * the on-device visual flow is verified separately.
 */
object ReaderImageBridge {

    /**
     * JS that wiretap every `<img>` not wrapped in a link and reports the tap
     * to `window.IridiumReader.onImageTap(src)`. Idempotent per element via a
     * data-attribute so re-running on a new resource never double-binds.
     */
    fun imageTapScript(): String = """
        (function() {
          var mark = 'data-iridium-image-bound';
          function bind(root) {
            var imgs = root.querySelectorAll('img');
            for (var i = 0; i < imgs.length; i++) {
              var img = imgs[i];
              if (img.getAttribute(mark)) continue;
              img.setAttribute(mark, '1');
              img.addEventListener('click', function(e) {
                // Links own their click: let openLink handle those.
                var a = this.closest('a[href]');
                if (a) return;
                e.preventDefault();
                e.stopPropagation();
                var s = this.currentSrc || this.src || '';
                if (window.IridiumReader && window.IridiumReader.onImageTap) {
                  window.IridiumReader.onImageTap(s);
                }
              });
            }
          }
          bind(document);
        })();
    """.trimIndent()
}

/**
 * Native side of the image bridge: pushed by the injected script's
 * `window.IridiumReader.onImageTap(src)` calls and handed to the reader store.
 */
internal class IridiumReaderJsInterface(
    private val onImageTap: (String) -> Unit,
) {
    @JavascriptInterface
    fun onImageTap(src: String?) {
        if (!src.isNullOrBlank()) onImageTap(src)
    }
}
