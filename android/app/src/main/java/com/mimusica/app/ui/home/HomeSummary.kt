package com.mimusica.app.ui.home

import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.Song

/** Live numbers behind the Home tiles' one-line subtitles. */
data class HomeSummary(val songs: Int, val playlists: Int, val singable: Int)

/** Pure: counts downloaded songs, lists and the songs that have lyrics (so "Cantar" is not an empty promise). */
fun homeSummary(songs: List<Song>, playlists: List<Playlist>): HomeSummary =
    HomeSummary(songs = songs.size, playlists = playlists.size, singable = songs.count { it.hasLyrics })
