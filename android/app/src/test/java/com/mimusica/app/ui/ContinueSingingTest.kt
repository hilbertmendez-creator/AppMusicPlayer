package com.mimusica.app.ui

import com.mimusica.app.fakes.song
import com.mimusica.app.ui.home.continueSingingSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContinueSingingTest {
    private val a = song("a", synced = true)
    private val b = song("b", plain = true)
    private val c = song("c", synced = true)

    @Test
    fun `no singable songs means nothing to continue`() {
        assertNull(continueSingingSong("x", "y", emptyList()))
    }

    @Test
    fun `the loaded song wins when it has lyrics`() {
        assertEquals(b, continueSingingSong(currentSongId = "b", lastSungSongId = "c", singable = listOf(a, b, c)))
    }

    @Test
    fun `a loaded song without lyrics is ignored and the last sung one is used`() {
        assertEquals(c, continueSingingSong(currentSongId = "no-lyrics", lastSungSongId = "c", singable = listOf(a, b, c)))
    }

    @Test
    fun `a last sung song that was deleted falls back to the first singable`() {
        assertEquals(a, continueSingingSong(currentSongId = null, lastSungSongId = "gone", singable = listOf(a, b, c)))
    }

    @Test
    fun `with no history the first singable song is used`() {
        assertEquals(a, continueSingingSong(null, null, listOf(a, b, c)))
    }
}
