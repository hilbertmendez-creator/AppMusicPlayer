package com.mimusica.app.ui.home

import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.Song

/**
 * Live numbers behind the Home rows' subtitles, plus the song the "Seguir cantando" hero opens
 * ([continueSong] is null when no song has lyrics).
 */
data class HomeSummary(val songs: Int, val playlists: Int, val singable: Int, val continueSong: Song? = null)

/**
 * Which song "Seguir cantando" opens: the loaded one if it has lyrics, else the last one sung (if it still exists
 * and can be sung), else the first singable song, else null.
 */
fun continueSingingSong(currentSongId: String?, lastSungSongId: String?, singable: List<Song>): Song? =
    singable.firstOrNull { it.id == currentSongId }
        ?: singable.firstOrNull { it.id == lastSungSongId }
        ?: singable.firstOrNull()

/** Pure: counts downloaded songs and lists, and picks the hero song among the songs that have lyrics. */
fun homeSummary(
    songs: List<Song>,
    playlists: List<Playlist>,
    currentSongId: String? = null,
    lastSungSongId: String? = null,
): HomeSummary {
    val singable = songs.filter { it.hasLyrics }
    return HomeSummary(
        songs = songs.size,
        playlists = playlists.size,
        singable = singable.size,
        continueSong = continueSingingSong(currentSongId, lastSungSongId, singable),
    )
}
