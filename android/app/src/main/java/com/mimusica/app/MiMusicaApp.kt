package com.mimusica.app

import android.app.Application
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.OkHttpClient

/**
 * WorkManager is initialised on demand (see the manifest) so its Room database is not opened on the main thread
 * at process start; Coil gets a lazily built network client for the same reason.
 */
class MiMusicaApp : Application(), Configuration.Provider, ImageLoaderFactory {
    val container: AppContainer by lazy { AppContainer(this) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .callFactory { OkHttpClient() }
            .crossfade(false)
            .build()

    override fun onCreate() {
        super.onCreate()
        container.start()
    }
}
