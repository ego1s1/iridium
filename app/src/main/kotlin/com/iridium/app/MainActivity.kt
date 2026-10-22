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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var importHandler: ImportDeepLinkHandler

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
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    /** Imports a shared/opened EPUB, then opens it in the reader. */
    private fun handleDeepLink(intent: Intent?) {
        lifecycleScope.launch {
            val bookId = importHandler.handle(intent) ?: return@launch
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
