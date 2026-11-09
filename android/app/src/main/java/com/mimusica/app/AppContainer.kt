package com.mimusica.app

import android.app.Application
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.Room
import com.mimusica.app.data.AudioStorage
import com.mimusica.app.data.FileAudioStorage
import com.mimusica.app.data.LibraryRepository
import com.mimusica.app.data.SongDownloader
import com.mimusica.app.data.SyncStateStore
import com.mimusica.app.data.local.AppDatabase
import com.mimusica.app.data.remote.ConnectionConfig
import com.mimusica.app.data.remote.NetworkFactory
import com.mimusica.app.data.remote.RemoteMusicApi
import com.mimusica.app.data.update.UpdateManager
import com.mimusica.app.data.settings.DataStoreSettingsStore
import com.mimusica.app.data.settings.SettingsStore
import com.mimusica.app.data.work.SyncScheduler
import com.mimusica.app.data.work.WorkManagerDownloadScheduler
import com.mimusica.app.domain.player.PlayerPort
import com.mimusica.app.domain.repository.MusicRemote
import com.mimusica.app.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File

/** Manual dependency graph (no Hilt). Created once by [MiMusicaApp]. */
class AppContainer(private val app: Application) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val dataStoreSettings = DataStoreSettingsStore(app)
    val settingsStore: SettingsStore = dataStoreSettings
    private val syncState: SyncStateStore = dataStoreSettings

    private val connection = ConnectionConfig()
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(app, AppDatabase::class.java, "mimusica.db").build()
    }
    // Lazy: FileAudioStorage creates its folder (disk IO), which must not happen on the main thread at launch.
    val storage: AudioStorage by lazy { FileAudioStorage(File(app.filesDir, "songs")) }

    val remote: MusicRemote by lazy { RemoteMusicApi(NetworkFactory.service(NetworkFactory.okHttp(connection))) }
    private val downloadScheduler by lazy { WorkManagerDownloadScheduler(app) }

    val library: LibraryRepository by lazy {
        LibraryRepository(remote, database.songDao(), database.playlistDao(), downloadScheduler, storage, syncState)
    }
    val songDownloader: SongDownloader by lazy { SongDownloader(remote, database.songDao(), storage) }

    val player: PlayerPort by lazy { PlayerController(app, appScope) }

    val updates: UpdateManager by lazy { UpdateManager(app, appScope) }

    /**
     * Keeps the HTTP layer in sync with the caregiver settings and schedules syncing once the app is configured.
     * Nothing here may block the main thread: it runs before the first frame.
     */
    fun start() {
        updates.checkOnStart() // network work runs on Dispatchers.IO inside; failures are silent
        appScope.launch {
            // Heavy singletons (Room, OkHttp/Retrofit, WorkManager) are built on a worker thread right away,
            // so by the time Home composes they are ready instead of being created during composition.
            launch(Dispatchers.Default) { warmUp() }
            var scheduled = false
            settingsStore.settings.collect { settings ->
                connection.baseUrl = settings.serverUrl.toHttpUrlOrNull()
                connection.token = settings.token
                if (settings.isConfigured && !scheduled) {
                    scheduled = true
                    withContext(Dispatchers.Default) {
                        // First WorkManager use initialises its own database: keep that off the main thread.
                        val syncScheduler = SyncScheduler(app)
                        syncScheduler.schedulePeriodic()
                        syncScheduler.syncOnStart()
                    }
                }
            }
        }
    }

    private suspend fun warmUp() {
        library
        songDownloader
        withContext(Dispatchers.IO) { database.openHelper.writableDatabase }
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
