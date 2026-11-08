package com.mimusica.app.data.remote

import com.mimusica.app.domain.error.ApiException
import com.mimusica.app.domain.error.NoInternetException
import com.mimusica.app.domain.model.DownloadJob
import com.mimusica.app.domain.model.Lyrics
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.SearchResult
import com.mimusica.app.domain.model.Song
import com.mimusica.app.domain.repository.MusicRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException

/** Retrofit-backed [MusicRemote]: maps DTOs to domain models and failures to domain exceptions. */
class RemoteMusicApi(private val service: MusicApiService) : MusicRemote {

    override suspend fun search(query: String, mode: String, limit: Int): List<SearchResult> =
        call { service.search(query, mode, limit).results.map { it.toDomain() } }

    override suspend fun requestDownload(sourceId: String, title: String?, artist: String?): DownloadJob =
        call { service.requestDownload(DownloadRequestDto(sourceId, title, artist)).toDomain() }

    override suspend fun getDownload(jobId: String): DownloadJob =
        call { service.getDownload(jobId).toDomain() }

    override suspend fun getLibraryId(): String? = try {
        call { service.getLibrary().libraryId }
    } catch (e: ApiException) {
        if (e.code == 404) null else throw e
    }

    override suspend fun listSongs(): List<Song> = call { service.listSongs().songs.map { it.toDomain() } }

    override suspend fun getSong(id: String): Song = call { service.getSong(id).toDomain() }

    override suspend fun deleteSong(id: String) {
        call { service.deleteSong(id).requireSuccess() }
    }

    override suspend fun getLyrics(songId: String): Lyrics = call { service.getLyrics(songId).toDomain() }

    override suspend fun downloadAudio(songId: String, target: File) {
        call {
            service.getAudio(songId).use { body ->
                withContext(Dispatchers.IO) {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out -> body.byteStream().copyTo(out) }
                }
            }
        }
    }

    override suspend fun listPlaylists(): List<Playlist> =
        call { service.listPlaylists().playlists.map { it.toDomain() } }

    override suspend fun createPlaylist(name: String, songIds: List<String>): Playlist =
        call { service.createPlaylist(PlaylistCreateDto(name, songIds)).toDomain() }

    override suspend fun updatePlaylist(id: String, name: String?, songIds: List<String>?): Playlist =
        call { service.updatePlaylist(id, PlaylistUpdateDto(name, songIds)).toDomain() }

    override suspend fun deletePlaylist(id: String) {
        call { service.deletePlaylist(id).requireSuccess() }
    }

    private fun Response<Unit>.requireSuccess() {
        if (!isSuccessful) throw ApiException(code(), message())
    }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: HttpException) {
        throw ApiException(e.code(), e.message())
    } catch (e: IOException) {
        throw NoInternetException(e)
    }
}
