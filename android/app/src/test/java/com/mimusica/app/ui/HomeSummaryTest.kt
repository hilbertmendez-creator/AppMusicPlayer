package com.mimusica.app.ui

import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.domain.model.Song
import com.mimusica.app.ui.home.HomeSummary
import com.mimusica.app.ui.home.homeSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeSummaryTest {
    private fun song(id: String, synced: Boolean = false, plain: Boolean = false) = Song(
        id = id, sourceId = "s$id", title = "T$id", artist = "A", durationSeconds = 100, thumbnailUrl = null,
        hasSyncedLyrics = synced, hasPlainLyrics = plain, lyricsOffsetMs = 0, audioPath = "/a.m4a",
    )

    @Test
    fun `empty library has nothing to count`() {
        assertEquals(HomeSummary(songs = 0, playlists = 0, singable = 0), homeSummary(emptyList(), emptyList()))
    }

    @Test
    fun `singable counts songs with synced or plain lyrics only`() {
        val songs = listOf(song("1", synced = true), song("2", plain = true), song("3"))
        val summary = homeSummary(songs, listOf(Playlist("p", "Mix", emptyList())))
        assertEquals(HomeSummary(songs = 3, playlists = 1, singable = 2), summary)
    }
}
