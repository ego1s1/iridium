package com.iridium.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.iridium.feature.reader.api.ReaderKeyInterceptor
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IridiumApp()
        }
    }

    /**
     * Reader-owned keys (volume paging) are consumed before the system sees
     * them, so a handled press never also moves the system volume.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (ReaderKeyInterceptor.handler?.invoke(event) == true) return true
        return super.dispatchKeyEvent(event)
    }
}
