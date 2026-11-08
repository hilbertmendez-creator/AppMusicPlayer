package com.mimusica.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileAudioStorageTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `fileFor uses the extension it is given`() {
        val storage = FileAudioStorage(tmp.newFolder())

        assertEquals("a.webm", storage.fileFor("a", "webm").name)
    }

    @Test
    fun `find returns the saved file whatever its extension`() {
        val storage = FileAudioStorage(tmp.newFolder())
        storage.fileFor("a", "webm").writeBytes(byteArrayOf(1))

        assertEquals("a.webm", storage.find("a")!!.name)
        assertTrue(storage.exists("a"))
    }

    @Test
    fun `find ignores empty files, partial downloads and other songs`() {
        val storage = FileAudioStorage(tmp.newFolder())
        storage.fileFor("a", "m4a").createNewFile()
        storage.fileFor("a", AudioStorage.PART_EXTENSION).writeBytes(byteArrayOf(1))
        storage.fileFor("ab", "m4a").writeBytes(byteArrayOf(1))

        assertNull(storage.find("a"))
        assertFalse(storage.exists("a"))
    }

    @Test
    fun `delete removes every file of the song but not other songs`() {
        val storage = FileAudioStorage(tmp.newFolder())
        storage.fileFor("a", "webm").writeBytes(byteArrayOf(1))
        storage.fileFor("a", AudioStorage.PART_EXTENSION).writeBytes(byteArrayOf(1))
        storage.fileFor("ab", "m4a").writeBytes(byteArrayOf(1))

        storage.delete("a")

        assertFalse(storage.exists("a"))
        assertFalse(storage.fileFor("a", AudioStorage.PART_EXTENSION).exists())
        assertTrue(storage.exists("ab"))
    }
}
