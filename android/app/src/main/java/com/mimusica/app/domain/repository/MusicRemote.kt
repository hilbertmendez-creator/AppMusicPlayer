package com.mimusica.app.domain.repository

import com.mimusica.app.domain.model.DownloadJob
import com.mimusica.app.domain.model.Lyrics
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.SearchResult
import com.mimusica.app.domain.model.Song
import java.io.File

/**
 * Port to the family music server. Implementations throw
 * [com.mimusica.app.domain.error.NoInternetException] or [com.mimusica.app.domain.error.ApiException].
 */
interface MusicRemote {
    suspend fun search(query: String, mode: String = "auto", limit: Int = 5): List<SearchResult>
    suspend fun requestDownload(sourceId: String, title: String? = null, artist: String? = null): DownloadJob
    suspend fun getDownload(jobId: String): DownloadJob
    /** Persistent id of the server's library, or null when the server predates the endpoint (404). */
    suspend fun getLibraryId(): String?
    suspend fun listSongs(): List<Song>
    suspend fun getSong(id: String): Song
    suspend fun deleteSong(id: String)
    suspend fun getLyrics(songId: String): Lyrics
    suspend fun downloadAudio(songId: String, target: File)
    suspend fun listPlaylists(): List<Playlist>
    suspend fun createPlaylist(name: String, songIds: List<String> = emptyList()): Playlist
    suspend fun updatePlaylist(id: String, name: String? = null, songIds: List<String>? = null): Playlist
    suspend fun deletePlaylist(id: String)
}
