package com.mimusica.app.ui

import com.mimusica.app.data.LibraryRepository
import com.mimusica.app.data.local.toEntity
import com.mimusica.app.domain.error.NoInternetException
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.fakes.FakePlaylistDao
import com.mimusica.app.fakes.FakeRemote
import com.mimusica.app.fakes.FakeScheduler
import com.mimusica.app.fakes.FakeSongDao
import com.mimusica.app.fakes.FakeStorage
import com.mimusica.app.fakes.FakeSyncState
import com.mimusica.app.fakes.MainDispatcherRule
import com.mimusica.app.fakes.song
import com.mimusica.app.ui.playlists.PlaylistsViewModel
import com.mimusica.app.ui.state.UiError
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaylistsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val remote = FakeRemote()
    private val songDao = FakeSongDao()
    private val playlistDao = FakePlaylistDao()
    private val library =
        LibraryRepository(remote, songDao, playlistDao, FakeScheduler(), FakeStorage(), FakeSyncState())

    private suspend fun seedPlaylist(id: String, vararg songs: String) {
        val p = Playlist(id, "List $id", songs.toList())
        remote.playlists += p
        playlistDao.upsert(listOf(p.toEntity()))
    }

    @Test
    fun `exposes playlists and downloaded songs`() = runTest {
        seedPlaylist("p1", "a")
        songDao.upsert(listOf(song("a").toEntity("/x"), song("b").toEntity(null)))
        val vm = PlaylistsViewModel(library)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertEquals(listOf("p1"), s.playlists.map { it.id })
        assertEquals(listOf("a"), s.songs.map { it.id })
    }

    @Test
    fun `create playlist trims name and saves it`() = runTest {
        val vm = PlaylistsViewModel(library)
        vm.createPlaylist("  Boleros  ")
        advanceUntilIdle()

        assertEquals("Boleros", vm.uiState.value.playlists.single().name)
        assertEquals("Boleros", remote.playlists.single().name)
    }

    @Test
    fun `blank playlist name is ignored`() = runTest {
        val vm = PlaylistsViewModel(library)
        vm.createPlaylist("   ")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.playlists.isEmpty())
        assertTrue(remote.playlists.isEmpty())
    }

    @Test
    fun `add songs appends without duplicates`() = runTest {
        seedPlaylist("p1", "a")
        val vm = PlaylistsViewModel(library)
        vm.addSongs("p1", listOf("a", "b", "c"))
        advanceUntilIdle()

        assertEquals(listOf("a", "b", "c"), vm.uiState.value.playlists.single().songIds)
        assertEquals(listOf("a", "b", "c"), remote.playlists.single().songIds)
    }

    @Test
    fun `remove song updates both sides`() = runTest {
        seedPlaylist("p1", "a", "b")
        val vm = PlaylistsViewModel(library)
        vm.removeSong("p1", "a")
        advanceUntilIdle()

        assertEquals(listOf("b"), vm.uiState.value.playlists.single().songIds)
    }

    @Test
    fun `delete playlist removes it`() = runTest {
        seedPlaylist("p1")
        val vm = PlaylistsViewModel(library)
        vm.deletePlaylist("p1")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.playlists.isEmpty())
    }

    @Test
    fun `no internet keeps data and reports friendly error until dismissed`() = runTest {
        seedPlaylist("p1", "a")
        val vm = PlaylistsViewModel(library)
        advanceUntilIdle()
        remote.failure = NoInternetException(RuntimeException())

        vm.removeSong("p1", "a")
        advanceUntilIdle()

        assertEquals(UiError.NO_INTERNET, vm.uiState.value.error)
        assertEquals(listOf("a"), vm.uiState.value.playlists.single().songIds)
        assertEquals(false, vm.uiState.value.busy)

        vm.dismissError()
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `adding a song remembers which list it went to`() = runTest {
        seedPlaylist("p1")
        val vm = PlaylistsViewModel(library)
        advanceUntilIdle()

        vm.addSongs("p1", listOf("a"))
        advanceUntilIdle()

        assertEquals("List p1", vm.uiState.value.lastAddedTo)
        vm.clearLastAdded()
        assertNull(vm.uiState.value.lastAddedTo)
    }

    @Test
    fun `a new list can start with a song and says so`() = runTest {
        val vm = PlaylistsViewModel(library)
        vm.createPlaylist("Para bailar", songIds = listOf("a"))
        advanceUntilIdle()

        assertEquals(listOf("a"), remote.playlists.single().songIds)
        assertEquals("Para bailar", vm.uiState.value.lastAddedTo)
    }

    @Test
    fun `a plain new list does not claim a song was added`() = runTest {
        val vm = PlaylistsViewModel(library)
        vm.createPlaylist("Vacía")
        advanceUntilIdle()

        assertNull(vm.uiState.value.lastAddedTo)
    }
}
