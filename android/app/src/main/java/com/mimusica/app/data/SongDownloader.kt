package com.mimusica.app.data

import com.mimusica.app.data.local.LyricsEntity
import com.mimusica.app.data.local.SongDao
import com.mimusica.app.data.local.toEntity
import com.mimusica.app.domain.repository.MusicRemote
import java.io.File

/** Downloads a song's audio and lyrics into app-private storage and then marks it available. */
class SongDownloader(
    private val remote: MusicRemote,
    private val songDao: SongDao,
    private val storage: AudioStorage,
) {
    suspend fun download(songId: String) {
        if (songDao.getById(songId) == null) {
            songDao.upsert(listOf(remote.getSong(songId).toEntity(audioPath = null)))
        }
        val target = storage.fileFor(songId)
        if (!target.exists() || target.length() == 0L) {
            fetchAudio(songId, target)
        }
        val lyrics = remote.getLyrics(songId)
        songDao.upsertLyrics(LyricsEntity(songId, lyrics.syncedLrc, lyrics.plain, lyrics.offsetMs))
        songDao.setAudioPath(songId, target.absolutePath)
    }

    private suspend fun fetchAudio(songId: String, target: File) {
        val part = File(target.path + ".part")
        try {
            remote.downloadAudio(songId, part)
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
        } catch (e: Throwable) {
            part.delete()
            throw e
        }
    }
}
