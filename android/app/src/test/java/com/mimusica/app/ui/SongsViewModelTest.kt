package com.mimusica.app.ui

import com.mimusica.app.data.LibraryRepository
import com.mimusica.app.data.local.toEntity
import com.mimusica.app.domain.error.NoInternetException
import com.mimusica.app.fakes.FakePlaylistDao
import com.mimusica.app.fakes.FakeRemote
import com.mimusica.app.fakes.FakeScheduler
import com.mimusica.app.fakes.FakeSongDao
import com.mimusica.app.fakes.FakeStorage
import com.mimusica.app.fakes.FakeSyncState
import com.mimusica.app.fakes.MainDispatcherRule
import com.mimusica.app.fakes.song
import com.mimusica.app.ui.songs.SongsViewModel
import com.mimusica.app.ui.state.UiError
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SongsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val remote = FakeRemote()
    private val songDao = FakeSongDao()
    private val library =
        LibraryRepository(remote, songDao, FakePlaylistDao(), FakeScheduler(), FakeStorage(), FakeSyncState())

    @Test
    fun `lists downloaded songs and filters singable ones`() = runTest {
        songDao.upsert(
            listOf(
                song("a", synced = true).toEntity("/x/a"),
                song("b").toEntity("/x/b"),
                song("c", synced = true).toEntity(null),
            ),
        )
        val vm = SongsViewModel(library)
        advanceUntilIdle()

        assertEquals(setOf("a", "b"), vm.uiState.value.songs.map { it.id }.toSet())
        assertEquals(listOf("a"), vm.uiState.value.singable.map { it.id })
    }

    @Test
    fun `delete failure keeps the song and reports no internet`() = runTest {
        songDao.upsert(listOf(song("a").toEntity("/x/a")))
        val vm = SongsViewModel(library)
        advanceUntilIdle()
        remote.failure = NoInternetException(RuntimeException())

        vm.delete(vm.uiState.value.songs.single())
        advanceUntilIdle()

        assertEquals(UiError.NO_INTERNET, vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.songs.size)
    }

    @Test
    fun `delete success remembers which song was deleted so the screen can say so`() = runTest {
        remote.songs += song("a", title = "Besame Mucho")
        songDao.upsert(listOf(song("a", title = "Besame Mucho").toEntity("/x/a")))
        val vm = SongsViewModel(library)
        advanceUntilIdle()

        vm.delete(vm.uiState.value.songs.single())
        advanceUntilIdle()

        assertEquals("Besame Mucho", vm.uiState.value.deletedTitle)
    }

    @Test
    fun `delete failure leaves no deleted notice`() = runTest {
        songDao.upsert(listOf(song("a").toEntity("/x/a")))
        val vm = SongsViewModel(library)
        advanceUntilIdle()
        remote.failure = NoInternetException(RuntimeException())

        vm.delete(vm.uiState.value.songs.single())
        advanceUntilIdle()

        assertEquals(null, vm.uiState.value.deletedTitle)
    }

    @Test
    fun `the deleted notice can be cleared`() = runTest {
        remote.songs += song("a")
        songDao.upsert(listOf(song("a").toEntity("/x/a")))
        val vm = SongsViewModel(library)
        advanceUntilIdle()
        vm.delete(vm.uiState.value.songs.single())
        advanceUntilIdle()

        vm.clearNotice()

        assertEquals(null, vm.uiState.value.deletedTitle)
    }

    @Test
    fun `delete success removes the song`() = runTest {
        remote.songs += song("a")
        songDao.upsert(listOf(song("a").toEntity("/x/a")))
        val vm = SongsViewModel(library)
        advanceUntilIdle()

        vm.delete(vm.uiState.value.songs.single())
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.songs.size)
    }
}
