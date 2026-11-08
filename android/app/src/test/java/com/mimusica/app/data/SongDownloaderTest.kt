package com.mimusica.app.data

import com.mimusica.app.domain.model.Lyrics
import com.mimusica.app.fakes.FakeRemote
import com.mimusica.app.fakes.FakeSongDao
import com.mimusica.app.fakes.song
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SongDownloaderTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private class TempStorage(private val dir: File) : AudioStorage {
        override fun fileFor(songId: String) = File(dir, "$songId.m4a")
        override fun exists(songId: String) = fileFor(songId).exists()
        override fun delete(songId: String) {
            fileFor(songId).delete()
        }
    }

    @Test
    fun `downloads audio and lyrics then marks the song available`() = runTest {
        val remote = FakeRemote().apply {
            songs += song("a", synced = true)
            lyricsBySong["a"] = Lyrics("[00:01.00]hola", null, 10)
        }
        val dao = FakeSongDao()
        val storage = TempStorage(tmp.newFolder())
        val downloader = SongDownloader(remote, dao, storage)

        downloader.download("a")

        assertTrue(storage.exists("a"))
        assertEquals(storage.fileFor("a").absolutePath, dao.getById("a")!!.audioPath)
        assertEquals("[00:01.00]hola", dao.getLyrics("a")!!.syncedLrc)
        assertFalse(File(storage.fileFor("a").path + ".part").exists())
    }

    @Test
    fun `failed audio download leaves no partial file and song unavailable`() = runTest {
        val remote = FakeRemote().apply { songs += song("a") }
        val dao = FakeSongDao()
        val storage = TempStorage(tmp.newFolder())
        val downloader = SongDownloader(remote, dao, storage)
        remote.failure = java.io.IOException("boom")

        runCatching { downloader.download("a") }

        assertFalse(storage.exists("a"))
        assertNotNull(dao.getAll())
        assertTrue(dao.getAll().all { it.audioPath == null })
    }
}
