package com.mimusica.app.ui

import com.mimusica.app.data.LibraryRepository
import com.mimusica.app.data.local.toEntity
import com.mimusica.app.domain.error.NoInternetException
import com.mimusica.app.domain.model.DownloadJob
import com.mimusica.app.domain.model.DownloadState
import com.mimusica.app.domain.model.DownloadStatus
import com.mimusica.app.domain.model.MatchType
import com.mimusica.app.domain.model.SearchResult
import com.mimusica.app.fakes.FakePlaylistDao
import com.mimusica.app.fakes.FakeRemote
import com.mimusica.app.fakes.FakeScheduler
import com.mimusica.app.fakes.FakeSongDao
import com.mimusica.app.fakes.FakeStorage
import com.mimusica.app.fakes.FakeSyncState
import com.mimusica.app.fakes.MainDispatcherRule
import com.mimusica.app.fakes.song
import com.mimusica.app.ui.search.SearchViewModel
import com.mimusica.app.ui.state.UiError
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val remote = FakeRemote()
    private val scheduler = FakeScheduler()
    private val songDao = FakeSongDao()
    private val library = LibraryRepository(remote, songDao, FakePlaylistDao(), scheduler, FakeStorage(), FakeSyncState())
    // Lazy: the ViewModel launches in init, which needs the Main dispatcher the rule installs after construction.
    private val vm by lazy { SearchViewModel(remote, library, pollDelayMs = 1_000, maxPolls = 5) }

    private fun result(id: String, inLibrary: Boolean = false, match: MatchType = MatchType.TITLE) =
        SearchResult(id, "Title $id", "Artist", null, 100, null, match, inLibrary)

    private fun job(status: DownloadStatus, songId: String? = null) =
        DownloadJob("job1", "s1", status, songId, null)

    @Test
    fun `starts idle with empty query`() {
        val s = vm.uiState.value
        assertEquals("", s.query)
        assertFalse(s.isSearching)
        assertFalse(s.hasSearched)
        assertTrue(s.items.isEmpty())
        assertNull(s.error)
    }

    @Test
    fun `search shows results with download state from library flag`() = runTest {
        remote.searchResults = listOf(result("s1"), result("s2", inLibrary = true, match = MatchType.LYRICS))
        vm.onQueryChange("  volver  ")

        vm.search()
        assertTrue(vm.uiState.value.isSearching)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertFalse(s.isSearching)
        assertTrue(s.hasSearched)
        assertEquals(listOf("volver"), remote.searchedQueries)
        assertEquals(DownloadState.NotDownloaded, s.items[0].state)
        assertEquals(DownloadState.InLibrary, s.items[1].state)
        assertEquals(MatchType.LYRICS, s.items[1].result.match)
    }

    @Test
    fun `query never keeps line breaks`() = runTest {
        vm.onQueryChange("volver\n")
        assertEquals("volver", vm.uiState.value.query)
        vm.onQueryChange("el rey\r\nde la noche")
        assertEquals("el rey de la noche", vm.uiState.value.query)
        vm.onVoiceResult("cielito\nlindo")
        assertEquals("cielito lindo", vm.uiState.value.query)
    }

    @Test
    fun `at most five results are shown`() = runTest {
        remote.searchResults = (1..9).map { result("s$it") }
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()
        assertEquals(5, vm.uiState.value.items.size)
    }

    @Test
    fun `blank query does not hit the server`() = runTest {
        vm.onQueryChange("   ")
        vm.search()
        advanceUntilIdle()
        assertTrue(remote.searchedQueries.isEmpty())
        assertFalse(vm.uiState.value.hasSearched)
    }

    @Test
    fun `no results marks searched with empty list`() = runTest {
        vm.onQueryChange("zzz")
        vm.search()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasSearched)
        assertTrue(vm.uiState.value.items.isEmpty())
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `no internet during search surfaces friendly error`() = runTest {
        remote.failure = NoInternetException(RuntimeException())
        vm.onQueryChange("volver")
        vm.search()
        advanceUntilIdle()
        assertEquals(UiError.NO_INTERNET, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun `voice result fills the query and searches`() = runTest {
        remote.searchResults = listOf(result("s1"))
        vm.onVoiceResult("la barca")
        advanceUntilIdle()
        assertEquals("la barca", vm.uiState.value.query)
        assertEquals(listOf("la barca"), remote.searchedQueries)
    }

    @Test
    fun `download polls the job until ready then adds song to library`() = runTest {
        remote.songs += song("lib1")
        remote.searchResults = listOf(result("s1"))
        remote.jobs.addAll(
            listOf(job(DownloadStatus.PENDING), job(DownloadStatus.DOWNLOADING), job(DownloadStatus.READY, "lib1")),
        )
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()

        vm.download("s1")
        assertEquals(DownloadState.Downloading, vm.uiState.value.items[0].state)
        advanceUntilIdle()

        // The server has it; the phone is still saving the file.
        assertEquals(DownloadState.Saving, vm.uiState.value.items[0].state)
        assertEquals("lib1", vm.uiState.value.items[0].songId)
        assertEquals(listOf("lib1"), scheduler.enqueued)
    }

    @Test
    fun `saving becomes ready and playable once the file is on the phone`() = runTest {
        remote.songs += song("lib1")
        remote.searchResults = listOf(result("s1"))
        remote.jobs.add(job(DownloadStatus.READY, "lib1"))
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()
        vm.download("s1")
        advanceUntilIdle()
        assertEquals(DownloadState.Saving, vm.uiState.value.items[0].state)

        songDao.setAudioPath("lib1", "/songs/lib1.m4a")
        advanceUntilIdle()

        val item = vm.uiState.value.items[0]
        assertEquals(DownloadState.InLibrary, item.state)
        assertEquals("lib1", item.song?.id)
    }

    @Test
    fun `a result already on the phone is playable right away`() = runTest {
        songDao.upsert(listOf(song("lib9").toEntity("/songs/lib9.m4a")))
        remote.searchResults = listOf(result("src-lib9", inLibrary = true))
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()

        val item = vm.uiState.value.items[0]
        assertEquals(DownloadState.InLibrary, item.state)
        assertEquals("lib9", item.song?.id)
    }

    @Test
    fun `songs already on the phone come first and the rest keep their order`() = runTest {
        songDao.upsert(listOf(song("lib9").toEntity("/songs/lib9.m4a")))
        remote.searchResults = listOf(result("s1"), result("src-lib9", inLibrary = true), result("s3"))
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()

        assertEquals(
            listOf("src-lib9", "s1", "s3"),
            vm.uiState.value.items.map { it.result.sourceId },
        )
    }

    @Test
    fun `a download that finishes later does not reshuffle the cards`() = runTest {
        remote.songs += song("lib1")
        remote.searchResults = listOf(result("s1"), result("s2"))
        remote.jobs.add(job(DownloadStatus.READY, "lib1"))
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()

        vm.download("s2")
        advanceUntilIdle()
        songDao.setAudioPath("lib1", "/songs/lib1.m4a")
        advanceUntilIdle()

        assertEquals(listOf("s1", "s2"), vm.uiState.value.items.map { it.result.sourceId })
    }

    @Test
    fun `a failed job has no specific reason`() = runTest {
        remote.searchResults = listOf(result("s1"))
        remote.jobs.add(job(DownloadStatus.FAILED))
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()
        vm.download("s1")
        advanceUntilIdle()

        assertNull(vm.uiState.value.items[0].failure)
    }

    @Test
    fun `no internet is remembered as the reason on that card`() = runTest {
        remote.searchResults = listOf(result("s1"))
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()
        remote.failure = NoInternetException(RuntimeException())
        vm.download("s1")
        advanceUntilIdle()

        assertEquals(UiError.NO_INTERNET, vm.uiState.value.items[0].failure)
    }

    @Test
    fun `retrying clears the old failure`() = runTest {
        remote.searchResults = listOf(result("s1"))
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()
        remote.failure = NoInternetException(RuntimeException())
        vm.download("s1")
        advanceUntilIdle()
        remote.failure = null
        remote.jobs.add(job(DownloadStatus.PENDING))

        vm.download("s1")

        assertEquals(DownloadState.Downloading, vm.uiState.value.items[0].state)
        assertNull(vm.uiState.value.items[0].failure)
    }

    @Test
    fun `failed job marks the result as failed`() = runTest {
        remote.searchResults = listOf(result("s1"))
        remote.jobs.add(job(DownloadStatus.FAILED))
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()

        vm.download("s1")
        advanceUntilIdle()

        assertEquals(DownloadState.Failed, vm.uiState.value.items[0].state)
        assertTrue(scheduler.enqueued.isEmpty())
    }

    @Test
    fun `polling gives up after max attempts`() = runTest {
        remote.searchResults = listOf(result("s1"))
        remote.jobs.addAll(listOf(job(DownloadStatus.PENDING), job(DownloadStatus.PENDING)))
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()

        vm.download("s1")
        advanceUntilIdle()

        assertEquals(DownloadState.Failed, vm.uiState.value.items[0].state)
    }

    @Test
    fun `no internet while downloading marks failed and sets error`() = runTest {
        remote.searchResults = listOf(result("s1"))
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()
        remote.failure = NoInternetException(RuntimeException())

        vm.download("s1")
        advanceUntilIdle()

        assertEquals(DownloadState.Failed, vm.uiState.value.items[0].state)
        assertEquals(UiError.NO_INTERNET, vm.uiState.value.error)
    }

    @Test
    fun `downloading an item already in library is a no-op`() = runTest {
        remote.searchResults = listOf(result("s1", inLibrary = true))
        vm.onQueryChange("x")
        vm.search()
        advanceUntilIdle()

        vm.download("s1")
        advanceUntilIdle()

        assertEquals(DownloadState.InLibrary, vm.uiState.value.items[0].state)
        assertTrue(remote.jobs.isEmpty())
    }
}
