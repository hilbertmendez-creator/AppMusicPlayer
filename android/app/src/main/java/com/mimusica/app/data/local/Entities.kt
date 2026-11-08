package com.mimusica.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int?,
    val thumbnailUrl: String?,
    val hasSyncedLyrics: Boolean,
    val hasPlainLyrics: Boolean,
    val lyricsOffsetMs: Int,
    /** Absolute path of the downloaded audio file; null while the file is not on the device. */
    val audioPath: String?,
)

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val songId: String,
    val syncedLrc: String?,
    val plain: String?,
    val offsetMs: Int,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Ordered song ids joined by [SONG_ID_SEPARATOR]. */
    val songIds: String,
)

const val SONG_ID_SEPARATOR = ","

fun SongEntity.toDomain() = Song(
    id = id,
    sourceId = sourceId,
    title = title,
    artist = artist,
    durationSeconds = durationSeconds,
    thumbnailUrl = thumbnailUrl,
    hasSyncedLyrics = hasSyncedLyrics,
    hasPlainLyrics = hasPlainLyrics,
    lyricsOffsetMs = lyricsOffsetMs,
    audioPath = audioPath,
)

fun Song.toEntity(audioPath: String? = this.audioPath) = SongEntity(
    id = id,
    sourceId = sourceId,
    title = title,
    artist = artist,
    durationSeconds = durationSeconds,
    thumbnailUrl = thumbnailUrl,
    hasSyncedLyrics = hasSyncedLyrics,
    hasPlainLyrics = hasPlainLyrics,
    lyricsOffsetMs = lyricsOffsetMs,
    audioPath = audioPath,
)

fun PlaylistEntity.toDomain() = Playlist(
    id = id,
    name = name,
    songIds = if (songIds.isEmpty()) emptyList() else songIds.split(SONG_ID_SEPARATOR),
)

fun Playlist.toEntity() = PlaylistEntity(
    id = id,
    name = name,
    songIds = songIds.joinToString(SONG_ID_SEPARATOR),
)
