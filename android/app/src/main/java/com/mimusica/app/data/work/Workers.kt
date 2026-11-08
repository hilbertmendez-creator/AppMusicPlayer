package com.mimusica.app.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.mimusica.app.MiMusicaApp
import com.mimusica.app.data.DownloadScheduler
import com.mimusica.app.domain.error.ApiException
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

private val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

/** Downloads one song (audio + lyrics) to app-private storage. Retries with backoff. */
class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val songId = inputData.getString(KEY_SONG_ID) ?: return Result.failure()
        val container = (applicationContext as MiMusicaApp).container
        return try {
            container.songDownloader.download(songId)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            if (e.code == 404) Result.failure() else retryOrFail()
        } catch (e: Exception) {
            retryOrFail()
        }
    }

    private fun retryOrFail(): Result =
        if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()

    companion object {
        const val KEY_SONG_ID = "song_id"
        private const val MAX_ATTEMPTS = 8
    }
}

/** Pulls songs and playlists from the server (periodic and on app start). */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as MiMusicaApp).container
        return try {
            container.library.sync()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

class WorkManagerDownloadScheduler(context: Context) : DownloadScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun enqueueDownload(songId: String) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(DownloadWorker.KEY_SONG_ID to songId))
            .setConstraints(connected)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("download-$songId", ExistingWorkPolicy.KEEP, request)
    }
}

class SyncScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    /** Every 15 minutes (the WorkManager minimum) while the device has a connection. */
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(connected)
            .build()
        workManager.enqueueUniquePeriodicWork("sync-periodic", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun syncOnStart() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(connected).build()
        workManager.enqueueUniqueWork("sync-start", ExistingWorkPolicy.REPLACE, request)
    }
}
