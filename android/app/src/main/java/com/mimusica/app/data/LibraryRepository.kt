package com.mimusica.app.data

import com.mimusica.app.data.local.LyricsEntity
import com.mimusica.app.data.local.PlaylistDao
import com.mimusica.app.data.local.PlaylistEntity
import com.mimusica.app.data.local.SongDao
import com.mimusica.app.data.local.SongEntity
import com.mimusica.app.data.local.toDomain
import com.mimusica.app.data.local.toEntity
import com.mimusica.app.domain.model.Lyrics
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.Song
import com.mimusica.app.domain.repository.MusicRemote
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/** Schedules the file download of a song (WorkManager in production, a fake in tests). */
interface DownloadScheduler {
    fun enqueueDownload(songId: String)
}

/** Where audio files live on the device. */
interface AudioStorage {
    fun fileFor(songId: String): File
    fun exists(songId: String): Boolean
    fun delete(songId: String)
}

/** Remembers which server library this phone last synced with. */
interface SyncStateStore {
    suspend fun lastLibraryId(): String?
    suspend fun setLastLibraryId(id: String)
}

/**
 * [serverMismatch] is true when the server looked like a different/unknown/empty library and local
 * deletions that a normal sync would have made were skipped to protect the phone's data.
 */
data class SyncReport(
    val queuedDownloads: Int,
    val removedSongs: Int,
    val serverMismatch: Boolean = false,
)

/**
 * The server is the source of truth for a library the phone has seen before. This repository mirrors
 * it into Room so the app works offline, and schedules downloads for songs the device does not have yet.
 * Local data is only ever deleted when the server proves to be the same library (see [sync]).
 */
class LibraryRepository(
    private val remote: MusicRemote,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val scheduler: DownloadScheduler,
    private val storage: AudioStorage,
    private val syncState: SyncStateStore,
) {
    /** Songs whose audio is already on the device, ordered by title. */
    fun observeDownloadedSongs(): Flow<List<Song>> =
        songDao.observeAll().map { list -> list.filter { it.audioPath != null }.map { it.toDomain() } }

    fun observePlaylists(): Flow<List<Playlist>> =
        playlistDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getPlaylist(id: String): Playlist? = playlistDao.getById(id)?.toDomain()

    suspend fun getSong(id: String): Song? = songDao.getById(id)?.toDomain()

    suspend fun getLyrics(songId: String): Lyrics? =
        songDao.getLyrics(songId)?.let { Lyrics(it.syncedLrc, it.plain, it.offsetMs) }

    /**
     * Pulls songs and playlists from the server. Throws before touching local data if the server is unreachable.
     *
     * Deletions are only applied when the server's library id equals the one stored on the phone. Otherwise
     * (new/unknown/older server, or an empty answer for a phone that has songs) remote data is merged but
     * nothing local is removed, so pointing the phone at an empty server can never wipe the library.
     */
    suspend fun sync(): SyncReport {
        val remoteId = remote.getLibraryId()
        val remoteSongs = remote.listSongs()
        val remotePlaylists = remote.listPlaylists()
        val local = songDao.getAll().associateBy { it.id }
        val localPlaylists = playlistDao.getAll()

        val storedId = syncState.lastLibraryId()
        val phoneIsEmpty = local.isEmpty() && localPlaylists.isEmpty()
        var trusted = storedId != null && storedId == remoteId
        if (storedId == null && remoteId != null && phoneIsEmpty) trusted = true
        if (trusted && remoteSongs.isEmpty() && local.values.any { it.audioPath != null }) trusted = false

        val merged = remoteSongs.map { it.toEntity(audioPath = usableAudioPath(local[it.id])) }
        songDao.upsert(merged)

        val remoteIds = remoteSongs.map { it.id }.toSet()
        val staleSongs = local.keys - remoteIds
        val removed = if (trusted) staleSongs else emptySet()
        removed.forEach { removeLocalSong(it) }

        val missing = merged.filter { it.audioPath == null }
        missing.forEach { scheduler.enqueueDownload(it.id) }

        remoteSongs.forEach { song ->
            val previous = local[song.id]
            if (previous != null && previous.audioPath != null && needsLyricsRefresh(previous, song)) {
                refreshLyrics(song.id)
            }
        }

        val remotePlaylistIds = remotePlaylists.map { it.id }.toSet()
        val stalePlaylists = localPlaylists.filter { it.id !in remotePlaylistIds }
        mirrorPlaylists(remotePlaylists, stalePlaylists.takeIf { trusted }.orEmpty())

        // Adopt the id only after a complete, successful pass; never overwrite a different stored id.
        if (storedId == null && remoteId != null) syncState.setLastLibraryId(remoteId)

        val skipped = !trusted && (staleSongs.isNotEmpty() || stalePlaylists.isNotEmpty())
        return SyncReport(queuedDownloads = missing.size, removedSongs = removed.size, serverMismatch = skipped)
    }

    /** After the server finished a download job: store the metadata and fetch the file to this device. */
    suspend fun addDownloadedSong(songId: String) {
        val song = remote.getSong(songId)
        val existing = songDao.getById(songId)
        songDao.upsert(listOf(song.toEntity(audioPath = usableAudioPath(existing))))
        if (usableAudioPath(existing) == null) scheduler.enqueueDownload(songId)
    }

    /** Deletes on the server first; the local copy only goes away if that worked. */
    suspend fun deleteSong(id: String) {
        remote.deleteSong(id)
        removeLocalSong(id)
        val affected = playlistDao.getAll().map { it.toDomain() }.filter { id in it.songIds }
        if (affected.isNotEmpty()) {
            playlistDao.upsert(affected.map { it.copy(songIds = it.songIds - id).toEntity() })
        }
    }

    suspend fun createPlaylist(name: String, songIds: List<String> = emptyList()): Playlist {
        val created = remote.createPlaylist(name, songIds)
        playlistDao.upsert(listOf(created.toEntity()))
        return created
    }

    suspend fun updatePlaylist(id: String, name: String?, songIds: List<String>?): Playlist {
        val updated = remote.updatePlaylist(id, name, songIds)
        playlistDao.upsert(listOf(updated.toEntity()))
        return updated
    }

    suspend fun deletePlaylist(id: String) {
        remote.deletePlaylist(id)
        playlistDao.deleteById(id)
    }

    private fun usableAudioPath(entity: SongEntity?): String? =
        entity?.audioPath?.takeIf { storage.exists(entity.id) }

    private suspend fun removeLocalSong(id: String) {
        songDao.deleteById(id)
        songDao.deleteLyrics(id)
        storage.delete(id)
    }

    private fun needsLyricsRefresh(previous: SongEntity, current: Song): Boolean =
        previous.hasSyncedLyrics != current.hasSyncedLyrics ||
            previous.hasPlainLyrics != current.hasPlainLyrics ||
            previous.lyricsOffsetMs != current.lyricsOffsetMs

    private suspend fun refreshLyrics(songId: String) {
        try {
            val lyrics = remote.getLyrics(songId)
            songDao.upsertLyrics(LyricsEntity(songId, lyrics.syncedLrc, lyrics.plain, lyrics.offsetMs))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep the previous lyrics; the next sync will try again.
        }
    }

    private suspend fun mirrorPlaylists(remotePlaylists: List<Playlist>, toDelete: List<PlaylistEntity>) {
        toDelete.forEach { playlistDao.deleteById(it.id) }
        playlistDao.upsert(remotePlaylists.map { it.toEntity() })
    }
}
