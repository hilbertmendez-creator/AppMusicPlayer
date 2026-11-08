package com.mimusica.app.data

import com.mimusica.app.data.local.LyricsEntity
import com.mimusica.app.data.local.toEntity
import com.mimusica.app.domain.error.NoInternetException
import com.mimusica.app.domain.model.Lyrics
import com.mimusica.app.domain.model.Playlist
import com.mimusica.app.fakes.FakePlaylistDao
import com.mimusica.app.fakes.FakeRemote
import com.mimusica.app.fakes.FakeScheduler
import com.mimusica.app.fakes.FakeSongDao
import com.mimusica.app.fakes.FakeStorage
import com.mimusica.app.fakes.FakeSyncState
import com.mimusica.app.fakes.song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LibraryRepositoryTest {
    private val remote = FakeRemote()
    private val songDao = FakeSongDao()
    private val playlistDao = FakePlaylistDao()
    private val scheduler = FakeScheduler()
    private val storage = FakeStorage()
    // By default the phone already knows this server, so legitimate deletions are trusted.
    private val syncState = FakeSyncState("lib-1")
    private val repo = LibraryRepository(remote, songDao, playlistDao, scheduler, storage, syncState)

    private suspend fun localDownloaded(id: String, offset: Int = 0, synced: Boolean = false) {
        songDao.upsert(listOf(song(id, offset = offset, synced = synced).toEntity("/fake/$id.m4a")))
        storage.files += id
    }

    @Test
    fun `sync enqueues download for songs missing on device`() = runTest {
        remote.songs += listOf(song("a"), song("b"))
        localDownloaded("a")

        val report = repo.sync()

        assertEquals(listOf("b"), scheduler.enqueued)
        assertEquals(1, report.queuedDownloads)
        assertEquals(setOf("a", "b"), songDao.getAll().map { it.id }.toSet())
    }

    @Test
    fun `sync keeps local audio path of already downloaded songs`() = runTest {
        remote.songs += song("a", title = "New title")
        localDownloaded("a")

        repo.sync()

        val entity = songDao.getById("a")!!
        assertEquals("New title", entity.title)
        assertEquals("/fake/a.m4a", entity.audioPath)
        assertTrue(scheduler.enqueued.isEmpty())
    }

    @Test
    fun `sync redownloads when file disappeared from storage`() = runTest {
        remote.songs += song("a")
        songDao.upsert(listOf(song("a").toEntity("/fake/a.m4a")))

        repo.sync()

        assertEquals(listOf("a"), scheduler.enqueued)
        assertNull(songDao.getById("a")!!.audioPath)
    }

    @Test
    fun `sync removes songs deleted on the server including files and lyrics`() = runTest {
        remote.songs += song("a")
        localDownloaded("a")
        localDownloaded("gone")
        songDao.upsertLyrics(LyricsEntity("gone", null, "x", 0))

        val report = repo.sync()

        assertNull(songDao.getById("gone"))
        assertNull(songDao.getLyrics("gone"))
        assertEquals(listOf("gone"), storage.deleted)
        assertEquals(1, report.removedSongs)
    }

    @Test
    fun `sync mirrors playlists adding updating and deleting`() = runTest {
        playlistDao.upsert(
            listOf(Playlist("old", "Old", listOf("a")).toEntity(), Playlist("keep", "Before", listOf("a")).toEntity()),
        )
        remote.playlists += Playlist("keep", "After", listOf("a", "b"))
        remote.playlists += Playlist("new", "New", emptyList())

        repo.sync()

        val result = repo.observePlaylists().first().associateBy { it.id }
        assertEquals(setOf("keep", "new"), result.keys)
        assertEquals("After", result["keep"]!!.name)
        assertEquals(listOf("a", "b"), result["keep"]!!.songIds)
    }

    @Test
    fun `sync refreshes lyrics of downloaded song when offset changed on server`() = runTest {
        localDownloaded("a", offset = 0, synced = true)
        songDao.upsertLyrics(LyricsEntity("a", "[00:01.00]hi", null, 0))
        remote.songs += song("a", offset = 300, synced = true)
        remote.lyricsBySong["a"] = Lyrics("[00:01.00]hi", null, 300)

        repo.sync()

        assertEquals(300, songDao.getLyrics("a")!!.offsetMs)
    }

    @Test
    fun `sync failure leaves local data untouched`() = runTest {
        localDownloaded("a")
        remote.failure = NoInternetException(RuntimeException("offline"))

        try {
            repo.sync()
            fail("expected failure")
        } catch (e: NoInternetException) {
            // expected
        }

        assertNotNull(songDao.getById("a"))
        assertTrue(storage.deleted.isEmpty())
    }

    @Test
    fun `sync with a different library id deletes nothing and merges remote additions`() = runTest {
        remote.libraryId = "lib-2"
        remote.songs += song("new")
        remote.playlists += Playlist("np", "New list", listOf("new"))
        localDownloaded("mine")
        songDao.upsertLyrics(LyricsEntity("mine", null, "x", 0))
        playlistDao.upsert(listOf(Playlist("mine-pl", "Mine", listOf("mine")).toEntity()))

        val report = repo.sync()

        assertTrue(report.serverMismatch)
        assertEquals(0, report.removedSongs)
        assertNotNull(songDao.getById("mine"))
        assertNotNull(songDao.getLyrics("mine"))
        assertNotNull(songDao.getById("new"))
        assertTrue(storage.deleted.isEmpty())
        assertEquals(listOf("new"), scheduler.enqueued)
        assertEquals(setOf("mine-pl", "np"), repo.observePlaylists().first().map { it.id }.toSet())
        assertEquals("lib-1", syncState.id)
    }

    @Test
    fun `sync with the same library id still removes songs deleted on the server`() = runTest {
        remote.songs += song("a")
        localDownloaded("a")
        localDownloaded("gone")

        val report = repo.sync()

        assertFalse(report.serverMismatch)
        assertEquals(1, report.removedSongs)
        assertNull(songDao.getById("gone"))
    }

    @Test
    fun `first ever sync with local data adopts the id but deletes nothing`() = runTest {
        syncState.id = null
        remote.songs += song("a")
        localDownloaded("a")
        localDownloaded("other")
        playlistDao.upsert(listOf(Playlist("p", "P", emptyList()).toEntity()))

        val report = repo.sync()

        assertEquals("lib-1", syncState.id)
        assertTrue(report.serverMismatch)
        assertEquals(0, report.removedSongs)
        assertNotNull(songDao.getById("other"))
        assertNotNull(playlistDao.getById("p"))
        assertTrue(storage.deleted.isEmpty())
    }

    @Test
    fun `first ever sync on an empty phone adopts the id and proceeds`() = runTest {
        syncState.id = null
        remote.songs += song("a")

        val report = repo.sync()

        assertEquals("lib-1", syncState.id)
        assertFalse(report.serverMismatch)
        assertEquals(listOf("a"), scheduler.enqueued)
        assertNotNull(songDao.getById("a"))
    }

    @Test
    fun `second sync after adopting the id is trusted`() = runTest {
        syncState.id = null
        remote.songs += song("a")
        localDownloaded("a")
        localDownloaded("gone")
        repo.sync()
        assertNotNull(songDao.getById("gone"))

        val report = repo.sync()

        assertEquals(1, report.removedSongs)
        assertNull(songDao.getById("gone"))
    }

    @Test
    fun `server without library id endpoint is untrusted`() = runTest {
        remote.libraryId = null
        remote.songs += song("a")
        localDownloaded("a")
        localDownloaded("other")
        playlistDao.upsert(listOf(Playlist("p", "P", emptyList()).toEntity()))

        val report = repo.sync()

        assertTrue(report.serverMismatch)
        assertNotNull(songDao.getById("other"))
        assertNotNull(playlistDao.getById("p"))
        assertEquals("lib-1", syncState.id)
    }

    @Test
    fun `trusted server returning zero songs does not wipe downloaded songs`() = runTest {
        localDownloaded("a")
        localDownloaded("b")

        val report = repo.sync()

        assertTrue(report.serverMismatch)
        assertEquals(0, report.removedSongs)
        assertNotNull(songDao.getById("a"))
        assertNotNull(songDao.getById("b"))
        assertTrue(storage.deleted.isEmpty())
    }

    @Test
    fun `failure fetching the library id leaves local data untouched`() = runTest {
        localDownloaded("a")
        playlistDao.upsert(listOf(Playlist("p", "P", emptyList()).toEntity()))
        remote.failure = NoInternetException(RuntimeException("offline"))
        syncState.id = null

        try {
            repo.sync()
            fail("expected failure")
        } catch (e: NoInternetException) {
            // expected
        }

        assertNotNull(songDao.getById("a"))
        assertNotNull(playlistDao.getById("p"))
        assertNull(syncState.id)
        assertTrue(storage.deleted.isEmpty())
    }

    @Test
    fun `observeDownloadedSongs only emits songs with audio`() = runTest {
        localDownloaded("a")
        songDao.upsert(listOf(song("pending").toEntity(null)))

        assertEquals(listOf("a"), repo.observeDownloadedSongs().first().map { it.id })
    }

    @Test
    fun `addDownloadedSong stores metadata and enqueues the file download`() = runTest {
        remote.songs += song("z")

        repo.addDownloadedSong("z")

        assertNotNull(songDao.getById("z"))
        assertEquals(listOf("z"), scheduler.enqueued)
    }

    @Test
    fun `deleteSong deletes remotely then locally and cleans playlists`() = runTest {
        remote.songs += song("a")
        localDownloaded("a")
        playlistDao.upsert(listOf(Playlist("p", "P", listOf("a", "b")).toEntity()))

        repo.deleteSong("a")

        assertEquals(listOf("a"), remote.deletedSongs)
        assertNull(songDao.getById("a"))
        assertEquals(listOf("a"), storage.deleted)
        assertEquals(listOf("b"), repo.observePlaylists().first().single().songIds)
    }

    @Test
    fun `deleteSong keeps local copy when server call fails`() = runTest {
        localDownloaded("a")
        remote.failure = NoInternetException(RuntimeException())

        try {
            repo.deleteSong("a")
            fail("expected failure")
        } catch (e: NoInternetException) {
            // expected
        }

        assertNotNull(songDao.getById("a"))
    }

    @Test
    fun `playlist create update delete go to server and local`() = runTest {
        val created = repo.createPlaylist("Boleros", listOf("a"))
        assertEquals(listOf(created), repo.observePlaylists().first())

        val updated = repo.updatePlaylist(created.id, name = null, songIds = listOf("a", "b"))
        assertEquals(listOf("a", "b"), updated.songIds)
        assertEquals(listOf("a", "b"), repo.observePlaylists().first().single().songIds)

        repo.deletePlaylist(created.id)
        assertTrue(repo.observePlaylists().first().isEmpty())
        assertTrue(remote.playlists.isEmpty())
    }

    @Test
    fun `getLyrics returns stored lyrics or null`() = runTest {
        assertNull(repo.getLyrics("a"))
        songDao.upsertLyrics(LyricsEntity("a", "[00:01.00]x", "x", 50))
        assertEquals(Lyrics("[00:01.00]x", "x", 50), repo.getLyrics("a"))
    }
}
