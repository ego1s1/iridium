package com.iridium.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.iridium.feature.reader.api.ReaderKeyInterceptor
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /**
     * Lazy on purpose: resolving this builds the whole library data graph
     * (Room, the DAOs, the EPUB engine) and injection happens before
     * `onCreate`, on the main thread, on every cold start - even for a
     * launcher intent with nothing to import.
     */
    @Inject
    lateinit var importHandler: Lazy<ImportDeepLinkHandler>

    @Inject
    lateinit var deepLinkNavigator: DeepLinkNavigator

    @Inject
    lateinit var keyInterceptor: ReaderKeyInterceptor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IridiumApp()
        }
        // Recreation (theme, locale, multi-window) re-delivers the same
        // intent: only handle a fresh launch here, onNewIntent covers the rest.
        if (savedInstanceState == null) handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private var lastHandledDeepLink: String? = null

    /** Imports a shared/opened EPUB, then opens it in the reader. */
    private fun handleDeepLink(intent: Intent?) {
        lifecycleScope.launch {
            // Resolve the handler only for an intent that is actually ours;
            // the check itself allocates nothing and touches no storage.
            val handler = importHandler.get()
            val request = handler.parseIntent(intent) ?: return@launch
            val key = request.uri.toString()
            // A redelivered intent (recreation, task restore) must never
            // import twice.
            if (key == lastHandledDeepLink) return@launch
            val bookId = handler.handle(intent) ?: return@launch
            lastHandledDeepLink = key
            deepLinkNavigator.openBook(bookId)
        }
    }

    /**
     * Reader-owned keys (volume paging) are consumed before the system sees
     * them, so a handled press never also moves the system volume.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (keyInterceptor.consume(event)) return true
        return super.dispatchKeyEvent(event)
    }
}
