package com.mimusica.app.ui.songs

import com.mimusica.app.domain.model.Song
import java.text.Normalizer

private val Accents = Regex("\\p{Mn}+")

/** Lower case and no accents, so "Bésame" is found by "besame" (and the other way round). */
private fun String.forSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Accents, "").lowercase()

/**
 * The songs whose title or artist contain every word typed in [query] (any order, accents and case ignored).
 * A blank query keeps the whole list in its order.
 */
fun filterSongs(songs: List<Song>, query: String): List<Song> {
    val words = query.forSearch().split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return songs
    return songs.filter { song ->
        val haystack = "${song.title} ${song.artist}".forSearch()
        words.all { it in haystack }
    }
}
