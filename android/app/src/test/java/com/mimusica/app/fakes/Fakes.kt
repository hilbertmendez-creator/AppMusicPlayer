package com.mimusica.app.fakes

import com.mimusica.app.data.AudioStorage
import com.mimusica.app.data.DownloadScheduler
import com.mimusica.app.data.SyncStateStore
import com.mimusica.app.data.local.LyricsEntity
import com.mimusica.app.data.local.PlaylistDao
import com.mimusica.app.data.local.PlaylistEntity
import com.mimusica.app.data.local.SongDao
import com.mimusica.app.data.local.SongEntity
import com.mimusica.app.domain.model.DownloadJob
import com.mimusica.app.domain.model.Lyrics
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.SearchResult
import com.mimusica.app.domain.model.Song
import com.mimusica.app.domain.player.PlaybackState
import com.mimusica.app.domain.player.PlayerPort
import com.mimusica.app.domain.repository.MusicRemote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File

fun song(
    id: String,
    title: String = "Song $id",
    artist: String = "Artist",
    synced: Boolean = false,
    plain: Boolean = false,
    offset: Int = 0,
    audioPath: String? = null,
) = Song(id, "src-$id", title, artist, 100, null, synced, plain, offset, audioPath)

class FakeSongDao : SongDao {
    val songs = MutableStateFlow<Map<String, SongEntity>>(emptyMap())
    val lyrics = mutableMapOf<String, LyricsEntity>()

    override fun observeAll(): Flow<List<SongEntity>> = songs.map { it.values.toList() }
    override suspend fun getAll(): List<SongEntity> = songs.value.values.toList()
    override suspend fun getById(id: String): SongEntity? = songs.value[id]
    override suspend fun upsert(songs: List<SongEntity>) {
        this.songs.value = this.songs.value + songs.associateBy { it.id }
    }
    override suspend fun deleteById(id: String) {
        songs.value = songs.value - id
    }
    override suspend fun setAudioPath(id: String, path: String?) {
        songs.value[id]?.let { songs.value = songs.value + (id to it.copy(audioPath = path)) }
    }
    override suspend fun upsertLyrics(lyrics: LyricsEntity) {
        this.lyrics[lyrics.songId] = lyrics
    }
    override suspend fun getLyrics(songId: String): LyricsEntity? = lyrics[songId]
    override suspend fun deleteLyrics(songId: String) {
        lyrics.remove(songId)
    }
}

class FakePlaylistDao : PlaylistDao {
    val playlists = MutableStateFlow<Map<String, PlaylistEntity>>(emptyMap())

    override fun observeAll(): Flow<List<PlaylistEntity>> = playlists.map { it.values.toList() }
    override suspend fun getAll(): List<PlaylistEntity> = playlists.value.values.toList()
    override suspend fun getById(id: String): PlaylistEntity? = playlists.value[id]
    override suspend fun upsert(playlists: List<PlaylistEntity>) {
        this.playlists.value = this.playlists.value + playlists.associateBy { it.id }
    }
    override suspend fun deleteById(id: String) {
        playlists.value = playlists.value - id
    }
}

class FakeScheduler : DownloadScheduler {
    val enqueued = mutableListOf<String>()
    override fun enqueueDownload(songId: String) {
        enqueued += songId
    }
}

class FakeStorage : AudioStorage {
    val files = mutableSetOf<String>()
    val deleted = mutableListOf<String>()
    override fun fileFor(songId: String, extension: String): File = File("/fake/$songId.$extension")
    override fun find(songId: String): File? = if (songId in files) File("/fake/$songId.m4a") else null
    override fun exists(songId: String): Boolean = songId in files
    override fun delete(songId: String) {
        files -= songId
        deleted += songId
    }
}

class FakeSyncState(var id: String? = null) : SyncStateStore {
    override suspend fun lastLibraryId(): String? = id
    override suspend fun setLastLibraryId(id: String) {
        this.id = id
    }
}

class FakeLastSung(initial: String? = null) : com.mimusica.app.data.LastSungStore {
    private val state = MutableStateFlow(initial)
    val value: String? get() = state.value
    override val lastSungSongId: Flow<String?> = state
    override suspend fun setLastSungSongId(id: String) {
        state.value = id
    }
}

class FakeRemote : MusicRemote {
    /** Media type the fake server reports for downloaded audio; null simulates a missing Content-Type. */
    var audioMediaType: String? = "audio/mp4"
    /** Identity the fake server reports; null simulates an older server without the endpoint. */
    var libraryId: String? = "lib-1"
    var songs: MutableList<Song> = mutableListOf()
    var playlists: MutableList<Playlist> = mutableListOf()
    var searchResults: List<SearchResult> = emptyList()
    var lyricsBySong: MutableMap<String, Lyrics> = mutableMapOf()
    var failure: Throwable? = null
    var jobs: ArrayDeque<DownloadJob> = ArrayDeque()
    val deletedSongs = mutableListOf<String>()
    val searchedQueries = mutableListOf<String>()
    var audioTarget: File? = null

    private fun check() {
        failure?.let { throw it }
    }

    override suspend fun search(query: String, mode: String, limit: Int): List<SearchResult> {
        check()
        searchedQueries += query
        return searchResults
    }
    override suspend fun requestDownload(sourceId: String, title: String?, artist: String?): DownloadJob {
        check()
        return jobs.removeFirst()
    }
    override suspend fun getDownload(jobId: String): DownloadJob {
        check()
        return if (jobs.size > 1) jobs.removeFirst() else jobs.first()
    }
    override suspend fun getLibraryId(): String? {
        check()
        return libraryId
    }
    override suspend fun listSongs(): List<Song> {
        check()
        return songs.toList()
    }
    override suspend fun getSong(id: String): Song {
        check()
        return songs.first { it.id == id }
    }
    override suspend fun deleteSong(id: String) {
        check()
        deletedSongs += id
        songs.removeAll { it.id == id }
    }
    override suspend fun getLyrics(songId: String): Lyrics {
        check()
        return lyricsBySong[songId] ?: Lyrics(null, null, 0)
    }
    override suspend fun downloadAudio(songId: String, target: File): String? {
        check()
        audioTarget = target
        target.parentFile?.mkdirs()
        target.writeBytes(byteArrayOf(1, 2, 3))
        return audioMediaType
    }
    override suspend fun listPlaylists(): List<Playlist> {
        check()
        return playlists.toList()
    }
    override suspend fun createPlaylist(name: String, songIds: List<String>): Playlist {
        check()
        val p = Playlist("p${playlists.size + 1}", name, songIds)
        playlists += p
        return p
    }
    override suspend fun updatePlaylist(id: String, name: String?, songIds: List<String>?): Playlist {
        check()
        val old = playlists.first { it.id == id }
        val updated = old.copy(name = name ?: old.name, songIds = songIds ?: old.songIds)
        playlists[playlists.indexOf(old)] = updated
        return updated
    }
    override suspend fun deletePlaylist(id: String) {
        check()
        playlists.removeAll { it.id == id }
    }
}

class FakePlayer : PlayerPort {
    override val state = MutableStateFlow(PlaybackState())
    val played = mutableListOf<List<Song>>()
    override fun play(songs: List<Song>, startIndex: Int) {
        played += songs
        state.value = state.value.copy(currentSong = songs[startIndex], isPlaying = true)
    }
    override fun togglePlayPause() {
        state.value = state.value.copy(isPlaying = !state.value.isPlaying)
    }
    override fun next() = Unit
    override fun previous() = Unit
    val seeks = mutableListOf<Long>()
    override fun seekTo(positionMs: Long) {
        seeks += positionMs
        state.value = state.value.copy(positionMs = positionMs)
    }
    override fun volumeUp() = Unit
    override fun volumeDown() = Unit
    fun setPosition(ms: Long) {
        state.value = state.value.copy(positionMs = ms)
    }
    fun setDuration(ms: Long) {
        state.value = state.value.copy(durationMs = ms)
    }
}
