package com.mimusica.app.data.remote

import com.mimusica.app.domain.model.DownloadJob
import com.mimusica.app.domain.model.DownloadStatus
import com.mimusica.app.domain.model.Lyrics
import com.mimusica.app.domain.model.MatchType
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.SearchResult
import com.mimusica.app.domain.model.Song
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = false
}

@Serializable
data class SearchResultDto(
    @SerialName("source_id") val sourceId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val match: String = "title",
    @SerialName("in_library") val inLibrary: Boolean = false,
)

@Serializable
data class SearchResponseDto(val results: List<SearchResultDto> = emptyList())

@Serializable
data class DownloadRequestDto(
    @SerialName("source_id") val sourceId: String,
    val title: String? = null,
    val artist: String? = null,
)

@Serializable
data class DownloadJobDto(
    val id: String,
    @SerialName("source_id") val sourceId: String,
    val status: String,
    @SerialName("song_id") val songId: String? = null,
    val error: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class SongDto(
    val id: String,
    @SerialName("source_id") val sourceId: String,
    val title: String,
    val artist: String,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("has_synced_lyrics") val hasSyncedLyrics: Boolean = false,
    @SerialName("has_plain_lyrics") val hasPlainLyrics: Boolean = false,
    @SerialName("lyrics_offset_ms") val lyricsOffsetMs: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class SongListDto(val songs: List<SongDto> = emptyList())

@Serializable
data class LyricsDto(
    @SerialName("synced_lrc") val syncedLrc: String? = null,
    val plain: String? = null,
    @SerialName("offset_ms") val offsetMs: Int = 0,
)

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    @SerialName("song_ids") val songIds: List<String> = emptyList(),
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class LibraryIdentityDto(@SerialName("library_id") val libraryId: String)

@Serializable
data class PlaylistListDto(val playlists: List<PlaylistDto> = emptyList())

@Serializable
data class PlaylistCreateDto(
    val name: String,
    @SerialName("song_ids") val songIds: List<String> = emptyList(),
)

@Serializable
data class PlaylistUpdateDto(
    val name: String? = null,
    @SerialName("song_ids") val songIds: List<String>? = null,
)

fun SearchResultDto.toDomain() = SearchResult(
    sourceId = sourceId,
    title = title,
    artist = artist,
    album = album,
    durationSeconds = durationSeconds,
    thumbnailUrl = thumbnailUrl,
    match = if (match.equals("lyrics", ignoreCase = true)) MatchType.LYRICS else MatchType.TITLE,
    inLibrary = inLibrary,
)

fun DownloadJobDto.toDomain() = DownloadJob(
    id = id,
    sourceId = sourceId,
    status = when (status.lowercase()) {
        "downloading" -> DownloadStatus.DOWNLOADING
        "ready" -> DownloadStatus.READY
        "failed" -> DownloadStatus.FAILED
        else -> DownloadStatus.PENDING
    },
    songId = songId,
    error = error,
)

fun SongDto.toDomain() = Song(
    id = id,
    sourceId = sourceId,
    title = title,
    artist = artist,
    durationSeconds = durationSeconds,
    thumbnailUrl = thumbnailUrl,
    hasSyncedLyrics = hasSyncedLyrics,
    hasPlainLyrics = hasPlainLyrics,
    lyricsOffsetMs = lyricsOffsetMs,
    audioPath = null,
)

fun LyricsDto.toDomain() = Lyrics(syncedLrc = syncedLrc, plain = plain, offsetMs = offsetMs)

fun PlaylistDto.toDomain() = Playlist(id = id, name = name, songIds = songIds)
