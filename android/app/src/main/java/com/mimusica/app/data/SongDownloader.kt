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
        val audio = storage.find(songId) ?: fetchAudio(songId)
        val lyrics = remote.getLyrics(songId)
        songDao.upsertLyrics(LyricsEntity(songId, lyrics.syncedLrc, lyrics.plain, lyrics.offsetMs))
        songDao.setAudioPath(songId, audio.absolutePath)
    }

    /** Downloads to a temporary file, then names it after the format the server really sent. */
    private suspend fun fetchAudio(songId: String): File {
        val part = storage.fileFor(songId, AudioStorage.PART_EXTENSION)
        try {
            val mediaType = remote.downloadAudio(songId, part)
            val target = storage.fileFor(songId, AudioFormat.extensionFor(mediaType))
            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            return target
        } catch (e: Throwable) {
            part.delete()
            throw e
        }
    }
}
