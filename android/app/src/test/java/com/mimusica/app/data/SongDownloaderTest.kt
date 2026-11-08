package com.mimusica.app.data

import com.mimusica.app.domain.model.Lyrics
import com.mimusica.app.fakes.FakeRemote
import com.mimusica.app.fakes.FakeSongDao
import com.mimusica.app.fakes.song
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SongDownloaderTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun storage(dir: File) = FileAudioStorage(dir)

    @Test
    fun `downloads audio and lyrics then marks the song available`() = runTest {
        val remote = FakeRemote().apply {
            songs += song("a", synced = true)
            lyricsBySong["a"] = Lyrics("[00:01.00]hola", null, 10)
        }
        val dao = FakeSongDao()
        val storage = storage(tmp.newFolder())
        val downloader = SongDownloader(remote, dao, storage)

        downloader.download("a")

        assertTrue(storage.exists("a"))
        assertEquals(storage.fileFor("a", "m4a").absolutePath, dao.getById("a")!!.audioPath)
        assertEquals("[00:01.00]hola", dao.getLyrics("a")!!.syncedLrc)
        assertFalse(storage.fileFor("a", AudioStorage.PART_EXTENSION).exists())
    }

    @Test
    fun `saves the audio with the real extension the server reports`() = runTest {
        val remote = FakeRemote().apply {
            songs += song("a")
            audioMediaType = "audio/webm"
        }
        val dao = FakeSongDao()
        val storage = storage(tmp.newFolder())

        SongDownloader(remote, dao, storage).download("a")

        val path = dao.getById("a")!!.audioPath!!
        assertEquals("a.webm", File(path).name)
        assertTrue(File(path).exists())
        assertFalse(storage.fileFor("a", "m4a").exists())
        assertFalse(storage.fileFor("a", AudioStorage.PART_EXTENSION).exists())
    }

    @Test
    fun `saves as m4a when the server sends no media type`() = runTest {
        val remote = FakeRemote().apply {
            songs += song("a")
            audioMediaType = null
        }
        val dao = FakeSongDao()
        val storage = storage(tmp.newFolder())

        SongDownloader(remote, dao, storage).download("a")

        assertEquals("a.m4a", File(dao.getById("a")!!.audioPath!!).name)
    }

    @Test
    fun `does not download again when the song is already saved in another format`() = runTest {
        val remote = FakeRemote().apply { songs += song("a") }
        val dao = FakeSongDao()
        val storage = storage(tmp.newFolder())
        storage.fileFor("a", "webm").writeBytes(byteArrayOf(9))

        SongDownloader(remote, dao, storage).download("a")

        assertNull(remote.audioTarget)
        assertEquals("a.webm", File(dao.getById("a")!!.audioPath!!).name)
    }

    @Test
    fun `failed audio download leaves no partial file and song unavailable`() = runTest {
        val remote = FakeRemote().apply { songs += song("a") }
        val dao = FakeSongDao()
        val storage = storage(tmp.newFolder())
        val downloader = SongDownloader(remote, dao, storage)
        remote.failure = java.io.IOException("boom")

        runCatching { downloader.download("a") }

        assertFalse(storage.exists("a"))
        assertNotNull(dao.getAll())
        assertTrue(dao.getAll().all { it.audioPath == null })
    }
}
