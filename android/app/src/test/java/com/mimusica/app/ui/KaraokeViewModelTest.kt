package com.mimusica.app.ui

import com.mimusica.app.data.LibraryRepository
import com.mimusica.app.data.local.LyricsEntity
import com.mimusica.app.data.local.toEntity
import com.mimusica.app.fakes.FakePlayer
import com.mimusica.app.fakes.FakePlaylistDao
import com.mimusica.app.fakes.FakeRemote
import com.mimusica.app.fakes.FakeScheduler
import com.mimusica.app.fakes.FakeSongDao
import com.mimusica.app.fakes.FakeStorage
import com.mimusica.app.fakes.FakeSyncState
import com.mimusica.app.fakes.MainDispatcherRule
import com.mimusica.app.fakes.song
import com.mimusica.app.ui.karaoke.KaraokeMode
import com.mimusica.app.ui.karaoke.KaraokeViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class KaraokeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val songDao = FakeSongDao()
    private val player = FakePlayer()
    private val library =
        LibraryRepository(FakeRemote(), songDao, FakePlaylistDao(), FakeScheduler(), FakeStorage(), FakeSyncState())

    private val lrc = "[offset:0]\n[00:01.00]one\n[00:05.00]two\n[00:09.00]three"

    private suspend fun seed(id: String, synced: String?, plain: String?, offset: Int = 0) {
        songDao.upsert(
            listOf(song(id, synced = synced != null, plain = plain != null).toEntity("/fake/$id.m4a")),
        )
        songDao.upsertLyrics(LyricsEntity(id, synced, plain, offset))
    }

    @Test
    fun `starts loading then shows synced lyrics`() = runTest {
        seed("a", lrc, null)
        val vm = KaraokeViewModel("a", library, player)
        assertEquals(KaraokeMode.LOADING, vm.uiState.value.mode)

        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(KaraokeMode.SYNCED, s.mode)
        assertEquals(listOf("one", "two", "three"), s.lines.map { it.text })
        assertEquals(-1, s.currentIndex)
    }

    @Test
    fun `current line follows the player position`() = runTest {
        seed("a", lrc, null)
        val vm = KaraokeViewModel("a", library, player)
        advanceUntilIdle()

        player.setPosition(5_100)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.currentIndex)

        player.setPosition(9_500)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.currentIndex)
    }

    @Test
    fun `server offset and lrc offset tag are both applied`() = runTest {
        seed("a", "[offset:200]\n[00:05.00]x\n[00:09.00]y", null, offset = 300)
        val vm = KaraokeViewModel("a", library, player)
        advanceUntilIdle()

        player.setPosition(4_499)
        advanceUntilIdle()
        assertEquals(-1, vm.uiState.value.currentIndex)

        player.setPosition(4_500)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.currentIndex)
    }

    @Test
    fun `plain lyrics only shows static text`() = runTest {
        seed("a", null, "la la la")
        val vm = KaraokeViewModel("a", library, player)
        advanceUntilIdle()

        assertEquals(KaraokeMode.PLAIN, vm.uiState.value.mode)
        assertEquals("la la la", vm.uiState.value.plain)
    }

    @Test
    fun `progress follows the playback fraction`() = runTest {
        seed("a", null, "la la la")
        val vm = KaraokeViewModel("a", library, player)
        advanceUntilIdle()
        assertEquals(0f, vm.uiState.value.progress, 0f)

        player.setDuration(200_000)
        player.setPosition(50_000)
        advanceUntilIdle()

        assertEquals(0.25f, vm.uiState.value.progress, 0.0001f)
    }

    @Test
    fun `restart seeks to the beginning`() = runTest {
        seed("a", lrc, null)
        val vm = KaraokeViewModel("a", library, player)
        advanceUntilIdle()
        player.setPosition(7_000)

        vm.restart()
        advanceUntilIdle()

        assertEquals(listOf(0L), player.seeks)
        assertEquals(-1, vm.uiState.value.currentIndex)
    }

    @Test
    fun `no lyrics shows friendly empty mode`() = runTest {
        seed("a", null, null)
        val vm = KaraokeViewModel("a", library, player)
        advanceUntilIdle()

        assertEquals(KaraokeMode.NONE, vm.uiState.value.mode)
    }

    @Test
    fun `unparseable synced lyrics fall back to plain`() = runTest {
        seed("a", "garbage only", "fallback text")
        val vm = KaraokeViewModel("a", library, player)
        advanceUntilIdle()

        assertEquals(KaraokeMode.PLAIN, vm.uiState.value.mode)
    }

    @Test
    fun `starts playback when another song is current`() = runTest {
        seed("a", lrc, null)
        KaraokeViewModel("a", library, player)
        advanceUntilIdle()

        assertEquals(1, player.played.size)
        assertEquals("a", player.state.value.currentSong?.id)
    }

    @Test
    fun `does not restart playback when same song already playing`() = runTest {
        seed("a", lrc, null)
        player.play(listOf(library.getSong("a")!!), 0)
        player.played.clear()

        KaraokeViewModel("a", library, player)
        advanceUntilIdle()

        assertTrue(player.played.isEmpty())
    }

    @Test
    fun `toggle play pause delegates to player`() = runTest {
        seed("a", lrc, null)
        val vm = KaraokeViewModel("a", library, player)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isPlaying)

        vm.togglePlayPause()
        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.isPlaying)
    }
}
