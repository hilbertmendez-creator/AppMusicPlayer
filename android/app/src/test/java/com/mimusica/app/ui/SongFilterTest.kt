package com.mimusica.app.ui

import com.mimusica.app.fakes.song
import com.mimusica.app.ui.songs.filterSongs
import org.junit.Assert.assertEquals
import org.junit.Test

class SongFilterTest {
    private val songs = listOf(
        song("1").copy(title = "Bésame Mucho", artist = "Luis Miguel"),
        song("2").copy(title = "Volver", artist = "Carlos Gardel"),
        song("3").copy(title = "Cielito Lindo", artist = "Luis Miguel"),
    )

    private fun ids(query: String) = filterSongs(songs, query).map { it.id }

    @Test
    fun `a blank query keeps everything in order`() {
        assertEquals(listOf("1", "2", "3"), ids(""))
        assertEquals(listOf("1", "2", "3"), ids("   "))
    }

    @Test
    fun `finds by title ignoring case and accents`() {
        assertEquals(listOf("1"), ids("besame"))
        assertEquals(listOf("1"), ids("BÉSAME"))
    }

    @Test
    fun `finds by artist`() {
        assertEquals(listOf("1", "3"), ids("luis"))
    }

    @Test
    fun `every word must match, in any order`() {
        assertEquals(listOf("3"), ids("lindo luis"))
        assertEquals(emptyList<String>(), ids("volver luis"))
    }

    @Test
    fun `no match gives an empty list`() {
        assertEquals(emptyList<String>(), ids("zzz"))
    }
}
