package com.iridium.app

import android.app.Application
import android.content.Context
import android.os.StrictMode
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class IridiumApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            // Debug-only hygiene net: log main-thread disk/network use and
            // leaked closables/activities while developing. Penalty is
            // log-only (never death) so a slow emulator cannot fail a run,
            // and release builds never install these policies.
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .build(),
            )
        }
    }

    /**
     * Cover image loader.
     *
     * Without this, Coil builds its default singleton: a memory cache sized at
     * a quarter of the heap and a disk cache that duplicates files which
     * already live in `files/covers` as JPEGs. A decoded grid cover is ~1.5 MB,
     * so the default let the library hold dozens of them for the life of the
     * process, including while reading.
     *
     * The disk cache stays off at the request level (see `BookCoverArt`): the
     * source is already a file in app-private storage, so a second copy is
     * pure cost. 15% of the heap comfortably holds a screen and a half of grid
     * cells plus the small list thumbnails.
     */
    override fun newImageLoader(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, MEMORY_CACHE_HEAP_FRACTION)
                    .build()
            }
            .build()

    private companion object {
        const val MEMORY_CACHE_HEAP_FRACTION = 0.15
    }
}
