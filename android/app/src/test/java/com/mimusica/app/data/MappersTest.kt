package com.mimusica.app.data

import com.mimusica.app.data.local.PlaylistEntity
import com.mimusica.app.data.local.toDomain
import com.mimusica.app.data.local.toEntity
import com.mimusica.app.data.remote.ApiJson
import com.mimusica.app.data.remote.DownloadJobDto
import com.mimusica.app.data.remote.LyricsDto
import com.mimusica.app.data.remote.PlaylistListDto
import com.mimusica.app.data.remote.SearchResponseDto
import com.mimusica.app.data.remote.SearchResultDto
import com.mimusica.app.data.remote.SongListDto
import com.mimusica.app.data.remote.toDomain
import com.mimusica.app.domain.model.DownloadStatus
import com.mimusica.app.domain.model.MatchType
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.fakes.song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    @Test
    fun `maps song list payload with snake_case keys`() {
        val json = """
            {"songs":[{"id":"s1","source_id":"yt1","title":"Volver","artist":"Gardel",
            "duration_seconds":180,"thumbnail_url":"https://img/x.jpg","has_synced_lyrics":true,
            "has_plain_lyrics":false,"lyrics_offset_ms":-200,"created_at":"2026-01-01T00:00:00Z","extra":1}]}
        """.trimIndent()
        val songs = ApiJson.decodeFromString<SongListDto>(json).songs.map { it.toDomain() }
        val song = songs.single()
        assertEquals("s1", song.id)
        assertEquals("yt1", song.sourceId)
        assertEquals(180, song.durationSeconds)
        assertTrue(song.hasSyncedLyrics)
        assertFalse(song.hasPlainLyrics)
        assertEquals(-200, song.lyricsOffsetMs)
        assertNull(song.audioPath)
        assertTrue(song.hasLyrics)
    }

    @Test
    fun `maps search result with lyrics match and null optionals`() {
        val json = """
            {"results":[{"source_id":"a","title":"T","artist":"A","album":null,"duration_seconds":null,
            "thumbnail_url":null,"match":"lyrics","in_library":true}]}
        """.trimIndent()
        val r = ApiJson.decodeFromString<SearchResponseDto>(json).results.single().toDomain()
        assertEquals(MatchType.LYRICS, r.match)
        assertTrue(r.inLibrary)
        assertNull(r.thumbnailUrl)
    }

    @Test
    fun `unknown match falls back to title`() {
        val dto = SearchResultDto("a", "T", "A", null, null, null, "weird", false)
        assertEquals(MatchType.TITLE, dto.toDomain().match)
    }

    @Test
    fun `maps every download status`() {
        fun status(s: String) = DownloadJobDto("j", "src", s, null, null, "now").toDomain().status
        assertEquals(DownloadStatus.PENDING, status("pending"))
        assertEquals(DownloadStatus.DOWNLOADING, status("downloading"))
        assertEquals(DownloadStatus.READY, status("ready"))
        assertEquals(DownloadStatus.FAILED, status("failed"))
    }

    @Test
    fun `maps lyrics with nulls`() {
        val l = LyricsDto(syncedLrc = null, plain = "la la", offsetMs = 120).toDomain()
        assertNull(l.syncedLrc)
        assertEquals("la la", l.plain)
        assertEquals(120, l.offsetMs)
    }

    @Test
    fun `maps playlists`() {
        val json = """{"playlists":[{"id":"p","name":"Tangos","song_ids":["a","b"],"updated_at":"x"}]}"""
        val p = ApiJson.decodeFromString<PlaylistListDto>(json).playlists.single().toDomain()
        assertEquals(Playlist("p", "Tangos", listOf("a", "b")), p)
    }

    @Test
    fun `song entity round trip keeps audio path`() {
        val song = song("s1", audioPath = "/data/s1.m4a")
        val entity = song.toEntity(audioPath = "/data/s1.m4a")
        assertEquals(song, entity.toDomain())
    }

    @Test
    fun `playlist entity round trip preserves order and empties`() {
        val p = Playlist("p", "Mi lista", listOf("z", "a", "m"))
        assertEquals(p, p.toEntity().toDomain())
        val empty = Playlist("e", "Vacia", emptyList())
        assertEquals(empty, empty.toEntity().toDomain())
        assertEquals(emptyList<String>(), PlaylistEntity("e", "x", "").toDomain().songIds)
    }
}
